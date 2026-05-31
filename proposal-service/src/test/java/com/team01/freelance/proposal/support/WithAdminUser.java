package com.team01.freelance.proposal.support;

import org.springframework.security.test.context.support.WithMockUser;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Applies {@link WithMockUser}(ADMIN) to integration test classes; inherited by subclasses. */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@WithMockUser(roles = "ADMIN")
public @interface WithAdminUser {
}
