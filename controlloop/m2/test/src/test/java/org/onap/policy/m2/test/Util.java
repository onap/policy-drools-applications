/*-
 * ============LICENSE_START=======================================================
 * m2/test
 * ================================================================================
 * Copyright (C) 2020 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2020 Bell Canada.
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

package org.onap.policy.m2.test;

import static org.junit.Assert.assertTrue;

import com.att.nsa.cambria.client.CambriaBatchingPublisher;
import com.att.nsa.cambria.client.CambriaClientBuilders;
import com.att.nsa.cambria.client.CambriaClientBuilders.ConsumerBuilder;
import com.att.nsa.cambria.client.CambriaClientBuilders.PublisherBuilder;
import com.att.nsa.cambria.client.CambriaConsumer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.drools.system.PolicyEngineConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Util {
    private static Logger logger = LoggerFactory.getLogger(Util.class);

    // used for JSON <-> String conversion
    private static StandardCoder coder = new StandardCoder();

    // used for pretty-printing: gson.toJson(JsonObject obj)
    private static Gson gson =
        new GsonBuilder().setPrettyPrinting().serializeNulls().create();

    // contains the currently running set of servers
    private static List<Server> runningServers = new LinkedList<>();

    /**
     * Read from an 'InputStream' until EOF or until it is closed.  This method
     * may block, depending on the type of 'InputStream'.
     *
     * @param input This is the input stream
     * @return A 'String' containing the contents of the input stream
     */
    public static String inputStreamToString(InputStream input) {
        StringBuilder sb = new StringBuilder();
        byte[] buffer = new byte[8192];
        int length;

        try {
            while ((length = input.read(buffer)) > 0) {
                sb.append(new String(buffer, 0, length));
            }
        } catch (IOException e) {
            // return what we have so far
        }
        return sb.toString();
    }

    /**
     * Read in a file, converting the contents to a string.
     *
     * @param file the input file
     * @return a String containing the contents of the file
     */
    public static String fileToString(File file)
        throws IOException, FileNotFoundException {
        try (FileInputStream fis = new FileInputStream(file)) {
            String string = inputStreamToString(fis);
            return string;
        }
    }

    /**
     * Create a file containing the contents of the specified string.
     *
     * @param string the input string
     * @param suffix the suffix to pass to 'createTempFile
     * @return a File, whose contents contain the string
     */
    public static File stringToFile(String string, String suffix)
        throws IOException {
        File file = File.createTempFile("templates-util", suffix);
        file.deleteOnExit();

        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(string.getBytes());
        }
        return file;
    }

    /**
     * Create a file containing the contents of the specified string.
     *
     * @param string the input string
     * @return a File, whose contents contain the string
     */
    public static File stringToFile(String string)
        throws IOException {
        return stringToFile(string, "");
    }

    /**
     * This method converts a YAML string into one that can be embedded into
     * a '.drl' file.
     *
     * @param yaml the input string, which is typically read from a file
     * @return the converted string
     */
    public static String convertYaml(String yaml) {
        yaml = yaml.replace("\n", "%0A");
        yaml = yaml.replace("\r", "");
        yaml = yaml.replace(":", "%3A");
        yaml = yaml.replace(' ', '+');
        return yaml;
    }

    /**
     * This is a convenience method which reads a file into a string, and
     * then does a set of string replacements on it. The real purpose is to
     * make it easy to do '${parameter}' replacements in files, as part of
     * building a Drools artifact.
     *
     * @param fileName this is the input file name
     * @param args these parameters come in pairs:
     *     'input-string' and 'output-string'.
     * @return a String containing the contents of the file, with the parameters
     *     replaced
     */
    public static String openAndReplace(String fileName, String... args)
        throws IOException, FileNotFoundException {
        String text = fileToString(new File(fileName));
        for (int i = 0; i < args.length; i += 2) {
            text = text.replace(args[i], args[i + 1]);
        }
        return text;
    }

    /**
     * Convert an Object to a JsonElement.
     *
     * @param object the object to convert
     * @return a JsonElement that corresponds to 'object'
     */
    public static JsonElement toJsonElement(Object object) {
        if (object == null || object instanceof JsonElement) {
            return (JsonElement) object;
        }
        if (object instanceof Number) {
            return new JsonPrimitive((Number) object);
        }
        if (object instanceof Boolean) {
            return new JsonPrimitive((Boolean) object);
        }
        if (object instanceof Character) {
            return new JsonPrimitive((Character) object);
        }
        return new JsonPrimitive(object.toString());
    }

    /**
     * This is a convenience method to build a 'JsonObject', and populate
     * it with a set of keyword/value pairs.
     *
     * @param data this parameter comes in pairs: 'keyword', and 'value'
     * @return the populated JsonObject
     */
    public static JsonObject json(Object... data) {
        JsonObject obj = new JsonObject();
        for (int i = 0; i < data.length; i += 2) {
            obj.add(data[i].toString(), toJsonElement(data[i + 1]));
        }
        return obj;
    }

    /**
     * Convert a JsonElement to a String (pretty-printing).
     *
     * @param jsonElement the object to convert
     * @return a pretty-printed string
     */
    public static String prettyPrint(JsonElement jsonElement) {
        return gson.toJson(jsonElement);
    }

    /**
     * This method is used to check whether a JSON message has a set of fields
     * populated with the values expected.
     *
     * @param subset this is a 'JsonObject', which contains field names and
     *     values (the values are interpreted as regular expressions). The values
     *     may also be 'JsonObject' instances, in which case they are compared
     *     recursively.
     * @param whole ordinarily, this will be a 'JsonObject', and will contain
     *     a superset of the fields in 'subset'. If not, the 'assert' fails.
     */
    public static void assertSubset(JsonObject subset, Object whole) {
        StringBuilder sb = new StringBuilder();
        assertSubsetAssist(sb, "", subset, toJsonElement(whole));
        String sbString = sb.toString();
        assertTrue(sbString, sbString.isEmpty());
    }

    /**
     * This is similar to 'assertSubset', but just returns 'true' if the
     * pattern matches.
     *
     * @param subset this is a 'JsonObject', which contains field names and
     *     values (the values are interpreted as regular expressions). The values
     *     may also be 'JsonObject' instances, in which case they are compared
     *     recursively.
     * @param whole ordinarily, this will be a 'JsonObject', and will contain
     *     a superset of the fields in 'subset'. If not, the 'assert' fails.
     * @return 'true' if 'whole' is a superset of 'subset'
     */
    public static boolean testSubset(JsonObject subset, Object whole) {
        StringBuilder sb = new StringBuilder();
        assertSubsetAssist(sb, "", subset, toJsonElement(whole));
        return sb.length() == 0;
    }

    /**
     * This is an internal support method for 'assertSubset' and 'testSubset',
     * and handles the recursive comparison.
     *
     * @param sb a 'StringBuilder', which is appended to when there are
     *     mismatches
     * @param prefix the field name being compared (the empty string indicates
     *     the top-level field).
     * @param subset the 'JsonObject' being compared at this level
     * @param argWhole the value being tested -- if it is not a 'JsonObject',
     *     the comparison fails
     */
    private static void assertSubsetAssist(StringBuilder sb, String prefix, JsonObject subset, JsonElement argWhole) {
        if (!(argWhole.isJsonObject())) {
            sb.append(prefix).append(" is not a JsonObject\n");
            return;
        }
        JsonObject whole = argWhole.getAsJsonObject();
        for (String key : subset.keySet()) {
            String fullKey = (prefix.isEmpty() ? key : prefix + "." + key);
            JsonElement value = subset.get(key);
            JsonElement value2 = whole.get(key);
            if (value.isJsonObject()) {
                assertSubsetAssist(sb, fullKey, value.getAsJsonObject(), value2);
            } else if (!value.equals(value2)
                && (value2 == null || !value2.toString().matches(value.toString()))) {
                sb.append(fullKey)
                    .append(": got ")
                    .append(String.valueOf(value2))
                    .append(", expected ")
                    .append(String.valueOf(value))
                    .append("\n");
            }
        }
    }

    /**
     * Do whatever needs to be done to start the server. I don't know exactly
     * what abstractions the various pieces provide, but the following code
     * ties the pieces together, and starts up the server.
     *
     * @param name used as the 'ServerConnector' name, and also used to generate
     *     a name for the server thread
     * @param host the host IP address to bind to
     * @param port the port to bind to
     * @param clazz the class containing the provider methods
     */
    public static void startRestServer(String name, String host, int port, Class<?> clazz) {
        ServletContextHandler context =
            new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");

        final Server jettyServer = new Server();

        ServerConnector connector = new ServerConnector(jettyServer);
        connector.setName(name);
        connector.setReuseAddress(true);
        connector.setPort(port);
        connector.setHost(host);

        jettyServer.addConnector(connector);
        jettyServer.setHandler(context);

        ServletHolder holder =
            context.addServlet(org.glassfish.jersey.servlet.ServletContainer.class.getName(), "/*");
        holder.setInitParameter(
            "jersey.config.server.provider.classnames",
            "org.onap.policy.common.gson.GsonMessageBodyHandler"
            + "," + clazz.getName());

        synchronized (runningServers) {
            runningServers.add(jettyServer);
        }

        new Thread(() -> {
            try {
                jettyServer.start();
                jettyServer.join();
                logger.info("{}: back from jettyServer.join()", name);
            } catch (Exception e) {
                logger.info(name + ": Exception starting jettyServer", e);
            }
        }, "REST Server: " + name).start();
    }

    private static boolean initNeeded = true;

    /**
     * This method starts services shared by all of the tests. The services are
     * started the first time it is invoked -- subsequent invocations have no
     * effect.
     */
    public static void commonInit() {
        if (initNeeded) {
            initNeeded = false;

            // start DMAAP Simulator
            startRestServer("simdmaap", "127.0.0.1", 3904, SimDmaap.class);

            // start Guard Simulator
            startRestServer("simguard", "127.0.0.1", 8443, SimGuard.class);

            // start PolicyEngine
            PolicyEngineConstants.getManager().configure(new Properties());
            PolicyEngineConstants.getManager().start();
        }
    }

    /**
     * This method shuts down all of the servers that were started.
     */
    public static void commonShutdown() {
        synchronized (runningServers) {
            for (Server server : runningServers) {
                try {
                    server.stop();
                } catch (Exception e) {
                    logger.info("Exception shutting down server: {}", e);
                }
            }
            runningServers.clear();
            initNeeded = true;
        }
    }

    /* ============================================================ */

    /**
     * This class is used to create an outgoing (publisher) topic message
     * channel. 'topic' is the only parameter -- everything else is hard-wired.
     */
    public static class Output {
        CambriaBatchingPublisher publisher;
        String topic;

        /**
         * Constructor - create the outgoing topic message channel.
         *
         * @param topic a DMAAP or UEB topic name
         */
        public Output(String topic) throws Exception {
            this.topic = topic;
            PublisherBuilder builder =
                new CambriaClientBuilders.PublisherBuilder();
            builder
                .usingHosts("127.0.0.1")
                .onTopic(topic)
                .withSocketTimeout(5000);
            publisher = builder.build();
        }

        /**
         * Send a JSON message out this channel.
         *
         * @param msg a 'JsonObject' containing the message to be sent
         */
        public void send(JsonObject msg) throws Exception {
            logger.info("Sending message, topic = {}\n{}",
                        topic, gson.toJson(msg));
            publisher.send("123", msg.toString());
        }

        /**
         * Close the channel.
         */
        public void close() {
            publisher.close();
        }
    }

    /* ============================================================ */

    /**
     * This class is used to create an incoming (consumer) topic message channel,
     * as well as a Thread that reads from it. Incoming messages are placed in
     * a 'LinkedBlockingQueue', which may be polled for messages.
     */
    public static class Input extends Thread {
        CambriaConsumer consumer;
        String topic;
        LinkedBlockingQueue<JsonObject> queue = new LinkedBlockingQueue<>();
        volatile boolean running = true;

        /**
         * Constructor - create the incoming topic message channel.
         *
         * @param topic a DMAAP or UEB topic name
         */
        public Input(String topic) throws Exception {
            this.topic = topic;

            // initialize reader
            ConsumerBuilder builder = new CambriaClientBuilders.ConsumerBuilder();
            builder
                .knownAs(UUID.randomUUID().toString(), "1")
                .usingHosts("127.0.0.1")
                .onTopic(topic)
                .waitAtServer(15000)
                .receivingAtMost(100)
                .withSocketTimeout(20000);
            consumer = builder.build();
            start();
        }

        /**
         * This is the Thread main loop. It fetches messages, and queues them.
         */
        @Override
        public void run() {
            while (running) {
                try {
                    for (String message : consumer.fetch()) {
                        // a message was received -- parse it as JSON
                        JsonObject msg = coder.decode(message, JsonObject.class);

                        // construct a message to print, and print it
                        logger.info("Received message, topic = {}\n{}",
                                    topic, gson.toJson(msg));

                        // queue the message
                        queue.add(msg);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        /**
         * Return the first message in the queue. If none are available, wait up
         * to 30 seconds for one to appear.
         *
         * @return a 'JsonObject' if a message has been received, and 'null' if not
         */
        public JsonObject poll() throws InterruptedException {
            return queue.poll(30, TimeUnit.SECONDS);
        }

        /**
         * Stop the thread, and close the channel.
         */
        public void close() {
            running = false;
            consumer.close();
        }
    }
}
