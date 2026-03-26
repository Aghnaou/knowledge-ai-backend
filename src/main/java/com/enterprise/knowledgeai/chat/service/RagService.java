package com.enterprise.knowledgeai.chat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RagService {

    private final VectorStore vectorStore;

    /**
     * Searches PGVector for top-5 chunks relevant to the question,
     * filtered strictly by tenant_id for data isolation.
     * Returns formatted context string ready to inject into the prompt.
     */
    public String buildContext(String question, UUID tenantId) {
        List<org.springframework.ai.document.Document> docs = search(question, tenantId);

        if (docs.isEmpty()) {
            log.debug("No relevant documents found for tenant {} and question: {}", tenantId, question);
            return "";
        }

        StringBuilder context = new StringBuilder();
        context.append("Relevant information from uploaded documents:\n\n");
        for (org.springframework.ai.document.Document doc : docs) {
            String docName = (String) doc.getMetadata().getOrDefault("document_name", "Unknown");
            context.append("--- Source: ").append(docName).append(" ---\n");
            context.append(doc.getText()).append("\n\n");
        }

        log.debug("Built context from {} chunks for question: {}", docs.size(), question);
        return context.toString();
    }

    /**
     * Returns distinct document names referenced for source citations.
     */
    public List<String> getSourceNames(String question, UUID tenantId) {
        return search(question, tenantId).stream()
                .map(doc -> (String) doc.getMetadata().getOrDefault("document_name", "Unknown"))
                .distinct()
                .toList();
    }

    private List<org.springframework.ai.document.Document> search(String question, UUID tenantId) {
        try {
            SearchRequest request = SearchRequest.builder()
                    .query(question)
                    .topK(5)
                    .filterExpression("tenant_id == '" + tenantId + "'")
                    .build();
            return vectorStore.similaritySearch(request);
        } catch (Exception e) {
            log.warn("Vector search failed, proceeding without context: {}", e.getMessage());
            return List.of();
        }
    }
}
