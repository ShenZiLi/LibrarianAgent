package com.librarian.service;

import com.librarian.config.RagProperties;
import com.librarian.model.dto.*;
import com.librarian.model.entity.DocumentEntity;
import com.librarian.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentIngestService {

    private final DocumentChunker chunker;
    private final VectorStoreService vectorStoreService;
    private final DocumentRepository documentRepository;
    private final EmbeddingModel embeddingModel;
    private final RagProperties ragProperties;
    private final Tika tika = new Tika();

    public DocumentUploadResponse ingestDocument(MultipartFile file, String category, String language) throws IOException {
        String documentId = UUID.randomUUID().toString();
        String filename = file.getOriginalFilename();
        String fileType = detectFileType(file);

        DocumentEntity document = DocumentEntity.builder()
                .documentId(documentId)
                .filename(filename)
                .fileType(fileType)
                .category(category != null ? category : "general")
                .language(language != null ? language : "auto")
                .status("PROCESSING")
                .build();

        documentRepository.save(document);

        ingestAsync(file, documentId, category, language);

        return DocumentUploadResponse.builder()
                .documentId(documentId)
                .status("PROCESSING")
                .estimatedSeconds(estimateProcessingTime(file.getSize(), fileType))
                .build();
    }

    @Async
    public CompletableFuture<Integer> ingestAsync(MultipartFile file, String documentId,
                                                   String category, String language) {
        try {
            String text = extractText(file);
            String detectedLanguage = "auto".equals(language) ? detectLanguage(text) : language;

            List<String> chunks = chunker.chunk(text, detectedLanguage);

            List<Map<String, Object>> chunkMaps = new ArrayList<>();
            for (int i = 0; i < chunks.size(); i++) {
                String chunkText = chunks.get(i);
                List<Double> embedding = embeddingModel.embed(chunkText);
                List<Float> embeddingList = new ArrayList<>(embedding.size());
                for (Double v : embedding) {
                    embeddingList.add(v.floatValue());
                }

                Map<String, Object> chunkMap = new HashMap<>();
                chunkMap.put("id", documentId + "_chunk_" + i);
                chunkMap.put("content", chunkText);
                chunkMap.put("embedding", embeddingList);
                chunkMap.put("source_file", file.getOriginalFilename());
                chunkMap.put("page_number", 0L);
                chunkMap.put("section_title", "");
                chunkMap.put("language", detectedLanguage);
                chunkMap.put("document_type", category != null ? category : "general");

                chunkMaps.add(chunkMap);
            }

            vectorStoreService.insertChunks(chunkMaps);

            DocumentEntity document = documentRepository.findById(documentId).orElse(null);
            if (document != null) {
                document.setStatus("COMPLETED");
                document.setChunkCount(chunks.size());
                documentRepository.save(document);
            }

            log.info("Document ingestion completed: {} ({} chunks)", file.getOriginalFilename(), chunks.size());
            return CompletableFuture.completedFuture(chunks.size());

        } catch (Exception e) {
            log.error("Failed to ingest document: {}", file.getOriginalFilename(), e);

            DocumentEntity document = documentRepository.findById(documentId).orElse(null);
            if (document != null) {
                document.setStatus("FAILED");
                documentRepository.save(document);
            }

            return CompletableFuture.failedFuture(e);
        }
    }

    private String extractText(MultipartFile file) throws IOException {
        String fileType = tika.detect(file.getInputStream());

        if (fileType != null && (fileType.contains("pdf"))) {
            return extractPdfText(file.getInputStream());
        }

        try (InputStream is = file.getInputStream()) {
            return tika.parseToString(is);
        }
    }

    private String extractPdfText(InputStream inputStream) throws IOException {
        try (PDDocument document = Loader.loadPDF(inputStream.readAllBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    private String detectFileType(MultipartFile file) throws IOException {
        return tika.detect(file.getInputStream());
    }

    private String detectLanguage(String text) {
        if (text == null || text.isEmpty()) {
            return "en";
        }

        int chineseCount = 0;
        int totalChars = Math.min(text.length(), 1000);

        for (int i = 0; i < totalChars; i++) {
            char c = text.charAt(i);
            if (c >= '\u4e00' && c <= '\u9fff') {
                chineseCount++;
            }
        }

        double chineseRatio = (double) chineseCount / totalChars;
        return chineseRatio > 0.1 ? "zh" : "en";
    }

    private long estimateProcessingTime(long fileSize, String fileType) {
        long baseSeconds = fileSize / (1024 * 1024) * 10;
        if (fileType != null && fileType.contains("pdf")) {
            baseSeconds = (long) (baseSeconds * 1.5);
        }
        return Math.max(baseSeconds, 5);
    }
}
