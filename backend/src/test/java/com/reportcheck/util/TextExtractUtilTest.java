package com.reportcheck.util;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextExtractUtilTest {

    private final TextExtractUtil textExtractUtil = new TextExtractUtil();

    @TempDir
    Path tempDir;

    @Test
    void shouldReadUtf8TextFile() throws Exception {
        Path file = tempDir.resolve("report.txt");
        String content = "实验报告查重系统支持 UTF-8 文本解析。";
        Files.writeString(file, content, StandardCharsets.UTF_8);

        assertEquals(content, textExtractUtil.extract(file, "txt"));
    }

    @Test
    void shouldReadGbkTextFile() throws Exception {
        Path file = tempDir.resolve("report-gbk.txt");
        String content = "实验报告查重系统兼容 GBK 文本。";
        Files.write(file, content.getBytes(Charset.forName("GBK")));

        assertEquals(content, textExtractUtil.extract(file, "txt"));
    }

    @Test
    void shouldReadDocxFile() throws Exception {
        Path file = tempDir.resolve("report.docx");
        String content = "实验报告查重系统支持 Word 文档解析。";
        try (XWPFDocument document = new XWPFDocument();
             OutputStream outputStream = Files.newOutputStream(file)) {
            document.createParagraph().createRun().setText(content);
            document.write(outputStream);
        }

        assertTrue(textExtractUtil.extract(file, "docx").contains(content));
    }

    @Test
    void shouldReadPdfFile() throws Exception {
        Path file = tempDir.resolve("report.pdf");
        String content = "Report check system supports text PDF parsing.";
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                stream.beginText();
                stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                stream.newLineAtOffset(60, 720);
                stream.showText(content);
                stream.endText();
            }
            document.save(file.toFile());
        }

        assertTrue(textExtractUtil.extract(file, "pdf").contains(content));
    }

    @Test
    void shouldReturnBlankTextForEmptyPdf() throws Exception {
        Path file = tempDir.resolve("empty.pdf");
        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage());
            document.save(file.toFile());
        }

        assertTrue(textExtractUtil.extract(file, "pdf").isBlank());
    }

    @Test
    void unsupportedFileTypeShouldThrowException() {
        Path file = tempDir.resolve("report.exe");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> textExtractUtil.extract(file, "exe")
        );
        assertTrue(exception.getMessage().contains("暂不支持"));
    }
}
