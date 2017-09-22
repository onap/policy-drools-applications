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

package org.onap.policy.appc.util;

import java.lang.reflect.Type;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public final class Serialization {
	
	public static final DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSSxxx");

	private Serialization(){
	}

	public static class gsonUTCAdapter implements JsonSerializer<ZonedDateTime>, JsonDeserializer<ZonedDateTime> {
		private static final Logger logger = LoggerFactory.getLogger(gsonUTCAdapter.class);
		
		@Override
		public ZonedDateTime deserialize(JsonElement element, Type type, JsonDeserializationContext context)
				throws JsonParseException {
			try {
				return ZonedDateTime.parse(element.getAsString(), format);
			} catch (Exception e) {
				logger.error("deserialize threw: ", e);
			}
			return null;
		}

		public JsonElement serialize(ZonedDateTime datetime, Type type, JsonSerializationContext context) {
			return new JsonPrimitive(datetime.format(format));
		}	
	}
	public static class gsonInstantAdapter implements JsonSerializer<Instant>, JsonDeserializer<Instant> {

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

	public static final Gson gsonPretty = new GsonBuilder()
			.disableHtmlEscaping()
			.setPrettyPrinting()
			.registerTypeAdapter(ZonedDateTime.class, new gsonUTCAdapter())
			.registerTypeAdapter(Instant.class, new gsonInstantAdapter())
//			.registerTypeAdapter(CommonHeader1607.class, new gsonCommonHeaderInstance())
//			.registerTypeAdapter(ResponseStatus1607.class, new gsonResponseStatus())
			.create();
	
}
