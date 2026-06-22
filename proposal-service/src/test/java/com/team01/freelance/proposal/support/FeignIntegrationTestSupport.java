package com.team01.freelance.proposal.support;

import com.team01.freelance.proposal.feign.ContractServiceClient;
import com.team01.freelance.proposal.feign.JobServiceClient;
import com.team01.freelance.proposal.feign.UserServiceClient;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Shared Feign client mocks for decoupled integration tests.
 */
public abstract class FeignIntegrationTestSupport extends AbstractIntegrationTest {

    @MockitoBean
    protected JobServiceClient jobServiceClient;

    @MockitoBean
    protected UserServiceClient userServiceClient;

    @MockitoBean
    protected ContractServiceClient contractServiceClient;
}
