/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.gateway.security.handler;

import com.ca.mfaas.error.ErrorService;
import com.ca.mfaas.constants.ApimlConstants;
import com.ca.mfaas.rest.response.ApiMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Handles unauthorized access
 */
@Slf4j
@Component
public class UnauthorizedHandler implements AuthenticationEntryPoint {
    private final ErrorService errorService;
    private final ObjectMapper mapper;

    public UnauthorizedHandler(ErrorService errorService, ObjectMapper objectMapper) {
        this.errorService = errorService;
        this.mapper = objectMapper;
    }

    /**
     * Creates unauthorized response with the appropriate message and http status
     *
     * @param request       the http request
     * @param response      the http response
     * @param authException the authorization exception
     * @throws IOException when the response cannot be written
     */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException {
        log.debug("Unauthorized access to '{}' endpoint", request.getRequestURI());

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_UTF8_VALUE);
        response.addHeader(HttpHeaders.WWW_AUTHENTICATE, ApimlConstants.BASIC_AUTHENTICATION_PREFIX);

        ApiMessage message = errorService.createApiMessage("apiml.gateway.security.login.invalidCredentials", request.getRequestURI());
        mapper.writeValue(response.getWriter(), message);
    }
}
