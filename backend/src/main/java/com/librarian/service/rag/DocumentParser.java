package com.librarian.service.rag;

import com.librarian.model.entity.DocumentChunk;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class DocumentParser {

    public List<DocumentChunk> parse(MultipartFile file) {
        String filename = file.getOriginalFilename();
        log.info("Parsing document: {}", filename);

        if (filename == null) {
            return Collections.emptyList();
        }

        if (filename.toLowerCase().endsWith(".pdf")) {
            return parsePdf(file);
        } else if (filename.toLowerCase().endsWith(".md") || 
                   filename.toLowerCase().endsWith(".txt")) {
            return parseMarkdown(file);
        }

        log.warn("Unsupported file format: {}", filename);
        return Collections.emptyList();
    }

    private List<DocumentChunk> parsePdf(MultipartFile file) {
        log.info("PDF parsing not yet implemented");
        return Collections.emptyList();
    }

    private List<DocumentChunk> parseMarkdown(MultipartFile file) {
        log.info("Markdown/TXT parsing not yet implemented");
        return Collections.emptyList();
    }
}
