/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.product.routing;

import com.ca.mfaas.product.utils.UrlUtils;

import java.util.HashMap;
import java.util.Map;

public class RoutedServices {
    public static final String ROUTED_SERVICES_PARAMETER = "routed-services";
    public static final String GATEWAY_URL_PARAMETER = "gateway-url";
    public static final String SERVICE_URL_PARAMETER = "service-url";

    private final Map<String, RoutedService> routedService = new HashMap<>();

    /**
     * Add route to the service
     *
     * @param route the route
     */
    public void addRoutedService(RoutedService route) {
        routedService.put(route.getGatewayUrl(), route);
    }

    /**
     * Find RoutedService by Gateway Url
     *
     * @param gatewayUrl the url of gateway
     * @return the route
     */
    public RoutedService findServiceByGatewayUrl(String gatewayUrl) {
        return routedService.get(gatewayUrl);
    }

    /**
     * Get best matching service url
     *
     * @param serviceUrl service url
     * @param type  service type
     * @return the route
     */
    public RoutedService getBestMatchingServiceUrl(String serviceUrl, ServiceType type) {
        RoutedService result = null;
        int maxSize = 0;

        for (Map.Entry<String, RoutedService> serviceEntry : routedService.entrySet()) {
            if (!type.equals(ServiceType.ALL)
                && !serviceEntry.getKey().toLowerCase().startsWith(type.name().toLowerCase())) {
                continue;
            }

            RoutedService value = serviceEntry.getValue();
            int size = value.getServiceUrl().length();
            //Remove last slash for service url
            String routeServiceUrl = UrlUtils.removeLastSlash(value.getServiceUrl().toLowerCase());
            if (size > maxSize &&
                serviceUrl.toLowerCase().startsWith(routeServiceUrl)) {
                result = value;
                maxSize = size;
            }
        }

        return result;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("[");
        for (Map.Entry<String, RoutedService> route : routedService.entrySet()) {
            builder.append(route.getKey());
            builder.append(" -> ");
            builder.append(route.toString());
            builder.append(", ");
        }
        if (routedService.size() > 0) {
            builder.setLength(builder.length() - 2); // Remove extra ", "
        }
        builder.append("]");
        return builder.toString();
    }
}
