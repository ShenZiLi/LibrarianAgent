package com.librarian.service.rag;

import com.librarian.model.entity.DocumentChunk;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
public class DocumentParser {

    private static final int OCR_DPI = 150;

    public List<DocumentChunk> parse(MultipartFile file) {
        String filename = file.getOriginalFilename();
        log.info("Parsing document: {}", filename);

        if (filename == null) {
            return List.of();
        }

        if (filename.toLowerCase().endsWith(".pdf")) {
            return parsePdf(file, filename);
        } else if (filename.toLowerCase().endsWith(".md") ||
                   filename.toLowerCase().endsWith(".txt")) {
            return parseMarkdown(file, filename);
        }

        log.warn("Unsupported file format: {}", filename);
        return List.of();
    }

    private List<DocumentChunk> parsePdf(MultipartFile file, String filename) {
        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String text = stripper.getText(document);

            if (text.isBlank()) {
                log.info("PDF appears to be scanned, falling back to OCR");
                return performOcr(document, filename);
            }

            DocumentChunk chunk = new DocumentChunk();
            chunk.setDocumentId(UUID.randomUUID().toString());
            chunk.setContent(text);
            chunk.addMetadata("fileName", filename);
            chunk.addMetadata("fileType", "pdf");
            chunk.addMetadata("pageCount", document.getNumberOfPages());
            return List.of(chunk);
        } catch (IOException e) {
            log.error("Failed to parse PDF: {}", filename, e);
            return List.of();
        }
    }

    private List<DocumentChunk> performOcr(PDDocument document, String filename) {
        List<DocumentChunk> chunks = new ArrayList<>();
        Tesseract tesseract = new Tesseract();
        tesseract.setDatapath(System.getenv("TESSDATA_PREFIX"));
        tesseract.setLanguage("eng+chi_sim");
        tesseract.setPageSegMode(3);

        PDFRenderer renderer = new PDFRenderer(document);
        StringBuilder fullText = new StringBuilder();

        try {
            for (int i = 0; i < document.getNumberOfPages(); i++) {
                BufferedImage image = renderer.renderImageWithDPI(i, OCR_DPI);
                String pageText = tesseract.doOCR(image);
                fullText.append(pageText).append("\n\n");
                log.debug("OCR completed for page {}/{} of {}", i + 1, document.getNumberOfPages(), filename);
            }

            DocumentChunk chunk = new DocumentChunk();
            chunk.setDocumentId(UUID.randomUUID().toString());
            chunk.setContent(fullText.toString());
            chunk.addMetadata("fileName", filename);
            chunk.addMetadata("fileType", "pdf-scanned");
            chunk.addMetadata("pageCount", document.getNumberOfPages());
            chunks.add(chunk);
        } catch (IOException | TesseractException e) {
            log.error("OCR failed for PDF: {}", filename, e);
        }

        return chunks;
    }

    private List<DocumentChunk> parseMarkdown(MultipartFile file, String filename) {
        try {
            String content = new String(file.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

            DocumentChunk chunk = new DocumentChunk();
            chunk.setDocumentId(UUID.randomUUID().toString());
            chunk.setContent(content);
            chunk.addMetadata("fileName", filename);
            chunk.addMetadata("fileType", filename.endsWith(".md") ? "markdown" : "text");

            return List.of(chunk);
        } catch (IOException e) {
            log.error("Failed to parse text file: {}", filename, e);
            return List.of();
        }
    }
}
