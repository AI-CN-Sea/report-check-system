package com.reportcheck.algorithm;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimilarityCalculatorTest {

    private final SimilarityCalculator calculator = new SimilarityCalculator();

    @Test
    void identicalTextShouldHaveHighSimilarity() {
        String text = """
                本次实验完成了基于 Spring Boot 的实验报告查重系统设计。
                系统包括用户登录、课程管理、实验任务管理、报告上传、文本解析和查重结果展示。
                查重算法结合 TF-IDF 余弦相似度和 SimHash 文本指纹，能够辅助教师定位疑似重复报告。
                """;

        SimilarityCalculator.SimilarityScore score = calculator.calculateWithCorpus(text, text, List.of(text, text));

        assertTrue(score.getCosineSimilarity() > 0.95);
        assertTrue(score.getSimhashSimilarity() > 0.95);
        assertTrue(score.getFinalSimilarity() > 0.95);
    }

    @Test
    void unrelatedTextShouldHaveLowSimilarity() {
        String source = """
                实验报告查重系统主要面向高校实验教学场景，完成报告上传、文本解析和相似度计算。
                教师可以查看风险等级、相似句子和统计图表，从而提高批量检查效率。
                """;
        String target = """
                图书借阅管理系统用于维护图书信息、读者信息、借阅记录和归还记录。
                管理员可以录入新书、处理借阅申请，并按照分类统计馆藏数量。
                """;

        SimilarityCalculator.SimilarityScore score = calculator.calculateWithCorpus(source, target, List.of(source, target));

        assertTrue(score.getFinalSimilarity() < 0.45);
    }

    @Test
    void emptyTextShouldNotProduceSimhashSimilarity() {
        SimilarityCalculator.SimilarityScore score = calculator.calculate("", "");

        assertEquals(0, score.getCosineSimilarity());
        assertEquals(0, score.getSimhashSimilarity());
        assertEquals(0, score.getFinalSimilarity());
    }

    @Test
    void riskLevelShouldFollowConfiguredThresholds() {
        assertEquals("高风险", calculator.riskLevel(0.80));
        assertEquals("中风险", calculator.riskLevel(0.60));
        assertEquals("低风险", calculator.riskLevel(0.40));
        assertEquals("正常", calculator.riskLevel(0.39));
    }

    @Test
    void sentenceMatchingShouldReturnSimilarSentences() {
        String source = "系统采用 TF-IDF 余弦相似度和 SimHash 文本指纹完成实验报告查重。教师可以查看相似句子和风险等级。";
        String target = "系统采用 TF-IDF 余弦相似度和 SimHash 文本指纹完成实验报告查重，并向教师展示相似句子和风险等级。";

        List<SimilarityCalculator.SimilarSentenceMatch> matches = calculator.findSimilarSentences(source, target);

        assertTrue(matches.size() >= 1);
        assertTrue(matches.get(0).getSimilarity() >= 0.55);
    }

    @Test
    void fallbackSentenceMatchingShouldReturnReviewCandidates() {
        String source = "报告上传后需要保存文件并提取正文内容，同时记录解析状态和字数信息。教师查看详情时需要看到中文原因提示。";
        String target = "报告上传后需要保存文件并提取正文内容，同时系统还要返回解析状态、字数信息和中文说明。";

        List<SimilarityCalculator.SimilarSentenceMatch> matches = calculator.findSimilarSentences(source, target);

        assertTrue(matches.size() >= 1);
        assertTrue(matches.get(0).getSimilarity() >= 0.20);
    }
}
