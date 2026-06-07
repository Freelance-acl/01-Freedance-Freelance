package com.team01.freelance.contract.security;

import java.io.IOException;

public interface AuthHandler {
    void setNext(AuthHandler next);

    boolean handle(AuthContext context) throws IOException;
}
