/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.gatewayservice;


import com.ca.apiml.security.login.LoginRequest;
import com.ca.mfaas.utils.config.ConfigReader;
import com.ca.mfaas.utils.config.GatewayServiceConfiguration;
import com.ca.mfaas.utils.config.TlsConfiguration;
import com.ca.mfaas.utils.config.ZosmfServiceConfiguration;
import com.netflix.discovery.shared.transport.jersey.SSLSocketFactoryAdapter;
import io.restassured.config.SSLConfig;
import org.apache.http.conn.ssl.DefaultHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.ssl.SSLContexts;

import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;

public class SecurityUtils {
    public final static String ZOSMF_TOKEN = "LtpaToken2";

    private final static String GATEWAY_TOKEN = "apimlAuthenticationToken";
    private final static String GATEWAY_LOGIN_ENDPOINT = "/auth/login";
    private final static String GATEWAY_BASE_PATH = "/api/v1/gateway";
    private final static String ZOSMF_LOGIN_ENDPOINT = "/zosmf/info";

    private final static GatewayServiceConfiguration serviceConfiguration = ConfigReader.environmentConfiguration().getGatewayServiceConfiguration();
    private final static ZosmfServiceConfiguration zosmfServiceConfiguration = ConfigReader.environmentConfiguration().getZosmfServiceConfiguration();

    private final static String gatewayScheme = serviceConfiguration.getScheme();
    private final static String gatewayHost = serviceConfiguration.getHost();
    private final static int gatewayPort = serviceConfiguration.getPort();

    private final static String zosmfScheme = zosmfServiceConfiguration.getScheme();
    private final static String zosmfHost = zosmfServiceConfiguration.getHost();
    private final static int zosmfPort = zosmfServiceConfiguration.getPort();

    //@formatter:off
    public static String gatewayToken(String username, String password) {
        LoginRequest loginRequest = new LoginRequest(username, password);

        return given()
            .contentType(JSON)
            .body(loginRequest)
        .when()
            .post(String.format("%s://%s:%d%s%s", gatewayScheme, gatewayHost, gatewayPort, GATEWAY_BASE_PATH, GATEWAY_LOGIN_ENDPOINT))
        .then()
            .statusCode(is(SC_NO_CONTENT))
            .cookie(GATEWAY_TOKEN, not(isEmptyString()))
            .extract().cookie(GATEWAY_TOKEN);
    }

    public static String zosmfToken(String username, String password) {
        return given()
            .auth().preemptive().basic(username, password)
            .header("X-CSRF-ZOSMF-HEADER", "zosmf")
        .when()
            .get(String.format("%s://%s:%d%s", zosmfScheme, zosmfHost, zosmfPort, ZOSMF_LOGIN_ENDPOINT))
        .then()
            .statusCode(is(SC_OK))
            .cookie(ZOSMF_TOKEN, not(isEmptyString()))
            .extract().cookie(ZOSMF_TOKEN);
    }

    public static SSLConfig getConfiguredSslConfig() {
        TlsConfiguration tlsConfiguration = ConfigReader.environmentConfiguration().getTlsConfiguration();
        try {
            SSLContext sslContext = SSLContexts.custom()
                .loadKeyMaterial(
                    new File(tlsConfiguration.getKeyStore()),
                    tlsConfiguration.getKeyStorePassword() != null ? tlsConfiguration.getKeyStorePassword().toCharArray() : null,
                    tlsConfiguration.getKeyPassword() != null ? tlsConfiguration.getKeyPassword().toCharArray() : null,
                    (aliases, socket) -> tlsConfiguration.getKeyAlias())
                .loadTrustMaterial(
                    new File(tlsConfiguration.getTrustStore()),
                    tlsConfiguration.getTrustStorePassword() != null ? tlsConfiguration.getTrustStorePassword().toCharArray() : null)
                .build();
            SSLSocketFactoryAdapter sslSocketFactory = new SSLSocketFactoryAdapter(new SSLConnectionSocketFactory(sslContext, new DefaultHostnameVerifier()));
            return SSLConfig.sslConfig().with().sslSocketFactory(sslSocketFactory);
        } catch (KeyManagementException | UnrecoverableKeyException | NoSuchAlgorithmException | KeyStoreException
            | CertificateException | IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    //@formatter:on
}
