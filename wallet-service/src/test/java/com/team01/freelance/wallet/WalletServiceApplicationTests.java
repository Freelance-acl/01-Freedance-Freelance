package com.team01.freelance.wallet;

import com.team01.freelance.wallet.config.TestApplicationConfig;
import com.team01.freelance.wallet.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class WalletServiceApplicationTests extends AbstractIntegrationTest {

    @Test
    void contextLoads() {
    }

    @Test
    void runApplicationConfigTests() throws Exception {
        TestApplicationConfig configTest = new TestApplicationConfig();
        configTest.verifyWalletServiceMilestone2ConfigSpecs();
    }
}