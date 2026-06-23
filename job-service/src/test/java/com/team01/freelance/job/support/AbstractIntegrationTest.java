package com.team01.freelance.job.support;

import com.team01.freelance.job.feign.ContractServiceClient;
import com.team01.freelance.job.feign.ProposalServiceClient;
import com.team01.freelance.job.messaging.publishers.JobEventPublisher;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import com.team01.freelance.job.TestJobServiceApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

@SpringBootTest(classes = TestJobServiceApplication.class)
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    @MockitoBean
    protected ContractServiceClient contractServiceClient;

    @MockitoBean
    protected ProposalServiceClient proposalServiceClient;

    @MockitoBean
    protected JobEventPublisher jobEventPublisher;

    @MockitoBean
    protected RabbitTemplate rabbitTemplate;

    protected static org.springframework.test.web.servlet.MockMvc buildMockMvc(WebApplicationContext context) {
        return MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }
}
