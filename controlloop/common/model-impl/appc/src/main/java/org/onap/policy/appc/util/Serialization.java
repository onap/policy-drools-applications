/*-
 * ============LICENSE_START=======================================================
 * appc
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

package org.onap.policy.appc.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Serialization {
    public static final DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSSxxx");

    public static final Gson gsonPretty = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting()
            .registerTypeAdapter(ZonedDateTime.class, new GsonUtcAdapter())
            .registerTypeAdapter(Instant.class, new GsonInstantAdapter())
            // .registerTypeAdapter(CommonHeader1607.class, new gsonCommonHeaderInstance())
            // .registerTypeAdapter(ResponseStatus1607.class, new gsonResponseStatus())
            .create();

    private Serialization() {}

    public static class GsonUtcAdapter implements JsonSerializer<ZonedDateTime>, JsonDeserializer<ZonedDateTime> {
        private static final Logger logger = LoggerFactory.getLogger(GsonUtcAdapter.class);

        @Override
        public ZonedDateTime deserialize(JsonElement element, Type type, JsonDeserializationContext context) {
            try {
                return ZonedDateTime.parse(element.getAsString(), format);
            } catch (Exception e) {
                logger.error("deserialize threw: ", e);
            }
            return null;
        }

        @Override
        public JsonElement serialize(ZonedDateTime datetime, Type type, JsonSerializationContext context) {
            return new JsonPrimitive(datetime.format(format));
        }
    }

    public static class GsonInstantAdapter implements JsonSerializer<Instant>, JsonDeserializer<Instant> {

        @Override
        public Instant deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
            return Instant.ofEpochMilli(json.getAsLong());
        }

        @Override
        public JsonElement serialize(Instant src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.toEpochMilli());
        }

    }

}
