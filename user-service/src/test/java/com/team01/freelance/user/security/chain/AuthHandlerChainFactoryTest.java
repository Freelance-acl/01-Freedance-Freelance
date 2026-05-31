package com.team01.freelance.user.security.chain;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("test")
class AuthHandlerChainFactoryTest {

    @Autowired
    private AuthHandlerChainFactory chainFactory;

    @Test
    void buildChain_linksFourHandlersInOrder() throws Exception {
        AuthHandler chain = chainFactory.buildChain();
        assertNotNull(chain);
        assertInstanceOf(TokenExtractionHandler.class, chain);

        List<Class<?>> handlerTypes = new ArrayList<>();
        AuthHandler current = chain;
        while (current != null) {
            handlerTypes.add(current.getClass());
            current = nextHandler(current);
        }

        assertEquals(
                List.of(
                        TokenExtractionHandler.class,
                        SignatureValidationHandler.class,
                        UserLoaderHandler.class,
                        RoleAuthorizationHandler.class),
                handlerTypes);
    }

    private static AuthHandler nextHandler(AuthHandler handler) throws Exception {
        Field next = AuthHandler.class.getDeclaredField("next");
        next.setAccessible(true);
        return (AuthHandler) next.get(handler);
    }
}
