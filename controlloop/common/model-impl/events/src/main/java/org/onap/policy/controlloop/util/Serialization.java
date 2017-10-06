/*-
 * ============LICENSE_START=======================================================
 * controlloop
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

package org.onap.policy.controlloop.util;

import java.lang.reflect.Type;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.onap.policy.controlloop.ControlLoopNotificationType;
import org.onap.policy.controlloop.ControlLoopTargetType;
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

	public static class notificationTypeAdapter implements JsonSerializer<ControlLoopNotificationType>, JsonDeserializer<ControlLoopNotificationType> {


		@Override
		public JsonElement serialize(ControlLoopNotificationType src, Type typeOfSrc,
				JsonSerializationContext context) {
			return new JsonPrimitive(src.toString());
		}

		@Override
		public ControlLoopNotificationType deserialize(JsonElement json, Type typeOfT,
				JsonDeserializationContext context) throws JsonParseException {
			return ControlLoopNotificationType.toType(json.getAsString());
		}
		
	}
	
	public static class targetTypeAdapter implements JsonSerializer<ControlLoopTargetType>, JsonDeserializer<ControlLoopTargetType> {

		@Override
		public JsonElement serialize(ControlLoopTargetType src, Type typeOfSrc,
				JsonSerializationContext context) {
			return new JsonPrimitive(src.toString());
		}

		@Override
		public ControlLoopTargetType deserialize(JsonElement json, Type typeOfT,
				JsonDeserializationContext context) throws JsonParseException {
			return ControlLoopTargetType.toType(json.getAsString());
		}
		
	}
	
	public static class gsonUTCAdapter implements JsonSerializer<ZonedDateTime>, JsonDeserializer<ZonedDateTime> {
		private static final Logger logger = LoggerFactory.getLogger(gsonUTCAdapter.class);
		public static final DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSSxxx");

                @Override
		public ZonedDateTime deserialize(JsonElement element, Type type, JsonDeserializationContext context)
				throws JsonParseException {
			try {
				return ZonedDateTime.parse(element.getAsString(), format);
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
			return null;
		}

                @Override
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
	
	final static public Gson gson = new GsonBuilder().disableHtmlEscaping()
			.registerTypeAdapter(ZonedDateTime.class, new gsonUTCAdapter())
			.registerTypeAdapter(Instant.class, new gsonInstantAdapter())
			.registerTypeAdapter(ControlLoopNotificationType.class, new notificationTypeAdapter())
			.registerTypeAdapter(ControlLoopTargetType.class, new targetTypeAdapter())
			.create();

	
	final static public Gson gsonPretty = new GsonBuilder().disableHtmlEscaping()
			.setPrettyPrinting()
			.registerTypeAdapter(ZonedDateTime.class, new gsonUTCAdapter())
			.registerTypeAdapter(Instant.class, new gsonInstantAdapter())
			.registerTypeAdapter(ControlLoopNotificationType.class, new notificationTypeAdapter())
			.registerTypeAdapter(ControlLoopTargetType.class, new targetTypeAdapter())
			.create();
	
	final static public Gson gsonJunit = new GsonBuilder().disableHtmlEscaping()
			.setPrettyPrinting()
			.registerTypeAdapter(ZonedDateTime.class, new gsonUTCAdapter())
			.registerTypeAdapter(Instant.class, new gsonInstantAdapter())
			.registerTypeAdapter(ControlLoopTargetType.class, new targetTypeAdapter())
			.create();

}
