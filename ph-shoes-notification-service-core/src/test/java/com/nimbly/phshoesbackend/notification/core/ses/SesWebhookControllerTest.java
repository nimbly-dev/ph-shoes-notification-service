package com.nimbly.phshoesbackend.notification.core.ses;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SesWebhookController.class)
@AutoConfigureMockMvc(addFilters = false)
class SesWebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SesWebhookProcessor sesWebhookProcessor;

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
