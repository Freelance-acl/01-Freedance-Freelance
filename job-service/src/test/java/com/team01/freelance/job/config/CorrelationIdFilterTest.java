package com.team01.freelance.job.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import java.util.Collection;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

class CorrelationIdFilterTest {

    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new MdcProbeController())
            .addFilters(new CorrelationIdFilter())
            .build();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void incomingCorrelationIdIsAvailableInMdcDuringRequestHandling() throws Exception {
        String correlationId = "job-correlation-123";

        MvcResult result = mockMvc.perform(get("/test/mdc")
                        .header(CorrelationIdFilter.HEADER, correlationId)
                        .accept(MediaType.TEXT_PLAIN))
                .andExpect(status().isOk())
                .andExpect(header().string(CorrelationIdFilter.HEADER, correlationId))
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).isEqualTo(correlationId);
        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
    }

    @Test
    void missingCorrelationIdGeneratesNonBlankValueAndClearsMdc() throws Exception {
        MvcResult result = mockMvc.perform(get("/test/mdc").accept(MediaType.TEXT_PLAIN))
                .andExpect(status().isOk())
                .andReturn();

        String generated = result.getResponse().getContentAsString();
        assertThat(generated).isNotBlank();
        assertThat(result.getResponse().getHeader(CorrelationIdFilter.HEADER)).isEqualTo(generated);
        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
    }

    @Test
    void blankCorrelationIdGeneratesNonBlankValueAndClearsMdc() throws Exception {
        MvcResult result = mockMvc.perform(get("/test/mdc")
                        .header(CorrelationIdFilter.HEADER, "   ")
                        .accept(MediaType.TEXT_PLAIN))
                .andExpect(status().isOk())
                .andReturn();

        String generated = result.getResponse().getContentAsString();
        assertThat(generated).isNotBlank();
        assertThat(generated).isNotEqualTo("   ");
        assertThat(result.getResponse().getHeader(CorrelationIdFilter.HEADER)).isEqualTo(generated);
        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
    }

    @Test
    void feignInterceptorPropagatesCorrelationIdFromMdc() {
        String correlationId = "outbound-correlation-456";
        MDC.put(CorrelationIdFilter.MDC_KEY, correlationId);
        RequestInterceptor interceptor = new FeignCorrelationConfig().correlationIdInterceptor();
        RequestTemplate template = new RequestTemplate();

        interceptor.apply(template);

        Map<String, Collection<String>> headers = template.headers();
        assertThat(headers).containsKey(CorrelationIdFilter.HEADER);
        assertThat(headers.get(CorrelationIdFilter.HEADER)).containsExactly(correlationId);
    }

    @Controller
    private static class MdcProbeController {

        @GetMapping(value = "/test/mdc", produces = MediaType.TEXT_PLAIN_VALUE)
        @ResponseBody
        String correlationIdFromMdc() {
            return MDC.get(CorrelationIdFilter.MDC_KEY);
        }
    }
}
