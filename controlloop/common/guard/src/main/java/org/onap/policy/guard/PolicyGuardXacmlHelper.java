/*-
 * ============LICENSE_START=======================================================
 * guard
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

package org.onap.policy.guard;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
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

import com.att.research.xacml.api.Attribute;
import com.att.research.xacml.api.AttributeCategory;
import com.att.research.xacml.api.AttributeValue;
import com.att.research.xacml.api.Result;


public class PolicyGuardXacmlHelper {

	private static final Logger logger = LoggerFactory
			.getLogger(PolicyGuardXacmlHelper.class);
	
	private static final Logger netLogger = LoggerFactory.getLogger(org.onap.policy.drools.event.comm.Topic.NETWORK_LOGGER);

	public PolicyGuardXacmlHelper() {
		init(PolicyEngine.manager.getEnvironment());
	}

	// initialized from 'pdpx.url' property --
	// Each entry in 'restUrls' contains a destination URL, and an optional
	// 'Authorization' header entry. 'restUrlIndex' indicates the next
	// entry to try -- after each failure, the index is advanced to the
	// next entry (wrapping to the beginning, if needed).
	static private class URLEntry implements Serializable {
		URL restURL;
		String authorization = null;
		String clientAuth = null;
		String environment = null;
	};

	private URLEntry[] restUrls = null;
	private int restUrlIndex = 0;

	// REST timeout, initialized from 'pdpx.timeout' property
	private int timeout = 20000;


	// initialized from 'guard.disabled', but may also be set to 'true' if
	// there is an initialization error
	private boolean disabled = false;

	// errors that forced 'disabled' to be set to 'true'
	private String errorMessage = null;

	public String callPDP(PolicyGuardXacmlRequestAttributes xacmlReq) {
		//
		// Send it to the PDP
		//
		String response = null;

		//
		// Build the json request
		//
		JSONObject attributes = new JSONObject();
		attributes.put("actor", xacmlReq.getActor_id());
		attributes.put("recipe", xacmlReq.getOperation_id());
		attributes.put("target", xacmlReq.getTarget_id());
		if (xacmlReq.getClname_id() != null) {
			attributes.put("clname", xacmlReq.getClname_id());
		}
		JSONObject jsonReq = new JSONObject();
		jsonReq.put("decisionAttributes", attributes);
		jsonReq.put("onapName", "PDPD");

		URLEntry urlEntry = restUrls[restUrlIndex];

		try {
			//
			// Call RESTful PDP
			//
			netLogger.info("[OUT|{}|{}|]{}{}", "GUARD", urlEntry.restURL, System.lineSeparator(), jsonReq.toString());
			response = callRESTfulPDP(new ByteArrayInputStream(jsonReq
					.toString().getBytes()), urlEntry.restURL,
					urlEntry.authorization, urlEntry.clientAuth,
					urlEntry.environment);
			netLogger.info("[IN|{}|{}|]{}{}", "GUARD", urlEntry.restURL, System.lineSeparator(), response);
		} catch (Exception e) {
			logger.error("Error in sending RESTful request: ", e);
		}

		return response;
	}

	/**
	 * This makes an HTTP POST call to a running PDP RESTful servlet to get a
	 * decision.
	 * 
	 * @param file
	 * @return response from guard which contains "Permit" or "Deny"
	 */
	private String callRESTfulPDP(InputStream is, URL restURL,
			String authorization, String clientauth, String environment) {
		String response = null;
		String rawDecision = null;
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
			if (connection.getResponseCode() == 200) {
				//
				// Read the response
				//
				ContentType contentType = null;
				try {
					contentType = ContentType
							.parse(connection.getContentType());

					if (contentType.getMimeType().equalsIgnoreCase(
							ContentType.APPLICATION_JSON.getMimeType())) {
						InputStream iStream = connection.getInputStream();
						int contentLength = connection.getContentLength();

						// if content length is -1, respose is chunked, and
						// TCP connection will be dropped at the end
						byte[] buf = new byte[contentLength < 0 ? 1024
								: contentLength];
						int offset = 0;
						for (;;) {
							if (offset == contentLength) {
								// all expected bytes have been read
								response = new String(buf);
								break;
							}
							int size = iStream.read(buf, offset, buf.length
									- offset);
							if (size < 0) {
								if (contentLength > 0) {
									logger.error("partial input stream");
								} else {
									// chunked response --
									// dropped connection is expected
									response = new String(buf, 0, offset);
								}
								break;
							}
							offset += size;
						}
					} else {
						logger.error("unknown content-type: " + contentType);
					}

				} catch (Exception e) {
					String message = "Parsing Content-Type: "
							+ connection.getContentType();
					logger.error(message, e);
				}

			} else {
				logger.error(connection.getResponseCode() + " "
						+ connection.getResponseMessage());
			}
		} catch (Exception e) {
			logger.error(
					"Exception in 'PolicyGuardXacmlHelper.callRESTfulPDP'", e);
		}
		
		//
		// Connection may have failed or not been 200 OK, return Indeterminate
		//
		if(response == null || response.isEmpty()){
			return Util.INDETERMINATE;
		}
		
		rawDecision = new JSONObject(response).getString("decision");

		return rawDecision;
	}

	public static PolicyGuardResponse ParseXacmlPdpResponse(
			com.att.research.xacml.api.Response xacmlResponse) {

		if (xacmlResponse == null) {

			//
			// In case the actual XACML response was null, create an empty
			// response object with decision "Indeterminate"
			//
			return new PolicyGuardResponse("Indeterminate", null, "");
		}

		Iterator<Result> it_res = xacmlResponse.getResults().iterator();

		Result res = it_res.next();
		String decision_from_xacml_response = res.getDecision().toString();
		Iterator<AttributeCategory> it_attr_cat = res.getAttributes()
				.iterator();
		UUID req_id_from_xacml_response = null;
		String operation_from_xacml_response = "";

		while (it_attr_cat.hasNext()) {
			Iterator<Attribute> it_attr = it_attr_cat.next().getAttributes()
					.iterator();
			while (it_attr.hasNext()) {
				Attribute current_attr = it_attr.next();
				String s = current_attr.getAttributeId().stringValue();
				if ("urn:oasis:names:tc:xacml:1.0:request:request-id".equals(s)) {
					Iterator<AttributeValue<?>> it_values = current_attr
							.getValues().iterator();
					req_id_from_xacml_response = UUID.fromString(it_values
							.next().getValue().toString());
				}
				if ("urn:oasis:names:tc:xacml:1.0:operation:operation-id"
						.equals(s)) {
					Iterator<AttributeValue<?>> it_values = current_attr
							.getValues().iterator();
					operation_from_xacml_response = it_values.next().getValue()
							.toString();
				}

			}
		}

		return new PolicyGuardResponse(decision_from_xacml_response,
				req_id_from_xacml_response, operation_from_xacml_response);

	}

	private void init(Properties properties) {
		// used to store error messages
		StringBuilder sb = new StringBuilder();

		// fetch these parameters, if they exist
		String timeoutString = properties.getProperty("pdpx.timeout");
		String disabledString = properties.getProperty("guard.disabled");

		if (disabledString != null) {
			// decode optional 'guard.disabled' parameter
			disabled = new Boolean(disabledString);
			if (disabled) {
				// skip everything else
				return;
			}
		}

		/*
		 * Decode 'pdpx.*' parameters
		 */

		// first, the default parameters
		String defaultUser = properties.getProperty("pdpx.username");
		String defaultPassword = properties
				.getProperty("pdpx.password");
		String defaultClientUser = properties
				.getProperty("pdpx.client.username");
		String defaultClientPassword = properties
				.getProperty("pdpx.client.password");
		String defaultEnvironment = properties
				.getProperty("pdpx.environment");

		// now, see which numeric entries (1-9) exist
		ArrayList<URLEntry> entries = new ArrayList<>();

		for (int index = 0; index < 10; index += 1) {
			String urlPrefix = "guard.";
			String pdpxPrefix = "pdpx.";
			if (index != 0) {
				urlPrefix = urlPrefix + index + ".";
			}

			// see if the associated URL exists
			String restURLlist = properties.getProperty(urlPrefix + "url");
			if (nullOrEmpty(restURLlist)) {
				// no entry for this index
				continue;
			}

			// support a list of entries separated by semicolons. Each entry
			// can be:
			// URL
			// URL,user
			// URL,user,password
			for (String restURL : restURLlist.split("\\s*;\\s*")) {
				String[] segments = restURL.split("\\s*,\\s*");
				String user = null;
				String password = null;

				if (segments.length >= 2) {
					// user id is provided
					restURL = segments[0];
					user = segments[1];
					if (segments.length >= 3) {
						// password is also provided
						password = segments[2];
					}
				}

				// URL does exist -- create the entry
				URLEntry urlEntry = new URLEntry();
				try {
					urlEntry.restURL = new URL(restURL);
				} catch (java.net.MalformedURLException e) {
					// if we don't have a URL,
					// don't bother with the rest on this one
					sb.append("'").append(urlPrefix).append("url' '")
							.append(restURL).append("': ").append(e)
							.append(",");
					continue;
				}

				if (nullOrEmpty(user)) {
					// user id was not provided on '*.url' line --
					// extract it from a separate property
					user = properties.getProperty(pdpxPrefix + "username", defaultUser);
				}
				if (nullOrEmpty(password)) {
					// password was not provided on '*.url' line --
					// extract it from a separate property
					password = properties.getProperty(pdpxPrefix + "password",
							defaultPassword);
				}

				// see if 'user' and 'password' entries both exist
				if (!nullOrEmpty(user) && !nullOrEmpty(password)) {
					urlEntry.authorization = "Basic "
							+ Base64.getEncoder().encodeToString(
									(user + ":" + password).getBytes());
				}

				// see if 'client.user' and 'client.password' entries both exist
				String clientUser = properties.getProperty(pdpxPrefix
						+ "client.username", defaultClientUser);
				String clientPassword = properties.getProperty(pdpxPrefix
						+ "client.password", defaultClientPassword);
				if (!nullOrEmpty(clientUser) && !nullOrEmpty(clientPassword)) {
					urlEntry.clientAuth = "Basic "
							+ Base64.getEncoder().encodeToString(
									(clientUser + ":" + clientPassword)
											.getBytes());
				}

				// see if there is an 'environment' entry
				String environment = properties.getProperty(pdpxPrefix
						+ "environment", defaultEnvironment);
				if (!nullOrEmpty(environment)) {
					urlEntry.environment = environment;
				}

				// include this URLEntry in the list
				entries.add(urlEntry);
			}
		}

		if (entries.size() == 0) {
			sb.append("'pdpx.*' -- no URLs specified, ");
		} else {
			restUrls = entries.toArray(new URLEntry[0]);
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
			errorMessage = sb.toString();
			disabled = true;
			logger.error("Initialization failure: " + errorMessage);
		}
	}

	/**
	 * Check if a string is null or an empty string
	 *
	 * @param value
	 *            the string to be tested
	 * @return 'true' if the string is 'null' or has a length of 0, 'false'
	 *         otherwise
	 */
	static private boolean nullOrEmpty(String value) {
		return (value == null || value.isEmpty());
	}

}
