/*-
 * ============LICENSE_START=======================================================
 * guard
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

package org.onap.policy.guard;

import com.att.research.xacml.api.Attribute;
import com.att.research.xacml.api.AttributeCategory;
import com.att.research.xacml.api.AttributeValue;
import com.att.research.xacml.api.Result;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Iterator;
import java.util.Properties;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.apache.http.entity.ContentType;
import org.json.JSONObject;
import org.onap.policy.drools.system.PolicyEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class PolicyGuardXacmlHelper {
    private static final Logger logger = LoggerFactory.getLogger(PolicyGuardXacmlHelper.class);
    private static final Logger netLogger =
            LoggerFactory.getLogger(org.onap.policy.common.endpoints.event.comm.Topic.NETWORK_LOGGER);

    // Constant for the systme line separator
    private static final String SYSTEM_LS = System.lineSeparator();

    public PolicyGuardXacmlHelper() {
        init(PolicyEngine.manager.getEnvironment());
    }

    // initialized from 'pdpx.url' property --
    // Each entry in 'restUrls' contains a destination URL, and an optional
    // 'Authorization' header entry. 'restUrlIndex' indicates the next
    // entry to try -- after each failure, the index is advanced to the
    // next entry (wrapping to the beginning, if needed).
    private static class UrlEntry implements Serializable {
        private static final long serialVersionUID = -8859237552195400518L;

        URL restUrl;
        String authorization = null;
        String clientAuth = null;
        String environment = null;
    }

    private UrlEntry[] restUrls = null;
    private int restUrlIndex = 0;

    // REST timeout, initialized from 'pdpx.timeout' property
    private int timeout = 20000;

    /**
     * Call PDP.
     * 
     * @param xacmlReq the XACML request
     * @return the response
     */
    public String callPDP(PolicyGuardXacmlRequestAttributes xacmlReq) {
        //
        // Send it to the PDP
        //
        String response = null;

        //
        // Build the json request
        //
        JSONObject attributes = new JSONObject();
        attributes.put("actor", xacmlReq.getActorID());
        attributes.put("recipe", xacmlReq.getOperationID());
        attributes.put("target", xacmlReq.getTargetID());
        if (xacmlReq.getClnameID() != null) {
            attributes.put("clname", xacmlReq.getClnameID());
        }
        if (xacmlReq.getVfCount() != null) {
            attributes.put("vfCount", xacmlReq.getVfCount());
        }
        JSONObject jsonReq = new JSONObject();
        jsonReq.put("decisionAttributes", attributes);
        jsonReq.put("onapName", "PDPD");


        try {
            //
            // Call RESTful PDP
            //
            UrlEntry urlEntry = restUrls[restUrlIndex];
            String jsonRequestString = jsonReq.toString();
            netLogger.info("[OUT|{}|{}|]{}{}", "GUARD", urlEntry.restUrl, SYSTEM_LS, jsonRequestString);
            response = callRESTfulPDP(new ByteArrayInputStream(jsonReq.toString().getBytes()), urlEntry.restUrl,
                    urlEntry.authorization, urlEntry.clientAuth, urlEntry.environment);
            netLogger.info("[IN|{}|{}|]{}{}", "GUARD", urlEntry.restUrl, SYSTEM_LS, response);
        } catch (Exception e) {
            logger.error("Error in sending RESTful request: ", e);
        }

        return response;
    }

    /**
     * This makes an HTTP POST call to a running PDP RESTful servlet to get a decision.
     * 
     * @param is the InputStream
     * @param authorization the Authorization
     * @param clientauth the ClientAuth
     * @param environment the Environment
     * @return response from guard which contains "Permit" or "Deny"
     */
    private String callRESTfulPDP(InputStream is, URL restURL, String authorization, String clientauth,
            String environment) {
        HttpURLConnection connection = null;

        try {
            //
            // Open up the connection
            //
            connection = (HttpURLConnection) restURL.openConnection();
            connection.setRequestProperty("Content-Type", "application/json");
            //
            // Setup our method and headers
            //
            connection.setRequestProperty("Accept", "application/json");
            if (authorization != null) {
                connection.setRequestProperty("Authorization", authorization);
            }
            if (clientauth != null) {
                connection.setRequestProperty("ClientAuth", clientauth);
            }
            if (environment != null) {
                connection.setRequestProperty("Environment", environment);
            }
            connection.setConnectTimeout(timeout);
            connection.setReadTimeout(timeout);
            connection.setRequestMethod("POST");
            connection.setUseCaches(false);
            //
            // Adding this in. It seems the HttpUrlConnection class does NOT
            // properly forward our headers for POST re-direction. It does so
            // for a GET re-direction.
            //
            // So we need to handle this ourselves.
            //
            connection.setInstanceFollowRedirects(false);
            connection.setDoOutput(true);
            connection.setDoInput(true);
            //
            // Send the request
            //
            try (OutputStream os = connection.getOutputStream()) {
                IOUtils.copy(is, os);
            }

            //
            // Do the connect
            //
            connection.connect();

            if (connection.getResponseCode() != 200) {
                logger.error(connection.getResponseCode() + " " + connection.getResponseMessage());
                return Util.INDETERMINATE;
            }
        } catch (Exception e) {
            logger.error("Exception in 'PolicyGuardXacmlHelper.callRESTfulPDP'", e);
            return Util.INDETERMINATE;
        }

        //
        // Read the response
        //
        try {
            ContentType contentType = ContentType.parse(connection.getContentType());

            if (contentType.getMimeType().equalsIgnoreCase(ContentType.APPLICATION_JSON.getMimeType())) {
                InputStream inputStream = connection.getInputStream();
                int contentLength = connection.getContentLength();

                return readResponseFromStream(inputStream, contentLength);
            } else {
                logger.error("unknown content-type: {}", contentType);
                return Util.INDETERMINATE;
            }

        } catch (Exception e) {
            String message = "Parsing Content-Type: " + connection.getContentType();
            logger.error(message, e);
            return Util.INDETERMINATE;
        }
    }

    /**
     * Parse XACML PDP response.
     * 
     * @param xacmlResponse the XACML response
     * @return the PolicyGuardResponse
     */
    public static PolicyGuardResponse parseXACMLPDPResponse(com.att.research.xacml.api.Response xacmlResponse) {
        if (xacmlResponse == null) {
            //
            // In case the actual XACML response was null, create an empty
            // response object with decision "Indeterminate"
            //
            return new PolicyGuardResponse("Indeterminate", null, "");
        }

        Iterator<Result> itRes = xacmlResponse.getResults().iterator();

        Result res = itRes.next();
        String decisionFromXacmlResponse = res.getDecision().toString();
        Iterator<AttributeCategory> itAttrCat = res.getAttributes().iterator();
        UUID reqIdFromXacmlResponse = null;
        String operationFromXacmlResponse = "";

        while (itAttrCat.hasNext()) {
            Iterator<Attribute> itAttr = itAttrCat.next().getAttributes().iterator();
            while (itAttr.hasNext()) {
                Attribute currentAttr = itAttr.next();
                String attributeId = currentAttr.getAttributeId().stringValue();
                if ("urn:oasis:names:tc:xacml:1.0:request:request-id".equals(attributeId)) {
                    Iterator<AttributeValue<?>> itValues = currentAttr.getValues().iterator();
                    reqIdFromXacmlResponse = UUID.fromString(itValues.next().getValue().toString());
                }
                if ("urn:oasis:names:tc:xacml:1.0:operation:operation-id".equals(attributeId)) {
                    Iterator<AttributeValue<?>> itValues = currentAttr.getValues().iterator();
                    operationFromXacmlResponse = itValues.next().getValue().toString();
                }
            }
        }

        return new PolicyGuardResponse(decisionFromXacmlResponse, reqIdFromXacmlResponse, operationFromXacmlResponse);

    }

    private void init(Properties properties) {
        // used to store error messages
        StringBuilder sb = new StringBuilder();

        // fetch these parameters, if they exist
        String timeoutString = properties.getProperty("pdpx.timeout");
        String disabledString = properties.getProperty("guard.disabled");

        if (disabledString != null && Boolean.parseBoolean(disabledString)) {
            return;
        }

        ArrayList<UrlEntry> entries = initEntries(properties, sb);

        if (entries.isEmpty()) {
            sb.append("'pdpx.*' -- no URLs specified, ");
        } else {
            restUrls = entries.toArray(new UrlEntry[0]);
        }

        if (timeoutString != null) {
            try {
                // decode optional 'pdpx.timeout' parameter
                timeout = Integer.valueOf(timeoutString);
            } catch (NumberFormatException e) {
                sb.append("'pdpx.timeout': " + e + ", ");
                logger.trace(e.getLocalizedMessage());
            }
        }


        // if there are any errors, update 'errorMessage' & disable guard
        // queries
        if (sb.length() != 0) {
            // remove the terminating ", ", and extract resulting error message
            sb.setLength(sb.length() - 2);
            String errorMessage = sb.toString();
            logger.error("Initialization failure: {}", errorMessage);
        }
    }

    private ArrayList<UrlEntry> initEntries(Properties properties, StringBuilder sb) {
        // now, see which numeric entries (1-9) exist
        ArrayList<UrlEntry> entries = new ArrayList<>();

        for (int index = 0; index < 10; index += 1) {
            String urlPrefix = "guard.";
            if (index != 0) {
                urlPrefix = urlPrefix + index + ".";
            }

            // see if the associated URL exists
            String restUrllist = properties.getProperty(urlPrefix + "url");
            if (nullOrEmpty(restUrllist)) {
                // no entry for this index
                continue;
            }

            // support a list of entries separated by semicolons. Each entry
            // can be:
            // URL
            // URL,user
            // URL,user,password
            for (String restUrl : restUrllist.split("\\s*;\\s*")) {
                UrlEntry entry = initRestUrl(properties, sb, restUrl);
                // include this URLEntry in the list
                if (entry != null) {
                    entries.add(entry);
                }
            }
        }

        return entries;
    }

    private UrlEntry initRestUrl(Properties properties, StringBuilder sb, String restUrl) {
        String urlPrefix = "guard.";
        String pdpxPrefix = "pdpx.";

        String[] segments = restUrl.split("\\s*,\\s*");
        String user = null;
        String password = null;

        if (segments.length >= 2) {
            // user id is provided
            restUrl = segments[0];
            user = segments[1];
            if (segments.length >= 3) {
                // password is also provided
                password = segments[2];
            }
        }

        // URL does exist -- create the entry
        UrlEntry urlEntry = new UrlEntry();
        try {
            urlEntry.restUrl = new URL(restUrl);
        } catch (java.net.MalformedURLException e) {
            // if we don't have a URL,
            // don't bother with the rest on this one
            sb.append("'").append(urlPrefix).append("url' '").append(restUrl).append("': ").append(e).append(",");
            return null;
        }

        if (nullOrEmpty(user)) {
            // user id was not provided on '*.url' line --
            // extract it from a separate property
            user = properties.getProperty(pdpxPrefix + "username", properties.getProperty("pdpx.username"));
        }
        if (nullOrEmpty(password)) {
            // password was not provided on '*.url' line --
            // extract it from a separate property
            password = properties.getProperty(pdpxPrefix + "password", properties.getProperty("pdpx.password"));
        }

        // see if 'user' and 'password' entries both exist
        if (!nullOrEmpty(user) && !nullOrEmpty(password)) {
            urlEntry.authorization = "Basic " + Base64.getEncoder().encodeToString((user + ":" + password).getBytes());
        }

        // see if 'client.user' and 'client.password' entries both exist
        String clientUser =
                properties.getProperty(pdpxPrefix + "client.username", properties.getProperty("pdpx.client.username"));
        String clientPassword =
                properties.getProperty(pdpxPrefix + "client.password", properties.getProperty("pdpx.client.password"));
        if (!nullOrEmpty(clientUser) && !nullOrEmpty(clientPassword)) {
            urlEntry.clientAuth =
                    "Basic " + Base64.getEncoder().encodeToString((clientUser + ":" + clientPassword).getBytes());
        }

        // see if there is an 'environment' entry
        String environment =
                properties.getProperty(pdpxPrefix + "environment", properties.getProperty("pdpx.environment"));
        if (!nullOrEmpty(environment)) {
            urlEntry.environment = environment;
        }

        return urlEntry;
    }

    /**
     * Check if a string is null or an empty string.
     *
     * @param value the string to be tested
     * @return 'true' if the string is 'null' or has a length of 0, 'false' otherwise
     */
    private static boolean nullOrEmpty(String value) {
        return (value == null || value.isEmpty());
    }

    private static String readResponseFromStream(InputStream inputStream, int contentLength) throws IOException {
        // if content length is -1, response is chunked, and
        // TCP connection will be dropped at the end
        byte[] buf = new byte[contentLength < 0 ? 1024 : contentLength];
        int offset = 0;
        do {
            int size = inputStream.read(buf, offset, buf.length - offset);
            if (size < 0) {
                // In a chunked response a dropped connection is expected, but not if the response
                // is not chunked
                if (contentLength > 0) {
                    logger.error("partial input stream");
                }
                break;
            }
            offset += size;
        }
        while (offset != contentLength);

        String response = new String(buf, 0, offset);

        //
        // Connection may have failed or not been 200 OK, return Indeterminate
        //
        if (response.isEmpty()) {
            return Util.INDETERMINATE;
        }

        return new JSONObject(response).getString("decision");

    }
}
