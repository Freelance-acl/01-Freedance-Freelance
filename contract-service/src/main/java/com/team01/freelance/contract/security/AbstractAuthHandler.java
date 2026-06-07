package com.team01.freelance.contract.security;

import java.io.IOException;

public abstract class AbstractAuthHandler implements AuthHandler {

    private AuthHandler next;

    @Override
    public void setNext(AuthHandler next) {
        this.next = next;
    }

    protected boolean delegate(AuthContext context) throws IOException {
        return next == null || next.handle(context);
    }
}
