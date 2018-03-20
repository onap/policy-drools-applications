/*-
 * ============LICENSE_START=======================================================
 * so
 * ================================================================================
 * Copyright (C) 2017 AT&T Intellectual Property. All rights reserved.
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

import java.util.Base64;
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
 * This class handles the interface towards SO (Service Orchestrator) for the ONAP Policy Framework. The SO
 * API is defined at this link:
 * http://onap.readthedocs.io/en/latest/submodules/so.git/docs/SO_R1_Interface.html#get-orchestration-request
 *  
 */
public final class SOManager {
    private static final Logger logger = LoggerFactory.getLogger(SOManager.class);
    private static final Logger netLogger = LoggerFactory.getLogger(org.onap.policy.drools.event.comm.Topic.NETWORK_LOGGER);
    private static ExecutorService executors = Executors.newCachedThreadPool();

    private static final int SO_RESPONSE_ERROR = 999;
    private static final String MEDIA_TYPE = "application/json";
    private static final String LINE_SEPARATOR = System.lineSeparator();

    // REST get timeout value in milliseconds
    private static final int  GET_REQUESTS_BEFORE_TIMEOUT = 20;
    private static final long GET_REQUEST_WAIT_INTERVAL = 20000;

    // The REST manager used for processing REST calls for this VFC manager
    private RESTManager restManager;

    private long restGetTimeout = GET_REQUEST_WAIT_INTERVAL;

    /**
     * Default constructor
     */
    public SOManager() {
        restManager = new RESTManager();
    }

    /**
     * Create a service instance in SO.
     * @param url the SO URL
     * @param urlBase the base URL
     * @param username user name on SO
     * @param password password on SO
     * @param request the request to issue to SO
     * @return
     */
    public SOResponse createModuleInstance(final String url, final String urlBase, final String username, final String password, final SORequest request) {
        // Issue the HTTP POST request to SO to create the service instance
        String requestJson = Serialization.gsonPretty.toJson(request);
        netLogger.info("[OUT|{}|{}|{}|{}|{}|{}|]{}{}", "SO", url, username, password, createSimpleHeaders(), MEDIA_TYPE, LINE_SEPARATOR, requestJson);
        Pair<Integer, String> httpResponse = restManager.post(url, username, password, createSimpleHeaders(), MEDIA_TYPE, requestJson);

        // Process the response from SO
        SOResponse response =  waitForSOOperationCompletion(urlBase, username, password, url, httpResponse);
        if (SO_RESPONSE_ERROR != response.getHttpResponseCode()) {
            return response;
        }
        else {
            return null;
        }
    }
    
    /**
     * This method makes an asynchronous Rest call to MSO and inserts the response into Drools working memory.
     * @param wm the Drools working memory
     * @param url the URL to use on the POST request
     * @param urlBase the SO base URL
     * @param username user name for SO requests
     * @param password password for SO requests
     * @param request the SO request
     * @return a concurrent Future for the thread that handles the request
     */
    public Future<SOResponse> asyncSORestCall(final String requestID, final WorkingMemory wm, final String serviceInstanceId, final String vnfInstanceId, final SORequest request) {
        return executors.submit(new AsyncSORestCallThread(requestID, wm, serviceInstanceId, vnfInstanceId, request));
    }

    /**
     * This class handles an asynchronous request to SO as a thread.
     */
    private class AsyncSORestCallThread implements Callable<SOResponse> {
        final String requestID;
        final WorkingMemory wm;
        final String serviceInstanceId;
        final String vnfInstanceId;
        final SORequest request;

        /**
         * Constructor, sets the context of the request.
         * @param requestID The request ID
         * @param wm reference to the Drools working memory
         * @param serviceInstanceId the service instance in SO to use
         * @param vnfInstanceId the VNF instance that is the subject of the request
         * @param request the request itself
         */
        private AsyncSORestCallThread(final String requestID, final WorkingMemory wm, final String serviceInstanceId,    final String vnfInstanceId, final SORequest request) {
            this.requestID = requestID;
            this.wm = wm;
            this.serviceInstanceId = serviceInstanceId;
            this.vnfInstanceId = vnfInstanceId;
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

            // The URL of the request we will POST
            String url = urlBase + "/serviceInstances/v2/" + serviceInstanceId + "/vnfs/"
                    + vnfInstanceId + "/vfModules";

            // Create a JSON representation of the request
            String soJson = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create().toJson(request);

            netLogger.info("[OUT|{}|{}|]{}{}", "SO", url, LINE_SEPARATOR, soJson);
            Pair<Integer, String> httpResponse = restManager.post(url, "policy",    "policy", createAuthenticateHeaders(username, password), MEDIA_TYPE, soJson);

            // Process the response from SO
            SOResponse response =  waitForSOOperationCompletion(urlBase, username, password, url, httpResponse);
            
            // Return the response to Drools in its working memory
            SOResponseWrapper soWrapper = new SOResponseWrapper(response, requestID);
            wm.insert(soWrapper);
            
            return response;
        }
        
        /**
         * Create HTTP headers for authenticated requests to SO.
         * @param username user name on SO
         * @param password password on SO
         * @return the HTTP headers
         */
        private Map<String, String> createAuthenticateHeaders(final String username, final String password) {
            String auth = username + ":" + password;

            Map<String, String> headers = new HashMap<>();
            byte[] encodedBytes = Base64.getEncoder().encode(auth.getBytes());
            headers.put("Accept", MEDIA_TYPE);
            headers.put("Authorization", "Basic " + new String(encodedBytes));
            
            return headers;
        }
    }

