/*-
 * ============LICENSE_START=======================================================
 * appc
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

package org.onap.policy.appclcm.util;

import java.lang.reflect.Type;
import java.time.Instant;

import org.onap.policy.appclcm.LCMRequest;
import org.onap.policy.appclcm.LCMResponse;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public final class Serialization {

    public static class RequestAdapter implements JsonSerializer<LCMRequest>, JsonDeserializer<LCMRequest> {

        @Override
        public JsonElement serialize(LCMRequest src, Type typeOfSrc, JsonSerializationContext context) {            
            JsonElement requestJson = gsonPretty.toJsonTree(src, LCMRequest.class);
            JsonObject input = new JsonObject();
            input.add("input", requestJson);
            
            return input;
        }
        
        @Override
        public LCMRequest deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
            LCMRequest request = gsonPretty.fromJson(json.getAsJsonObject().get("input"), LCMRequest.class);
            return request;
        }
    }
    
    public static class ResponseAdapter implements JsonSerializer<LCMResponse>, JsonDeserializer<LCMResponse> {

        @Override
        public JsonElement serialize(LCMResponse src, Type typeOfSrc, JsonSerializationContext context) {
            JsonElement responseJson = gsonPretty.toJsonTree(src, LCMResponse.class);
            JsonObject output = new JsonObject();
            output.add("output", responseJson);
            return output;
        }
        
        @Override
        public LCMResponse deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
            LCMResponse response = gsonPretty.fromJson(json.getAsJsonObject().get("output"), LCMResponse.class);
            return response;
        }
    }

    public static class InstantAdapter implements JsonSerializer<Instant>, JsonDeserializer<Instant> {

        @Override
        public Instant deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
        	return Instant.parse(json.getAsString());
        }

        @Override
        public JsonElement serialize(Instant src, Type typeOfSrc, JsonSerializationContext context) {
        	return new JsonPrimitive(src.toString());
        }

    }
    
    public static class InstantJunitAdapter implements JsonSerializer<Instant>, JsonDeserializer<Instant> {

        @Override
        public Instant deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
        	return Instant.ofEpochMilli(json.getAsLong());
        }

        @Override
        public JsonElement serialize(Instant src, Type typeOfSrc, JsonSerializationContext context) {
        	return new JsonPrimitive(src.toEpochMilli());
        }

    }

    public static final Gson gsonPretty = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting()
            .registerTypeAdapter(Instant.class, new InstantAdapter()).create();
    
    public static final Gson gson = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting()
            .registerTypeAdapter(LCMRequest.class, new RequestAdapter())
            .registerTypeAdapter(LCMResponse.class, new ResponseAdapter()).create();
    
    public static final Gson gsonJunit = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting()
    		.registerTypeAdapter(Instant.class, new InstantJunitAdapter()).create();

}
