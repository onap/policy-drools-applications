/*-
 * ============LICENSE_START=======================================================
 * so
 * ================================================================================
 * Copyright (C) 2017-2018 AT&T Intellectual Property. All rights reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END=========================================================
 */

package org.onap.policy.so;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.drools.core.WorkingMemory;
import org.onap.policy.drools.system.PolicyEngine;
import org.onap.policy.rest.RESTManager;
import org.onap.policy.rest.RESTManager.Pair;
import org.onap.policy.so.util.Serialization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class handles the interface towards SO (Service Orchestrator) for the ONAP Policy
 * Framework. The SO API is defined at this link:
 * http://onap.readthedocs.io/en/latest/submodules/so.git/docs/SO_R1_Interface.html#get-orchestration-request
 *
 */
public final class SOManager {
    private static final Logger logger = LoggerFactory.getLogger(SOManager.class);
    private static final Logger netLogger =
                    LoggerFactory.getLogger(org.onap.policy.common.endpoints.event.comm.Topic.NETWORK_LOGGER);
    private static ExecutorService executors = Executors.newCachedThreadPool();

    private static final int SO_RESPONSE_ERROR = 999;
    private static final String MEDIA_TYPE = "application/json";
    private static final String LINE_SEPARATOR = System.lineSeparator();

    // REST get timeout value in milliseconds
    private static final int GET_REQUESTS_BEFORE_TIMEOUT = 20;
    private static final long GET_REQUEST_WAIT_INTERVAL = 20000;

    // The REST manager used for processing REST calls for this VFC manager
    private RESTManager restManager;

    private long restGetTimeout = GET_REQUEST_WAIT_INTERVAL;

    /**
     * Default constructor.
     */
    public SOManager() {
        restManager = new RESTManager();
    }

    /**
     * Create a service instance in SO.
     *
     * @param url the SO URL
     * @param urlBase the base URL
     * @param username user name on SO
     * @param password password on SO
     * @param request the request to issue to SO
     * @return the SO Response object
     */
    public SOResponse createModuleInstance(final String url, final String urlBase, final String username,
                    final String password, final SORequest request) {
        // Issue the HTTP POST request to SO to create the service instance
        String requestJson = Serialization.gsonPretty.toJson(request);
        netLogger.info("[OUT|{}|{}|{}|{}|{}|{}|]{}{}", "SO", url, username, password, createSimpleHeaders(), MEDIA_TYPE,
                        LINE_SEPARATOR, requestJson);
        Pair<Integer, String> httpResponse =
                        restManager.post(url, username, password, createSimpleHeaders(), MEDIA_TYPE, requestJson);

        // Process the response from SO
        SOResponse response = waitForSoOperationCompletion(urlBase, username, password, url, httpResponse);
        if (SO_RESPONSE_ERROR != response.getHttpResponseCode()) {
            return response;
        } else {
            return null;
        }
    }

    /**
     * Works just like SOManager#asyncSORestCall(String, WorkingMemory, String, String, String, SORequest)
     * except the vfModuleInstanceId is always null.
     *
     */
    public Future<SOResponse> asyncSORestCall(final String requestId, final WorkingMemory wm,
                                              final String serviceInstanceId, final String vnfInstanceId,
                                              final SORequest request) {
        return asyncSORestCall(requestId, wm, serviceInstanceId, vnfInstanceId, null, request);
    }

    /**
     * This method makes an asynchronous Rest call to MSO and inserts the response into
     * Drools working memory.
     *
     * @param requestId          the request id
     * @param wm                 the Drools working memory
     * @param serviceInstanceId  service instance id to construct the request url
     * @param vnfInstanceId      vnf instance id to construct the request url
     * @param vfModuleInstanceId vfModule instance id to construct the request url (required in case of delete vf
     *                           module)
     * @param request            the SO request
     * @return a concurrent Future for the thread that handles the request
     */
    public Future<SOResponse> asyncSORestCall(final String requestId, 
            final WorkingMemory wm,
            final String serviceInstanceId, 
            final String vnfInstanceId, 
            final String vfModuleInstanceId, final SORequest request) {
        return executors.submit(new AsyncSoRestCallThread(requestId, wm, serviceInstanceId, vnfInstanceId,
                vfModuleInstanceId, request));
    }

