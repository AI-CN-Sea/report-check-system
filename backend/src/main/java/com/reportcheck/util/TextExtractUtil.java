package com.reportcheck.util;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

@Component
public class TextExtractUtil {

    public String extract(Path path, String fileType) throws IOException {
        String lowerType = fileType.toLowerCase();
        if ("txt".equals(lowerType)) {
            return readText(path);
        }
        if ("docx".equals(lowerType)) {
            return readDocx(path);
        }
        if ("pdf".equals(lowerType)) {
            return readPdf(path);
        }
        throw new IllegalArgumentException("暂不支持该文件类型：" + fileType);
    }

    private String readText(Path path) throws IOException {
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            return Files.readString(path, Charset.forName("GBK"));
        }
    }

    private String readDocx(Path path) throws IOException {
        try (InputStream inputStream = Files.newInputStream(path);
             XWPFDocument document = new XWPFDocument(inputStream)) {
            return document.getParagraphs().stream()
                    .map(XWPFParagraph::getText)
                    .collect(Collectors.joining("\n"));
        }
    }

    private String readPdf(Path path) throws IOException {
        try (PDDocument document = Loader.loadPDF(path.toFile())) {
            return new PDFTextStripper().getText(document);
        }
    }
}
