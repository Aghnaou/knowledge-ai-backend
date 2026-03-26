package com.enterprise.knowledgeai.chat.service;

import com.enterprise.knowledgeai.analytics.entity.QueryLog;
import com.enterprise.knowledgeai.analytics.repository.QueryLogRepository;
import com.enterprise.knowledgeai.auth.entity.User;
import com.enterprise.knowledgeai.chat.dto.ConversationDTO;
import com.enterprise.knowledgeai.chat.dto.MessageDTO;
import com.enterprise.knowledgeai.chat.entity.Conversation;
import com.enterprise.knowledgeai.chat.entity.Message;
import com.enterprise.knowledgeai.chat.repository.ConversationRepository;
import com.enterprise.knowledgeai.chat.repository.MessageRepository;
import com.enterprise.knowledgeai.document.entity.Document;
import com.enterprise.knowledgeai.document.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final QueryLogRepository queryLogRepository;
    private final RagService ragService;
    private final ChatModel chatModel;
    private final ObjectMapper objectMapper;
    private final DocumentRepository documentRepository;

    // ─── Conversation CRUD ───────────────────────────────────────────────────

    public ConversationDTO createConversation(String title, User user) {
        Conversation conv = Conversation.builder()
                .title(title != null ? title : "New Conversation")
                .userId(user.getId())
                .tenantId(user.getTenantId())
                .build();
        return ConversationDTO.from(conversationRepository.save(conv));
    }

    public Page<ConversationDTO> listConversations(User user, Pageable pageable) {
        return conversationRepository
                .findByUserIdAndTenantId(user.getId(), user.getTenantId(), pageable)
                .map(ConversationDTO::from);
    }

    @Transactional
    public void deleteConversation(UUID id, User user) {
        Conversation conv = findConversationForUser(id, user);
        messageRepository.deleteByConversationId(id);
        conversationRepository.delete(conv);
        log.debug("Conversation {} deleted", id);
    }

    public ConversationDTO getConversation(UUID id, User user) {
        Conversation conv = findConversationForUser(id, user);
        List<MessageDTO> messages = messageRepository
                .findByConversationIdOrderByCreatedAtAsc(id)
                .stream().map(MessageDTO::from).toList();
        return ConversationDTO.from(conv, messages);
    }

    // ─── Streaming RAG Chat ──────────────────────────────────────────────────

    /**
     * Entry point: saves user message then kicks off async streaming.
     */
    public SseEmitter stream(UUID conversationId, String question, User user) {
        Conversation conv = findConversationForUser(conversationId, user);

        // Auto-title conversation from first question
        if ("New Conversation".equals(conv.getTitle())) {
            conv.setTitle(question.length() > 60 ? question.substring(0, 60) + "…" : question);
            conversationRepository.save(conv);
        }

        // Save user message
        messageRepository.save(Message.builder()
                .conversationId(conv.getId())
                .role(Message.Role.USER)
                .content(question)
                .build());

        SseEmitter emitter = new SseEmitter(180_000L); // 3-min timeout
        executeStream(emitter, conv, question, user);
        return emitter;
    }

    @Async("chatExecutor")
    protected void executeStream(SseEmitter emitter, Conversation conv,
                                  String question, User user) {
        long startTime = System.currentTimeMillis();
        StringBuilder fullResponse = new StringBuilder();

        try {
            // 1. RAG: retrieve context + sources
            String context = ragService.buildContext(question, user.getTenantId());
            List<String> sources = ragService.getSourceNames(question, user.getTenantId());

            // 2. Build prompt (system + history + question)
            List<org.springframework.ai.chat.messages.Message> messages = buildMessages(
                    conv.getId(), question, context, user.getTenantId());

            // 3. Stream response token by token
            chatModel.stream(new Prompt(messages))
                    .doOnNext(response -> {
                        String token = response.getResult().getOutput().getText();
                        if (token != null && !token.isEmpty()) {
                            fullResponse.append(token);
                            try {
                                // JSON-encode token so leading spaces are preserved
                                // (SSE spec strips one leading space from data: field values)
                                emitter.send(SseEmitter.event().data(objectMapper.writeValueAsString(token)));
                            } catch (IOException e) {
                                log.warn("SSE send error: {}", e.getMessage());
                            }
                        }
                    })
                    .doOnError(err -> {
                        log.error("Streaming error: {}", err.getMessage());
                        emitter.completeWithError(err);
                    })
                    .doOnComplete(() -> {
                        try {
                            emitter.send(SseEmitter.event().data(objectMapper.writeValueAsString("[DONE]")));
                            emitter.complete();
                        } catch (IOException e) {
                            log.warn("Error sending [DONE]: {}", e.getMessage());
                        }
                    })
                    .blockLast(); // blocks chatExecutor thread — intentional in non-reactive context

            // 4. Persist assistant message
            messageRepository.save(Message.builder()
                    .conversationId(conv.getId())
                    .role(Message.Role.ASSISTANT)
                    .content(fullResponse.toString())
                    .sources(sources)
                    .build());

            // 5. Log analytics
            long elapsed = System.currentTimeMillis() - startTime;
            queryLogRepository.save(QueryLog.builder()
                    .userId(user.getId())
                    .tenantId(user.getTenantId())
                    .question(question)
                    .answerLength(fullResponse.length())
                    .documentsReferenced(sources.toArray(new String[0]))
                    .responseTimeMs(elapsed)
                    .build());

        } catch (Exception e) {
            log.error("Chat execution failed: {}", e.getMessage(), e);
            emitter.completeWithError(e);
        }
    }

    // ─── Feedback ────────────────────────────────────────────────────────────

    public MessageDTO submitFeedback(UUID messageId, String feedback, User user) {
        Message msg = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found: " + messageId));
        msg.setFeedback(Message.Feedback.valueOf(feedback.toUpperCase()));
        return MessageDTO.from(messageRepository.save(msg));
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private List<org.springframework.ai.chat.messages.Message> buildMessages(
            UUID conversationId, String question, String context, UUID tenantId) {

        List<org.springframework.ai.chat.messages.Message> messages = new ArrayList<>();

        // Build document inventory for the tenant
        List<Document> docs = documentRepository.findByTenantId(tenantId);
        StringBuilder inventory = new StringBuilder();
        inventory.append("--- Document Inventory ---\n");
        inventory.append("Total documents: ").append(docs.size()).append("\n");
        docs.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        d -> d.getCategory() != null ? d.getCategory() : "Uncategorized"))
                .forEach((cat, catDocs) -> {
                    inventory.append(cat).append(": ").append(catDocs.size()).append(" document(s) — ");
                    inventory.append(catDocs.stream().map(Document::getName)
                            .collect(java.util.stream.Collectors.joining(", ")));
                    inventory.append("\n");
                });
        inventory.append("--------------------------");

        // System prompt with document inventory + RAG context
        String systemPrompt = """
                You are an enterprise AI assistant. Answer questions using the provided document context and inventory.
                Be professional, accurate, and concise. If the context doesn't contain the answer,
                say so clearly and suggest the user upload relevant documents.
                Always cite the source document when referencing specific information.
                For questions about how many documents exist, their categories, or names, use the Document Inventory section below.
                """
                + "\n\n" + inventory
                + (context.isEmpty() ? "" : "\n\n" + context);

        messages.add(new SystemMessage(systemPrompt));

        // Last 10 messages of chat history (reversed to chronological order)
        List<Message> history = messageRepository
                .findTop10ByConversationIdOrderByCreatedAtDesc(conversationId);
        Collections.reverse(history);
        for (Message h : history) {
            if (h.getRole() == Message.Role.USER) {
                messages.add(new UserMessage(h.getContent()));
            } else {
                messages.add(new AssistantMessage(h.getContent()));
            }
        }

        // Current question
        messages.add(new UserMessage(question));
        return messages;
    }

    private Conversation findConversationForUser(UUID id, User user) {
        Conversation conv = conversationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Conversation not found: " + id));
        if (!conv.getTenantId().equals(user.getTenantId())) {
            throw new RuntimeException("Access denied to conversation: " + id);
        }
        return conv;
    }

}
