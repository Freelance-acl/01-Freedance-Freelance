package com.team01.freelance.contract.controllers;

import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Map resource paths relative to the servlet root (e.g. {@code /health}). The public URL prefix
 * is {@code server.servlet.context-path} in {@code application.properties} (not repeated here).
 */
@RequestMapping("/")
public abstract class BaseApiController {
}
