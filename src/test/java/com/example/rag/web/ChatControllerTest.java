package com.example.rag.web;

import com.example.rag.assistant.RagAssistant;
import com.example.rag.service.RagService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ChatControllerTest {

    private MockMvc mockMvc;
    private RagAssistant assistant;

    @BeforeEach
    void setUp() {
        assistant = mock(RagAssistant.class);
        RagService ragService = mock(RagService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ChatController(assistant, ragService))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void returnsGroundedAnswer() throws Exception {
        when(assistant.chat("How much leave do I get?")).thenReturn("Employees receive 20 days.");

        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"How much leave do I get?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("Employees receive 20 days."))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void rejectsBlankMessage() throws Exception {
        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }
}
