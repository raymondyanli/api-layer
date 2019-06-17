/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.gateway.health;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;

import com.ca.apiml.security.config.SecurityConfigurationProperties;
import com.ca.mfaas.product.constants.CoreService;

import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;

public class GatewayHealthIndicatorTest {

    private static final String ZOSMF = "zosmf";

    private SecurityConfigurationProperties securityConfigurationProperties;

    @Before
    public void setUp() {
        securityConfigurationProperties = new SecurityConfigurationProperties();
        securityConfigurationProperties.setZosmfServiceId(ZOSMF);
    }

    @Test
    public void testStatusIsUpWhenCatalogAndDiscoveryAreAvailable() {
        DiscoveryClient discoveryClient = mock(DiscoveryClient.class);
        when(discoveryClient.getInstances(CoreService.API_CATALOG.getServiceId())).thenReturn(
            Collections.singletonList(new DefaultServiceInstance(CoreService.API_CATALOG.getServiceId(), "host", 10014, true)));
        when(discoveryClient.getInstances(CoreService.DISCOVERY.getServiceId())).thenReturn(
            Collections.singletonList(new DefaultServiceInstance(CoreService.DISCOVERY.getServiceId(), "host", 10011, true)));
        when(discoveryClient.getInstances(ZOSMF)).thenReturn(
            Collections.singletonList(new DefaultServiceInstance(ZOSMF, "host", 10050, true)));

        GatewayHealthIndicator gatewayHealthIndicator = new GatewayHealthIndicator(discoveryClient, securityConfigurationProperties);
        Health.Builder builder = new Health.Builder();
        gatewayHealthIndicator.doHealthCheck(builder);
        assertEquals(Status.UP, builder.build().getStatus());
    }

    @Test
    public void testStatusIsDownWhenAuthIsNotAvailable() {
        DiscoveryClient discoveryClient = mock(DiscoveryClient.class);
        when(discoveryClient.getInstances(CoreService.API_CATALOG.getServiceId())).thenReturn(
            Collections.singletonList(new DefaultServiceInstance(CoreService.API_CATALOG.getServiceId(), "host", 10014, true)));
        when(discoveryClient.getInstances(CoreService.DISCOVERY.getServiceId())).thenReturn(
            Collections.singletonList(new DefaultServiceInstance(CoreService.DISCOVERY.getServiceId(), "host", 10014, true)));
        when(discoveryClient.getInstances(ZOSMF)).thenReturn(Collections.emptyList());

        GatewayHealthIndicator gatewayHealthIndicator = new GatewayHealthIndicator(discoveryClient, securityConfigurationProperties);
        Health.Builder builder = new Health.Builder();
        gatewayHealthIndicator.doHealthCheck(builder);
        assertEquals(Status.DOWN, builder.build().getStatus());
    }
}