    /**
     * This class handles an asynchronous request to SO as a thread.
     */
    private class AsyncSoRestCallThread implements Callable<SOResponse> {
        final String requestId;
        final WorkingMemory wm;
        final String serviceInstanceId;
        final String vnfInstanceId;
        final String vfModuleInstanceId;
        final SORequest request;

        /**
         * Constructor, sets the context of the request.
         *
         * @param requestID          The request ID
         * @param wm                 reference to the Drools working memory
         * @param serviceInstanceId  the service instance in SO to use
         * @param vnfInstanceId      the VNF instance that is the subject of the request
         * @param vfModuleInstanceId the vf module instance id (not null in case of delete vf module request)
         * @param request            the request itself
         */
        private AsyncSoRestCallThread(final String requestId, 
                final WorkingMemory wm, final String serviceInstanceId,
                final String vnfInstanceId, final String vfModuleInstanceId, 
                final SORequest request) {
            this.requestId = requestId;
            this.wm = wm;
            this.serviceInstanceId = serviceInstanceId;
            this.vnfInstanceId = vnfInstanceId;
            this.vfModuleInstanceId = vfModuleInstanceId;
            this.request = request;
        }

        /**
         * Process the asynchronous SO request.
         */
        @Override
        public SOResponse call() {
            String urlBase = PolicyEngine.manager.getEnvironmentProperty("so.url");
            String username = PolicyEngine.manager.getEnvironmentProperty("so.username");
            String password = PolicyEngine.manager.getEnvironmentProperty("so.password");

            // Create a JSON representation of the request
            String soJson = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create().toJson(request);
            String url = null;
            Pair<Integer, String> httpResponse = null;

            if (request.getOperationType() != null && request.getOperationType()
                    .equals(SoOperationType.SCALE_OUT)) {
                url = urlBase + "/serviceInstantiation/v7/serviceInstances/" + serviceInstanceId + "/vnfs/" + vnfInstanceId
                        + "/vfModules/scaleOut";
                netLogger.info("[OUT|{}|{}|]{}{}", "SO", url, LINE_SEPARATOR, soJson);
                httpResponse = restManager.post(url, username, password, createSimpleHeaders(), MEDIA_TYPE, soJson);
            } else if (request.getOperationType() != null && request.getOperationType()
                    .equals(SoOperationType.DELETE_VF_MODULE)) {
                url = urlBase + "/serviceInstances/v7/" + serviceInstanceId + "/vnfs/" + vnfInstanceId
                        + "/vfModules/" + vfModuleInstanceId;
                netLogger.info("[OUT|{}|{}|]{}{}", "SO", url, LINE_SEPARATOR, soJson);
                httpResponse = restManager.delete(url, username, password, createSimpleHeaders(), MEDIA_TYPE, soJson);
            } else {
                return null;
            }

            // Process the response from SO
            SOResponse response = waitForSoOperationCompletion(urlBase, username, password, url, httpResponse);

            // Return the response to Drools in its working memory
            SOResponseWrapper soWrapper = new SOResponseWrapper(response, requestId);
            wm.insert(soWrapper);

            return response;
        }
    }

    /**
     * Wait for the SO operation we have ordered to complete.
     *
     * @param urlBaseSO The base URL for SO
     * @param username user name on SO
     * @param password password on SO
     * @param initialRequestURL The URL of the initial HTTP request
     * @param initialHTTPResponse The initial HTTP message returned from SO
     * @return The parsed final response of SO to the request
     */
    private SOResponse waitForSoOperationCompletion(final String urlBaseSO, final String username,
                    final String password, final String initialRequestURL,
                    final Pair<Integer, String> initialHTTPResponse) {
        // Process the initial response from SO, the response to a post
        SOResponse response = processSoResponse(initialRequestURL, initialHTTPResponse);
        if (SO_RESPONSE_ERROR == response.getHttpResponseCode()) {
            return response;
        }

        // The SO URL to use to get the status of orchestration requests
        String urlGet = urlBaseSO + "/orchestrationRequests/v5/" + response.getRequestReferences().getRequestId();

        // The HTTP status code of the latest response
        Pair<Integer, String> latestHttpResponse = initialHTTPResponse;

        // Wait for the response from SO
        for (int attemptsLeft = GET_REQUESTS_BEFORE_TIMEOUT; attemptsLeft >= 0; attemptsLeft--) {
            // The SO request may have completed even on the first request so we check the
            // response
            // here before
            // issuing any other requests
            if (isRequestStateFinished(latestHttpResponse, response)) {
                return response;
            }

            // Wait for the defined interval before issuing a get
            try {
                Thread.sleep(restGetTimeout);
            } catch (InterruptedException e) {
                logger.error("Interrupted exception: ", e);
                Thread.currentThread().interrupt();
                response.setHttpResponseCode(SO_RESPONSE_ERROR);
                return response;
            }

            // Issue a GET to find the current status of our request
            netLogger.info("[OUT|{}|{}|{}|{}|{}|{}|]{}", "SO", urlGet, username, password, createSimpleHeaders(),
                            MEDIA_TYPE, LINE_SEPARATOR);
            Pair<Integer, String> httpResponse = restManager.get(urlGet, username, password, createSimpleHeaders());

            // Get our response
            response = processSoResponse(urlGet, httpResponse);
            if (SO_RESPONSE_ERROR == response.getHttpResponseCode()) {
                return response;
            }

            // Our latest HTTP response code
            latestHttpResponse = httpResponse;
        }

        // We have timed out on the SO request
        response.setHttpResponseCode(SO_RESPONSE_ERROR);
        return response;
    }

