/*-
 * ============LICENSE_START=======================================================
 * rest
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

package org.onap.policy.rest;

import java.io.IOException;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RESTManager {

	private static final Logger logger = LoggerFactory.getLogger(RESTManager.class);
	
	public static class Pair<A, B> {
		public final A a;
		public final B b;
		
		public Pair(A a, B b) {
			this.a = a;
			this.b = b;
		}
	}

	public static Pair<Integer, String> post(String url, String username, String password, Map<String, String> headers, String contentType, String body) {
		CredentialsProvider credentials = new BasicCredentialsProvider();
		credentials.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));
		
		logger.debug("HTTP REQUEST: {} -> {} {} -> {}", url, username, ((password!=null)?password.length():"-"), contentType);
		if (headers != null) {
			logger.debug("Headers: ");
			headers.forEach((name, value) -> {
			    logger.debug("{} -> {}", name, value);
			});
		}
		logger.debug(body);
		
		try (CloseableHttpClient client = HttpClientBuilder.create().setDefaultCredentialsProvider(credentials).build()) {

			HttpPost post = new HttpPost(url);
			if (headers != null)  {
				for (String key : headers.keySet()) {
					post.addHeader(key, headers.get(key));
				}
			}
			post.addHeader("Content-Type", contentType);
			
			StringEntity input = new StringEntity(body);
			input.setContentType(contentType);
			post.setEntity(input);
			
			HttpResponse response = client.execute(post);
			
			String returnBody = EntityUtils.toString(response.getEntity(), "UTF-8");
			logger.debug("HTTP POST Response Status Code: {}", response.getStatusLine().getStatusCode());
			logger.debug("HTTP POST Response Body:");
			logger.debug(returnBody);

			return new Pair<Integer, String>(response.getStatusLine().getStatusCode(), returnBody);
		} catch (IOException e) {
			logger.error("Failed to POST to {}",url,e);

			return null;
		}
		
	}

	public static Pair<Integer, String> get(String url, String username, String password, Map<String, String> headers) {
		
		CredentialsProvider credentials = new BasicCredentialsProvider();
		credentials.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));
		
		try (CloseableHttpClient client = HttpClientBuilder.create().setDefaultCredentialsProvider(credentials).build()) {

			HttpGet get = new HttpGet(url);
			if (headers != null) {
				for (String key : headers.keySet()) {
					get.addHeader(key, headers.get(key));
				}
			}
			
			HttpResponse response = client.execute(get);
			
			String returnBody = EntityUtils.toString(response.getEntity(), "UTF-8");

			logger.debug("HTTP GET Response Status Code: {}", response.getStatusLine().getStatusCode());
			logger.debug("HTTP GET Response Body:");
			logger.debug(returnBody);

			return new Pair<Integer, String>(response.getStatusLine().getStatusCode(), returnBody);
		} catch (IOException e) {
			logger.error("Failed to GET to {}",url,e);
			return null;
		}
	}
}
