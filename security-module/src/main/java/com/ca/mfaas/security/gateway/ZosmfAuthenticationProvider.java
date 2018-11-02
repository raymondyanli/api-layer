/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.security.gateway;

import com.ca.mfaas.product.config.MFaaSConfigPropertiesContainer;
import com.ca.mfaas.security.login.InvalidUserException;
import com.ca.mfaas.security.token.TokenAuthentication;
import com.ca.mfaas.security.token.TokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.Base64;
import java.util.List;

@Slf4j
@Component
public class ZosmfAuthenticationProvider implements AuthenticationProvider {

    private static final String ZOSMF_END_POINT = "zosmf/info";
    private static final String ZOSMF_CSRF_HEADER = "X-CSRF-ZOSMF-HEADER";
    private static final String ZOSMF_DOMAIN = "zosmf_saf_realm";

    private final TokenService tokenService;
    private final DiscoveryClient discovery;
    private final MFaaSConfigPropertiesContainer propertiesContainer;
    private final ObjectMapper securityObjectMapper;
    private final RestTemplate restTemplate;

    public ZosmfAuthenticationProvider(TokenService tokenService,
                                       DiscoveryClient discovery,
                                       MFaaSConfigPropertiesContainer propertiesContainer,
                                       ObjectMapper securityObjectMapper,
                                       RestTemplate restTemplate) {
        this.discovery = discovery;
        this.tokenService = tokenService;
        this.propertiesContainer = propertiesContainer;
        this.securityObjectMapper = securityObjectMapper;
        this.restTemplate = restTemplate;
    }

    @Override
    public Authentication authenticate(Authentication authentication) {
        String user = authentication.getPrincipal().toString();
        String password = authentication.getCredentials().toString();

        String zosmf = propertiesContainer.getGateway().getZosmfServiceName();
        if (zosmf == null || zosmf.isEmpty()) {
            log.error("zOSMF service name not found. Set property mfaas.security.zosmf to your service name.");
            throw new AuthenticationServiceException("A failure occurred when authenticating.");
        }

        String uri = getURI(zosmf);

        String credentials = user + ":" + password;
        String authorization = "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes());

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.AUTHORIZATION, authorization);
        headers.add(ZOSMF_CSRF_HEADER, "");

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                uri + ZOSMF_END_POINT,
                HttpMethod.GET,
                new HttpEntity<>(null, headers),
                String.class);

            String cookie = response.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
            String ltpaToken = readLtpaToken(cookie);
            String domain = readDomain(response.getBody());
            String jwtToken = tokenService.createToken(user, domain, ltpaToken);

            TokenAuthentication tokenAuthentication = new TokenAuthentication(user, jwtToken);
            tokenAuthentication.setAuthenticated(true);

            return tokenAuthentication;
        } catch (RestClientException | IOException e) {
            log.error("Can not access z/OSMF service. Uri '{}' returned: {}", uri, e.getMessage());
            throw new AuthenticationServiceException("A failure occurred when authenticating.", e);
        }
    }

    private String getURI(String zosmf) {
        String uri = null;

        List<ServiceInstance> zosmfInstances = discovery.getInstances(zosmf);
        if (zosmfInstances != null) {
            ServiceInstance zosmfInstance = zosmfInstances.get(0);
            if (zosmfInstance != null) {
                uri = zosmfInstance.getUri().toString();
            }
        }

        if (uri == null) {
            log.error("zOSMF instance '{}' not found or incorrectly configured.", zosmf);
            throw new AuthenticationServiceException("zOSMF instance not found or incorrectly configured.");
        }

        return uri;
    }

    private String readLtpaToken(String cookie) {
        String ltpaToken;

        if (cookie == null || cookie.isEmpty() || !cookie.contains("LtpaToken2")) {
            throw new InvalidUserException("Username or password are invalid.");
        } else {
            ltpaToken = cookie.substring(0, cookie.indexOf(';'));
        }

        return ltpaToken;
    }

    private String readDomain(String content) throws IOException {
        ObjectNode zosmfNode = securityObjectMapper.readValue(content, ObjectNode.class);
        if (zosmfNode.has(ZOSMF_DOMAIN)) {
            return zosmfNode.get(ZOSMF_DOMAIN).asText();
        } else {
            throw new AuthenticationServiceException("zOSMF domain cannot be read.");
        }
    }

    @Override
    public boolean supports(Class<?> auth) {
        return auth.equals(UsernamePasswordAuthenticationToken.class);
    }

}
