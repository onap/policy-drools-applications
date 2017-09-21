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
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.apache.http.entity.ContentType;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.att.research.xacml.api.Attribute;
import com.att.research.xacml.api.AttributeCategory;
import com.att.research.xacml.api.AttributeValue;
import com.att.research.xacml.api.Result;


public class PolicyGuardXacmlHelper {
	
	private static final Logger logger = LoggerFactory.getLogger(PolicyGuardXacmlHelper.class);

	public static String callPDP(String restfulPdpUrl, PolicyGuardXacmlRequestAttributes xacmlReq) {
		//
		// Send it to the PDP
		//
//		com.att.research.xacml.api.Response response = null;
		String response = null;
		
		JSONObject attributes = new JSONObject();
		attributes.put("actor", xacmlReq.getActor_id());
		attributes.put("recipe", xacmlReq.getOperation_id());
		attributes.put("target", xacmlReq.getTarget_id());
		if (xacmlReq.getClname_id() != null){
			attributes.put("clname", xacmlReq.getClname_id());
		}
		JSONObject jsonReq = new JSONObject();
		jsonReq.put("decisionAttributes", attributes);
		jsonReq.put("onapName", "PDPD");
		
		try {
			//
			// Call RESTful PDP
			//
			response = callRESTfulPDP(new ByteArrayInputStream(jsonReq.toString().getBytes()), new URL(restfulPdpUrl/*"https://localhost:8443/pdp/"*/));
		} catch (Exception e) {
			logger.error("Error in sending RESTful request: ", e);
		}
		
		
		return response;
	}
	
	
	/**
	 * This makes an HTTP POST call to a running PDP RESTful servlet to get a decision.
	 * 
	 * @param file
	 * @return response from guard which contains "Permit" or "Deny"
	 */
	private static String callRESTfulPDP(InputStream is, URL restURL) {
//		com.att.research.xacml.api.Response response = null;
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
        			contentType = ContentType.parse(connection.getContentType());
        			
        			if (contentType.getMimeType().equalsIgnoreCase(ContentType.APPLICATION_JSON.getMimeType())) {
					  InputStream iStream = connection.getInputStream();
					  int contentLength = connection.getContentLength();

					  // if content length is -1, respose is chunked, and
					  // TCP connection will be dropped at the end
					  byte[] buf =
						new byte[contentLength < 0 ? 1024 : contentLength];
					  int offset = 0;
					  for ( ; ; )
						{
						  if (offset == contentLength)
							{
							  // all expected bytes have been read
							  response = new String(buf);
							  break;
							}
						  int size = iStream.read(buf, offset,
											 buf.length - offset);
						  if (size < 0)
							{
							  if (contentLength > 0)
								{
								  logger.error("partial input stream");
								}
							  else
								{
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
					String message = "Parsing Content-Type: " + connection.getContentType();
					logger.error(message, e);
        		}

            } else {
            	logger.error(connection.getResponseCode() + " " + connection.getResponseMessage());
            }
		} catch (Exception e) {
			logger.error("Exception in 'PolicyGuardXacmlHelper.callRESTfulPDP'", e);
		}
		
		rawDecision = new JSONObject(response).getString("decision");
		
		return rawDecision;
	}
	
	
	public static PolicyGuardResponse ParseXacmlPdpResponse(com.att.research.xacml.api.Response xacmlResponse){
		
		if(xacmlResponse == null){
			
			//
			//In case the actual XACML response was null, create an empty response object with decision "Indeterminate"
			//
			return new PolicyGuardResponse("Indeterminate", null, "");
		}
		
		Iterator<Result> it_res = xacmlResponse.getResults().iterator();
		
		Result res	= it_res.next();
		String decision_from_xacml_response = res.getDecision().toString();
		Iterator<AttributeCategory> it_attr_cat = res.getAttributes().iterator();
		UUID req_id_from_xacml_response = null;
		String operation_from_xacml_response = "";
		
		while(it_attr_cat.hasNext()){
			Iterator<Attribute>  it_attr = it_attr_cat.next().getAttributes().iterator();
			while(it_attr.hasNext()){
				Attribute current_attr = it_attr.next();
				String s = current_attr.getAttributeId().stringValue();
				if(s.equals("urn:oasis:names:tc:xacml:1.0:request:request-id")){
					Iterator<AttributeValue<?>> it_values = current_attr.getValues().iterator();
					req_id_from_xacml_response = UUID.fromString(it_values.next().getValue().toString());
				}
				if(s.equals("urn:oasis:names:tc:xacml:1.0:operation:operation-id")){
					Iterator<AttributeValue<?>> it_values = current_attr.getValues().iterator();
					operation_from_xacml_response = it_values.next().getValue().toString();
				}
				
			}
		}
		
		
		return new PolicyGuardResponse(decision_from_xacml_response, req_id_from_xacml_response, operation_from_xacml_response);
		
	}
	
	
	
}
