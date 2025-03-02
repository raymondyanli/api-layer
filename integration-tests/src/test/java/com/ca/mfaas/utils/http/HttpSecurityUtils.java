/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.utils.http;

import com.ca.mfaas.utils.config.ConfigReader;
import com.ca.mfaas.utils.config.Credentials;
import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;

import java.io.IOException;
import java.net.URI;

public class HttpSecurityUtils {
    private static final String GATEWAY_LOGIN_ENDPOINT = "/api/v1/gateway/auth/login/";

    public static String getCookieForGateway() throws IOException {
        Credentials credentials = ConfigReader.environmentConfiguration().getCredentials();
        String user = credentials.getUser();
        String password = credentials.getPassword();
        URI uri = HttpRequestUtils.getUriFromGateway(GATEWAY_LOGIN_ENDPOINT);

        return getCookie(uri, user, password);
    }

    public static String getCookie(URI loginUrl, String user, String password) throws IOException {
        HttpPost request = new HttpPost(loginUrl);
        HttpClient client = HttpClientUtils.client();
        String credentials = String.format("{\"username\":\"%s\", \"password\":\"%s\"}", user, password);
        StringEntity payload = new StringEntity(credentials);
        request.setEntity(payload);
        request.setHeader("Content-type", "application/json");

        HttpResponse response = client.execute(request);
        Header cookie = response.getFirstHeader("Set-Cookie");
        if (cookie == null) {
            return null;
        }
        return response.getFirstHeader("Set-Cookie").getValue();
    }

    public static void addCookie(HttpRequest request, String cookie) {
        request.addHeader("Cookie", cookie);
    }
}
