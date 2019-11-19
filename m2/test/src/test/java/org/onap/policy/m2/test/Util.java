/*-
 * ============LICENSE_START=======================================================
 * m2/test
 * ================================================================================
 * Copyright (C) 2019 AT&T Intellectual Property. All rights reserved.
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.onap.policy.drools.system.PolicyEngineConstants;

public class Util {

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
        for (int i = 0 ; i < args.length ; i += 2) {
            text = text.replace(args[i], args[i + 1]);
        }
        return text;
    }

    /**
     * This is a convenience method to build a 'JSONObject', and populate
     * it with a set of keyword/value pairs.
     *
     * @param data this parameter comes in pairs: 'keyword', and 'value'
     * @return the populated JSONObject
     */
    public static JSONObject json(Object... data) {
        JSONObject obj = new JSONObject();
        for (int i = 0 ; i < data.length ; i += 2) {
            obj.put(data[i].toString(), data[i + 1]);
        }
        return obj;
    }

    /**
     * This method is used to check whether a JSON message has a set of fields
     * populated with the values expected.
     *
     * @param subset this is a 'JSONObject', which contains field names and
     *     values (the values are interpreted as regular expressions). The values
     *     may also be 'JSONObject' instances, in which case they are compared
     *     recursively.
     * @param whole ordinarily, this will be a 'JSONObject', and will contain
     *     a superset of the fields in 'subset'. If not, the 'assert' fails.
     */
    public static void assertSubset(JSONObject subset, Object whole) {
        StringBuilder sb = new StringBuilder();
        assertSubsetAssist(sb, "", subset, whole);
        String sbString = sb.toString();
        assertTrue(sbString, sbString.isEmpty());
    }

    /**
     * This is similar to 'assertSubset', but just returns 'true' if the
     * pattern matches.
     *
     * @param subset this is a 'JSONObject', which contains field names and
     *     values (the values are interpreted as regular expressions). The values
     *     may also be 'JSONObject' instances, in which case they are compared
     *     recursively.
     * @param whole ordinarily, this will be a 'JSONObject', and will contain
     *     a superset of the fields in 'subset'. If not, the 'assert' fails.
     * @return 'true' if 'whole' is a superset of 'subset'
     */
    public static boolean testSubset(JSONObject subset, Object whole) {
        StringBuilder sb = new StringBuilder();
        assertSubsetAssist(sb, "", subset, whole);
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
     * @param subset the 'JSONObject' being compared at this level
     * @param argWhole the value being tested -- if it is not a 'JSONObject',
     *     the comparison fails
     */
    private static void assertSubsetAssist(StringBuilder sb, String prefix, JSONObject subset, Object argWhole) {
        if (!(argWhole instanceof JSONObject)) {
            sb.append(prefix).append(" is not a JSONObject\n");
            return;
        }
        JSONObject whole = (JSONObject)argWhole;
        for (String key : subset.keySet()) {
            String fullKey = (prefix.isEmpty() ? key : prefix + "." + key);
            Object value = subset.get(key);
            Object value2 = whole.opt(key);
            if (value instanceof JSONObject) {
                assertSubsetAssist(sb, fullKey, (JSONObject) value, value2);
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
            "com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider"
            + "," + clazz.getName());

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    jettyServer.start();
                    jettyServer.join();
                    System.out.println("NOTICE: back from jettyServer.join()");
                } catch (Exception e) {
                    e.printStackTrace();
                }
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
            startRestServer("simdmaap", "127.0.71.250", 3904, SimDmaap.class);

            // start Guard Simulator
            startRestServer("simguard", "127.0.71.201", 8443, SimGuard.class);

            // start PolicyEngine
            PolicyEngineConstants.getManager().configure(new Properties());
            PolicyEngineConstants.getManager().start();
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
                .usingHosts("127.0.71.250")
                .onTopic(topic)
                .withSocketTimeout(5000);
            publisher = builder.build();
        }

        /**
         * Send a JSON message out this channel.
         *
         * @param msg a 'JSONObject' containing the message to be sent
         */
        public void send(JSONObject msg) throws Exception {
            StringBuilder sb = new StringBuilder();
            sb.append("Sending message, topic = ")
                .append(topic)
                .append("\n")
                .append(msg.toString(4));
            System.out.println(sb.toString());
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
        LinkedBlockingQueue<JSONObject> queue = new LinkedBlockingQueue<>();
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
                .usingHosts("127.0.71.250")
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
                        JSONObject msg =
                            (JSONObject) new JSONTokener(message).nextValue();

                        // construct a message to print, and print it
                        StringBuilder sb = new StringBuilder();
                        sb.append("Received message, topic = ")
                            .append(topic)
                            .append('\n')
                            .append(msg.toString(4));
                        System.out.println(sb.toString());

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
         * @return a 'JSONObject' if a message has been received, and 'null' if not
         */
        public JSONObject poll() throws InterruptedException {
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