    /**
     * Parse the response message from SO into a SOResponse object.
     *
     * @param requestURL The URL of the HTTP request
     * @param httpResponse The HTTP message returned from SO
     * @return The parsed response
     */
    private SOResponse processSoResponse(final String requestUrl, final Pair<Integer, String> httpResponse) {
        SOResponse response = new SOResponse();

        // A null httpDetails indicates a HTTP problem, a valid response from SO must be
        // either 200
        // or 202
        if (!httpResultIsNullFree(httpResponse) || (httpResponse.first != 200 && httpResponse.first != 202)) {
            logger.error("Invalid HTTP response received from SO");
            response.setHttpResponseCode(SO_RESPONSE_ERROR);
            return response;
        }

        // Parse the JSON of the response into our POJO
        try {
            response = Serialization.gsonPretty.fromJson(httpResponse.second, SOResponse.class);
        } catch (JsonSyntaxException e) {
            logger.error("Failed to deserialize HTTP response into SOResponse: ", e);
            response.setHttpResponseCode(SO_RESPONSE_ERROR);
            return response;
        }

        // Set the HTTP response code of the response if needed
        if (response.getHttpResponseCode() == 0) {
            response.setHttpResponseCode(httpResponse.first);
        }

        netLogger.info("[IN|{}|{}|]{}{}", "SO", requestUrl, LINE_SEPARATOR, httpResponse.second);

        if (logger.isDebugEnabled()) {
            logger.debug("***** Response to SO Request to URL {}:", requestUrl);
            logger.debug(httpResponse.second);
        }

        return response;
    }

    /**
     * Method to allow tuning of REST get timeout.
     *
     * @param restGetTimeout the timeout value
     */
    protected void setRestGetTimeout(final long restGetTimeout) {
        this.restGetTimeout = restGetTimeout;
    }

    /**
     * Check that the request state of a response is defined.
     *
     * @param response The response to check
     * @return true if the request for the response is defined
     */
    private boolean isRequestStateDefined(final SOResponse response) {
        return response != null && response.getRequest() != null && response.getRequest().getRequestStatus() != null
                        && response.getRequest().getRequestStatus().getRequestState() != null;
    }

    /**
     * Check that the request state of a response is finished.
     *
     * @param latestHttpDetails the HTTP details of the response
     * @param response The response to check
     * @return true if the request for the response is finished
     */
    private boolean isRequestStateFinished(final Pair<Integer, String> latestHttpDetails, final SOResponse response) {
        if (latestHttpDetails != null && 200 == latestHttpDetails.first && isRequestStateDefined(response)) {
            String requestState = response.getRequest().getRequestStatus().getRequestState();
            return "COMPLETE".equalsIgnoreCase(requestState) || "FAILED".equalsIgnoreCase(requestState);
        } else {
            return false;
        }
    }

    /**
     * Check that a HTTP operation result has no nulls.
     *
     * @param httpOperationResult the result to check
     * @return true if no nulls are found
     */
    private boolean httpResultIsNullFree(Pair<Integer, String> httpOperationResult) {
        return httpOperationResult != null && httpOperationResult.first != null && httpOperationResult.second != null;
    }

    /**
     * Create simple HTTP headers for unauthenticated requests to SO.
     *
     * @return the HTTP headers
     */
    private Map<String, String> createSimpleHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Accept", MEDIA_TYPE);
        return headers;
    }
}
