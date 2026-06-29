package com.reportcheck.algorithm;

import lombok.Data;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class SimilarityCalculator {

    private static final Pattern CLEAN_PATTERN = Pattern.compile("[^\\p{IsHan}a-zA-Z0-9]");
    private static final Set<String> STOP_WORDS = Set.of(
            "的", "了", "和", "是", "在", "对", "中", "与", "及", "以及", "进行", "通过", "系统",
            "the", "a", "an", "and", "or", "of", "to", "in", "for", "is", "are"
    );

    public SimilarityScore calculate(String sourceText, String targetText) {
        List<String> sourceTokens = tokenize(sourceText);
        List<String> targetTokens = tokenize(targetText);
        double cosine = tfidfCosineSimilarity(sourceTokens, targetTokens, List.of(sourceTokens, targetTokens));
        return buildScore(sourceTokens, targetTokens, cosine);
    }

    public SimilarityScore calculateWithCorpus(String sourceText, String targetText, List<String> corpusTexts) {
        List<String> sourceTokens = tokenize(sourceText);
        List<String> targetTokens = tokenize(targetText);
        List<List<String>> corpusTokens = corpusTexts == null ? List.of() : corpusTexts.stream()
                .map(this::tokenize)
                .filter(tokens -> !tokens.isEmpty())
                .toList();
        double cosine = tfidfCosineSimilarity(sourceTokens, targetTokens, corpusTokens);
        return buildScore(sourceTokens, targetTokens, cosine);
    }

    private SimilarityScore buildScore(List<String> sourceTokens, List<String> targetTokens, double cosine) {
        double simhash = simhashSimilarity(sourceTokens, targetTokens);
        double finalScore = cosine * 0.7 + simhash * 0.3;
        if (Math.min(sourceTokens.size(), targetTokens.size()) < 20) {
            finalScore *= 0.85;
        }
        SimilarityScore score = new SimilarityScore();
        score.setCosineSimilarity(round(cosine));
        score.setSimhashSimilarity(round(simhash));
        score.setFinalSimilarity(round(finalScore));
        return score;
    }

    public List<SimilarSentenceMatch> findSimilarSentences(String sourceText, String targetText) {
        List<String> sourceSentences = splitSentences(sourceText);
        List<String> targetSentences = splitSentences(targetText);
        List<SimilarSentenceMatch> matches = new ArrayList<>();
        List<SimilarSentenceMatch> candidates = new ArrayList<>();
        for (String sourceSentence : sourceSentences) {
            for (String targetSentence : targetSentences) {
                List<String> sourceTokens = tokenize(sourceSentence);
                List<String> targetTokens = tokenize(targetSentence);
                double similarity = tfidfCosineSimilarity(sourceTokens, targetTokens, List.of(sourceTokens, targetTokens));
                if (similarity >= 0.20) {
                    SimilarSentenceMatch candidate = new SimilarSentenceMatch();
                    candidate.setSourceSentence(sourceSentence);
                    candidate.setTargetSentence(targetSentence);
                    candidate.setSimilarity(round(similarity));
                    candidates.add(candidate);
                }
                if (similarity >= 0.55) {
                    SimilarSentenceMatch match = new SimilarSentenceMatch();
                    match.setSourceSentence(sourceSentence);
                    match.setTargetSentence(targetSentence);
                    match.setSimilarity(round(similarity));
                    matches.add(match);
                }
            }
        }
        List<SimilarSentenceMatch> result = matches.stream()
                .sorted(Comparator.comparing(SimilarSentenceMatch::getSimilarity).reversed())
                .limit(8)
                .toList();
        if (!result.isEmpty()) {
            return result;
        }
        return candidates.stream()
                .sorted(Comparator.comparing(SimilarSentenceMatch::getSimilarity).reversed())
                .limit(3)
                .toList();
    }

    public List<String> splitSentences(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        String[] parts = text.replace("\r", "\n").split("[。！？!?；;\\n]+");
        List<String> result = new ArrayList<>();
        for (String part : parts) {
            String sentence = part.trim();
            if (sentence.length() >= 12) {
                result.add(sentence);
            }
        }
        return result;
    }

    public List<String> tokenize(String text) {
        String cleaned = clean(text);
        if (cleaned.isBlank()) {
            return List.of();
        }
        List<String> tokens = new ArrayList<>();
        for (String part : cleaned.split("\\s+")) {
            if (part.isBlank()) {
                continue;
            }
            if (part.length() <= 2) {
                if (!STOP_WORDS.contains(part)) {
                    tokens.add(part);
                }
                continue;
            }
            if (part.matches("[a-zA-Z0-9]+")) {
                if (!STOP_WORDS.contains(part)) {
                    tokens.add(part);
                }
                continue;
            }
            for (int i = 0; i < part.length() - 1; i++) {
                String token = part.substring(i, i + 2);
                if (!STOP_WORDS.contains(token)) {
                    tokens.add(token);
                }
            }
        }
        return tokens;
    }

    public String riskLevel(double finalSimilarity) {
        if (finalSimilarity >= 0.80) {
            return "高风险";
        }
        if (finalSimilarity >= 0.60) {
            return "中风险";
        }
        if (finalSimilarity >= 0.40) {
            return "低风险";
        }
        return "正常";
    }

    private String clean(String text) {
        if (text == null) {
            return "";
        }
        return CLEAN_PATTERN.matcher(text.toLowerCase()).replaceAll(" ").trim();
    }

    private double tfidfCosineSimilarity(List<String> sourceTokens, List<String> targetTokens, List<List<String>> corpusTokens) {
        if (sourceTokens.isEmpty() || targetTokens.isEmpty()) {
            return 0;
        }
        Map<String, Integer> sourceVector = frequency(sourceTokens);
        Map<String, Integer> targetVector = frequency(targetTokens);
        Set<String> union = new HashSet<>(sourceVector.keySet());
        union.addAll(targetVector.keySet());

        double dot = 0;
        double sourceNorm = 0;
        double targetNorm = 0;
        int sourceTotal = Math.max(1, sourceTokens.size());
        int targetTotal = Math.max(1, targetTokens.size());
        int corpusSize = Math.max(2, corpusTokens == null || corpusTokens.isEmpty() ? 2 : corpusTokens.size());
        for (String token : union) {
            int documentFrequency = documentFrequency(token, corpusTokens);
            double idf = Math.log((corpusSize + 1.0) / (documentFrequency + 1.0)) + 1.0;
            double sourceValue = (sourceVector.getOrDefault(token, 0) / (double) sourceTotal) * idf;
            double targetValue = (targetVector.getOrDefault(token, 0) / (double) targetTotal) * idf;
            dot += sourceValue * targetValue;
            sourceNorm += sourceValue * sourceValue;
            targetNorm += targetValue * targetValue;
        }
        if (sourceNorm == 0 || targetNorm == 0) {
            return 0;
        }
        return dot / (Math.sqrt(sourceNorm) * Math.sqrt(targetNorm));
    }

    private int documentFrequency(String token, List<List<String>> corpusTokens) {
        if (corpusTokens == null || corpusTokens.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (List<String> tokens : corpusTokens) {
            if (tokens.contains(token)) {
                count++;
            }
        }
        return count;
    }

    private double simhashSimilarity(List<String> sourceTokens, List<String> targetTokens) {
        if (sourceTokens.isEmpty() || targetTokens.isEmpty()) {
            return 0;
        }
        long sourceHash = simhash(sourceTokens);
        long targetHash = simhash(targetTokens);
        int distance = Long.bitCount(sourceHash ^ targetHash);
        return Math.max(0, 1.0 - distance / 64.0);
    }

    private long simhash(List<String> tokens) {
        int[] vector = new int[64];
        Map<String, Integer> frequencies = frequency(tokens);
        for (Map.Entry<String, Integer> entry : frequencies.entrySet()) {
            long hash = stableHash(entry.getKey());
            int weight = entry.getValue();
            for (int i = 0; i < 64; i++) {
                if (((hash >>> i) & 1L) == 1L) {
                    vector[i] += weight;
                } else {
                    vector[i] -= weight;
                }
            }
        }
        long result = 0;
        for (int i = 0; i < 64; i++) {
            if (vector[i] > 0) {
                result |= (1L << i);
            }
        }
        return result;
    }

    private long stableHash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            long hash = 0;
            for (int i = 0; i < 8; i++) {
                hash = (hash << 8) | (bytes[i] & 0xffL);
            }
            return hash;
        } catch (NoSuchAlgorithmException exception) {
            return token.hashCode();
        }
    }

    private Map<String, Integer> frequency(List<String> tokens) {
        Map<String, Integer> frequency = new LinkedHashMap<>();
        for (String token : tokens) {
            frequency.merge(token, 1, Integer::sum);
        }
        return frequency;
    }

    private double round(double value) {
        return Math.round(value * 10000.0) / 10000.0;
    }

    @Data
    public static class SimilarityScore {
        private double cosineSimilarity;
        private double simhashSimilarity;
        private double finalSimilarity;
    }

    @Data
    public static class SimilarSentenceMatch {
        private String sourceSentence;
        private String targetSentence;
        private double similarity;
    }
}
