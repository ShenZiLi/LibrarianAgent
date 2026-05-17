package com.librarian.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.librarian.config.RagProperties;
import com.librarian.mapper.DocumentRecordMapper;
import com.librarian.mapper.VectorDocumentChunkMapper;
import com.librarian.model.dto.DocumentDto.DocumentResponse;
import com.librarian.model.entity.DocumentChunk;
import com.librarian.model.entity.DocumentRecord;
import com.librarian.model.entity.VectorDocumentChunk;
import com.librarian.service.rag.DocumentParser;
import com.librarian.service.rag.TextChunker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class IngestionService {

    @Autowired
    private DocumentRecordMapper documentRecordMapper;
    @Autowired
    private VectorDocumentChunkMapper vectorDocumentChunkMapper;
    @Autowired
    private DocumentParser documentParser;
    @Autowired
    private TextChunker textChunker;
    @Autowired
    private VectorStore vectorStore;
    @Autowired
    private RagProperties ragProperties;

    @Async("documentIngestionExecutor")
    public void ingestDocument(byte[] fileContent, String fileName, String contentType, long fileSize) {
        String documentId = UUID.randomUUID().toString();
        log.info("Starting document ingestion: {} (id={})", fileName, documentId);

        DocumentRecord record = new DocumentRecord();
        record.setDocumentId(documentId);
        record.setFileName(fileName);
        record.setFileType(contentType);
        record.setFileSize(fileSize);
        record.setStatus("processing");
        record.setChunkCount(0);
        record.setCreatedAt(Instant.now());
        record.setUpdatedAt(Instant.now());
        documentRecordMapper.insert(record);

        doIngest(record, fileContent);
    }

    public void retryDocument(String documentId) {
        DocumentRecord record = documentRecordMapper.selectOne(
                new LambdaQueryWrapper<DocumentRecord>()
                        .eq(DocumentRecord::getDocumentId, documentId)
        );
        if (record == null) {
            throw new RuntimeException("Document not found: " + documentId);
        }
        if (!"failed".equals(record.getStatus())) {
            throw new RuntimeException("Only failed documents can be retried, current status: " + record.getStatus());
        }

        List<VectorDocumentChunk> existingChunks = vectorDocumentChunkMapper.selectList(
                new LambdaQueryWrapper<VectorDocumentChunk>()
                        .eq(VectorDocumentChunk::getDocumentId, documentId)
        );
        if (!existingChunks.isEmpty()) {
            try {
                vectorStore.delete(existingChunks.stream().map(VectorDocumentChunk::getVectorId).toList());
            } catch (Exception e) {
                log.warn("Failed to clean up existing vectors for retry {}: {}", documentId, e.getMessage());
            }
            vectorDocumentChunkMapper.delete(
                    new LambdaQueryWrapper<VectorDocumentChunk>()
                            .eq(VectorDocumentChunk::getDocumentId, documentId)
            );
        }

        record.setStatus("processing");
        record.setErrorMessage(null);
        record.setUpdatedAt(Instant.now());
        documentRecordMapper.updateById(record);

        doIngest(record, (byte[]) null);
    }

    private void doIngest(DocumentRecord record, byte[] fileContent) {
        String documentId = record.getDocumentId();
        try {
            if (fileContent == null || fileContent.length == 0) {
                throw new RuntimeException("File data not available for retry. Please re-upload the document.");
            }

            List<DocumentChunk> parsedChunks = documentParser.parse(fileContent, record.getFileName());
            if (parsedChunks.isEmpty()) {
                throw new RuntimeException("No content extracted from document");
            }

            List<DocumentChunk> chunkedDocuments = textChunker.chunkAll(parsedChunks);

            List<Document> aiDocuments = new ArrayList<>();
            for (int i = 0; i < chunkedDocuments.size(); i++) {
                DocumentChunk chunk = chunkedDocuments.get(i);
                Map<String, Object> metadata = Map.of(
                        "fileName", chunk.getMetadataAsString("fileName"),
                        "fileType", chunk.getMetadataAsString("fileType"),
                        "documentId", documentId
                );
                Document aiDoc = new Document(chunk.getContent(), metadata);
                aiDocuments.add(aiDoc);
            }

            vectorStore.add(aiDocuments);

            for (int i = 0; i < aiDocuments.size(); i++) {
                VectorDocumentChunk chunkRecord = new VectorDocumentChunk();
                chunkRecord.setDocumentId(documentId);
                chunkRecord.setChunkIndex(i);
                chunkRecord.setChunkText(aiDocuments.get(i).getText());
                chunkRecord.setVectorId(aiDocuments.get(i).getId());
                chunkRecord.setCreatedAt(Instant.now());
                vectorDocumentChunkMapper.insert(chunkRecord);
            }

            record.setStatus("completed");
            record.setChunkCount(aiDocuments.size());
            record.setUpdatedAt(Instant.now());
            documentRecordMapper.updateById(record);

            log.info("Document ingestion completed: {} ({} chunks)", documentId, aiDocuments.size());
        } catch (Exception e) {
            log.error("Document ingestion failed: {}", documentId, e);
            record.setStatus("failed");
            record.setErrorMessage(e.getMessage());
            record.setUpdatedAt(Instant.now());
            documentRecordMapper.updateById(record);
        }
    }

    public Page<DocumentResponse> listDocuments(int page, int size, String status) {
        LambdaQueryWrapper<DocumentRecord> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) {
            wrapper.eq(DocumentRecord::getStatus, status);
        }
        wrapper.orderByDesc(DocumentRecord::getCreatedAt);

        Page<DocumentRecord> recordPage = documentRecordMapper.selectPage(
                new Page<>(page, size), wrapper
        );

        Page<DocumentResponse> responsePage = new Page<>(
                recordPage.getCurrent(), recordPage.getSize(), recordPage.getTotal()
        );
        responsePage.setRecords(recordPage.getRecords().stream().map(this::toResponse).toList());
        return responsePage;
    }

    public DocumentResponse getDocument(String documentId) {
        DocumentRecord record = documentRecordMapper.selectOne(
                new LambdaQueryWrapper<DocumentRecord>()
                        .eq(DocumentRecord::getDocumentId, documentId)
        );
        if (record == null) {
            throw new RuntimeException("Document not found: " + documentId);
        }
        return toResponse(record);
    }

    public void deleteDocument(String documentId) {
        DocumentRecord record = documentRecordMapper.selectOne(
                new LambdaQueryWrapper<DocumentRecord>()
                        .eq(DocumentRecord::getDocumentId, documentId)
        );
        if (record == null) {
            throw new RuntimeException("Document not found: " + documentId);
        }

        List<VectorDocumentChunk> chunks = vectorDocumentChunkMapper.selectList(
                new LambdaQueryWrapper<VectorDocumentChunk>()
                        .eq(VectorDocumentChunk::getDocumentId, documentId)
        );

        if (!chunks.isEmpty()) {
            try {
                vectorStore.delete(chunks.stream().map(VectorDocumentChunk::getVectorId).toList());
                log.info("Deleted {} vectors for document: {}", chunks.size(), documentId);
            } catch (Exception e) {
                log.warn("Failed to delete vectors for document {}: {}", documentId, e.getMessage());
            }
            vectorDocumentChunkMapper.delete(
                    new LambdaQueryWrapper<VectorDocumentChunk>()
                            .eq(VectorDocumentChunk::getDocumentId, documentId)
            );
        }

        documentRecordMapper.deleteById(record.getId());
        log.info("Document deleted: {}", documentId);
    }

    @Scheduled(fixedDelayString = "${rag.processing-check-interval:300000}")
    public void markTimeoutDocuments() {
        Instant timeoutThreshold = Instant.now().minusSeconds(ragProperties.getProcessingTimeoutMinutes() * 60L);

        List<DocumentRecord> timedOutRecords = documentRecordMapper.selectList(
                new LambdaQueryWrapper<DocumentRecord>()
                        .eq(DocumentRecord::getStatus, "processing")
                        .lt(DocumentRecord::getUpdatedAt, timeoutThreshold)
        );

        for (DocumentRecord record : timedOutRecords) {
            log.warn("Marking timed out document as failed: {}", record.getDocumentId());
            record.setStatus("failed");
            record.setErrorMessage("Processing timeout after " + ragProperties.getProcessingTimeoutMinutes() + " minutes");
            record.setUpdatedAt(Instant.now());
            documentRecordMapper.updateById(record);
        }
    }

    private DocumentResponse toResponse(DocumentRecord record) {
        return new DocumentResponse(
                record.getDocumentId(),
                record.getFileName(),
                record.getFileType(),
                record.getFileSize(),
                record.getStatus(),
                record.getChunkCount(),
                record.getErrorMessage(),
                record.getCreatedAt(),
                record.getUpdatedAt()
        );
    }
}