    /**
     * Wait for the SO operation we have ordered to complete.
     * @param urlBaseSO The base URL for SO 
     * @param username user name on SO
     * @param password password on SO
     * @param initialRequestURL The URL of the initial HTTP request
     * @param initialHTTPResponse The initial HTTP message returned from SO
     * @return The parsed final response of SO to the request 
     */
    private SOResponse waitForSOOperationCompletion(final String urlBaseSO, final String username, final String password,
            final String initialRequestURL, final Pair<Integer, String> initialHTTPResponse) {
        // Process the initial response from SO, the response to a post
        SOResponse response = processSOResponse(initialRequestURL, initialHTTPResponse);
        if (SO_RESPONSE_ERROR == response.getHttpResponseCode()) {
            return response;
        }

        // The SO URL to use to get the status of orchestration requests
        String urlGet = urlBaseSO + "/orchestrationRequests/v2/" + response.getRequestReferences().getRequestId();

        // The HTTP status code of the latest response
        Pair<Integer, String> latestHTTPResponse = initialHTTPResponse;

        // Wait for the response from SO
        for (int attemptsLeft = GET_REQUESTS_BEFORE_TIMEOUT; attemptsLeft >= 0; attemptsLeft--) {
            // The SO request may have completed even on the first request so we check the response here before
            // issuing any other requests
            if (isRequestStateFinished(latestHTTPResponse, response)) {
                return response;
            }

            // Wait for the defined interval before issuing a get
            try {
                Thread.sleep(restGetTimeout);
            }
            catch (InterruptedException e) {
                logger.error("Interrupted exception: ", e);
                Thread.currentThread().interrupt();
                response.setHttpResponseCode(SO_RESPONSE_ERROR);
                return response;
            }
            
            // Issue a GET to find the current status of our request
            netLogger.info("[OUT|{}|{}|{}|{}|{}|{}|]{}", "SO", urlGet, username, password, createSimpleHeaders(), MEDIA_TYPE, LINE_SEPARATOR);
            Pair<Integer, String> httpResponse = restManager.get(urlGet, username, password, createSimpleHeaders());

            // Get our response
            response = processSOResponse(urlGet, httpResponse);
            if (SO_RESPONSE_ERROR == response.getHttpResponseCode()) {
                return response;
            }
            
            // Our latest HTTP response code
            latestHTTPResponse = httpResponse;
        }
        
        // We have timed out on the SO request
        response.setHttpResponseCode(SO_RESPONSE_ERROR);
        return response;
    }

    /**
     * Parse the response message from SO into a SOResponse object.
     * @param requestURL  The URL of the HTTP request
     * @param httpDetails The HTTP message returned from SO
     * @return The parsed response
     */
    private SOResponse processSOResponse(final String requestURL, final Pair<Integer, String> httpResponse) {
        SOResponse response = new SOResponse();

        // A null httpDetails indicates a HTTP problem, a valid response from SO must be either 200 or 202
        if (!httpResultIsNullFree(httpResponse) || (httpResponse.a != 200 && httpResponse.a != 202)) {
            logger.error("Invalid HTTP response received from SO");
            response.setHttpResponseCode(SO_RESPONSE_ERROR);
            return response;
        }

        // Parse the JSON of the response into our POJO
        try {
            response = Serialization.gsonPretty.fromJson(httpResponse.b, SOResponse.class);
        }
        catch (JsonSyntaxException e) {
            logger.error("Failed to deserialize HTTP response into SOResponse: ", e);
            response.setHttpResponseCode(SO_RESPONSE_ERROR);
            return response;
        }

        // Set the HTTP response code of the response if needed
        if (response.getHttpResponseCode() == 0) {
        		response.setHttpResponseCode(httpResponse.a);
        }
        
        netLogger.info("[IN|{}|{}|]{}{}", "SO", requestURL, LINE_SEPARATOR, httpResponse.b);

        if (logger.isDebugEnabled()) {
            logger.debug("***** Response to SO Request to URL {}:", requestURL);
            logger.debug(httpResponse.b);
        }

        return response;
    }

    /**
     * Method to allow tuning of REST get timeout. 
     * @param restGetTimeout the timeout value
     */
    protected void setRestGetTimeout(final long restGetTimeout) {
        this.restGetTimeout = restGetTimeout;
    }

    /**
     * Check that the request state of a response is defined.
     * @param response The response to check
     * @return true if the request for the response is defined
     */
    private boolean isRequestStateDefined(final SOResponse response) {
        return response != null &&
                response.getRequest() != null &&
                response.getRequest().getRequestStatus() != null &&
                response.getRequest().getRequestStatus().getRequestState() != null;
    }

    /**
     * Check that the request state of a response is finished.
     * @param latestHTTPDetails the HTTP details of the response 
     * @param response The response to check
     * @return true if the request for the response is finished
     */
    private boolean isRequestStateFinished(final Pair<Integer, String> latestHTTPDetails, final SOResponse response) {
        if (latestHTTPDetails != null && 200 == latestHTTPDetails.a && isRequestStateDefined(response)) {
            String requestState = response.getRequest().getRequestStatus().getRequestState();
            return "COMPLETE".equalsIgnoreCase(requestState) || "FAILED".equalsIgnoreCase(requestState);
        }
        else {
            return false;
        }
    }

    /**
     * Check that a HTTP operation result has no nulls.
     * @param httpOperationResult the result to check
     * @return true if no nulls are found
     */
    private boolean httpResultIsNullFree(Pair<Integer, String> httpOperationResult) {
        return httpOperationResult != null && httpOperationResult.a != null && httpOperationResult.b != null;
    }

    /**
     * Create simple HTTP headers for unauthenticated requests to SO.
     * @return the HTTP headers
     */
    private Map<String, String> createSimpleHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Accept", MEDIA_TYPE);
        return headers;
    }
}
