package com.nimbly.phshoesbackend.notification.core.ses;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SesWebhookControllerTest {

    @Mock
    private SesWebhookProcessor sesWebhookProcessor;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        SesWebhookController controller = new SesWebhookController(sesWebhookProcessor);
        this.mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void forwardsPayloadToProcessor() throws Exception {
        mockMvc.perform(post("/internal/webhooks/ses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("x-amz-sns-message-type", "Notification")
                        .content("{\"Type\":\"Notification\"}"))
                .andExpect(status().isNoContent());

        verify(sesWebhookProcessor).process("{\"Type\":\"Notification\"}", "Notification");
    }

    @Test
    void surfacesProcessorErrors() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "bad"))
                .when(sesWebhookProcessor).process(null, null);

        mockMvc.perform(post("/internal/webhooks/ses"))
                .andExpect(status().isBadRequest());
    }
}
