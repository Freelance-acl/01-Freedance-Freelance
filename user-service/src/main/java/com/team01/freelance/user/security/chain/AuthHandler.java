package com.team01.freelance.user.security.chain;

/**
 * DP-3 Chain of Responsibility — one step in JWT authentication.
 */
public abstract class AuthHandler {

    private AuthHandler next;

    public AuthHandler setNext(AuthHandler next) {
        this.next = next;
        return next;
    }

    public final void handle(AuthContext context) {
        if (context.hasFailed()) {
            return;
        }
        doHandle(context);
        if (!context.hasFailed() && next != null) {
            next.handle(context);
        }
    }

    protected abstract void doHandle(AuthContext context);
}
