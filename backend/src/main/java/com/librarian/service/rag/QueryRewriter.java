package com.librarian.service.rag;

import com.librarian.model.entity.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class QueryRewriter {

    private static final Logger log = LoggerFactory.getLogger(QueryRewriter.class);

    public String rewrite(String query, List<Message> history) {
        log.info("Rewriting query with {} history messages", history.size());
        return query;
    }
}
