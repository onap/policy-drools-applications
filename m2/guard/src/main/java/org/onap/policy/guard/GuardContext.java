/*-
 * ============LICENSE_START=======================================================
 * guard
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

package org.onap.policy.guard;

import java.io.InputStream;
import java.io.ObjectStreamException;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.apache.http.entity.ContentType;
import org.drools.core.WorkingMemory;
import org.json.JSONException;
import org.json.JSONObject;

import org.onap.policy.common.utils.security.CryptoUtils;
import org.onap.policy.drools.controller.DroolsController;
import org.onap.policy.drools.core.PolicyContainer;
import org.onap.policy.drools.core.PolicySession;
import org.onap.policy.drools.system.PolicyController;
import org.onap.policy.drools.system.PolicyControllerConstants;
import org.onap.policy.util.DroolsSessionCommonSerializable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Each instance of this class is initialized from a 'Properties' object,
 * which is most likely a '*-controller.properties' file. The expectation is
 * that it will be initialized within a '.drl' file, and be referenced by
 * the associated 'Context' object.
 */
public class GuardContext implements Serializable {
    private static final long serialVersionUID = 1L;

    private static Logger logger = LoggerFactory.getLogger(GuardContext.class);

    // thread pool used for background operations
    private static ExecutorService executors = Executors.newCachedThreadPool();

    // object that should be serialized
    private Object namedSerializable;

    /*==================================*/
    /* fields extracted from properties */
    /*==================================*/

    // initialized from 'guard.pdp.rest.url' property --
    // Each entry in 'restUrls' contains a destination URL, and an optional
    // 'Authorization' header entry. 'restUrlIndex' indicates the next
    // entry to try -- after each failure, the index is advanced to the
    // next entry (wrapping to the beginning, if needed).
    private static class UrlEntry implements Serializable {
        URL restUrl;
        String authorization = null;
        String clientAuth = null;
        String environment = null;
    }

    private UrlEntry[] restUrls = null;
    private int restUrlIndex = 0;

    // REST timeout, initialized from 'guard.pdp.rest.timeout' property
    private int timeout = 20000;

    // contains the four database properties, 'javax.persistence.jdbc.*',
    // initialized from 'guard.javax.persistence.jdbc.*'
    private Properties dbProperties = null;

    // initialized from 'guard.disabled', but may also be set to 'true' if
    // there is an initialization error
    private boolean disabled = false;

    // errors that forced 'disabled' to be set to 'true'
    private String errorMessage = null;

    // secret key for password decryption
    private static String secretKey = System.getenv("AES_ENCRYPTION_KEY");

    /*======================================================*/
    /* fields that shouldn't be included in serialized data */
    /*======================================================*/

    // derived from DB properties
    private transient EntityManagerFactory emf = null;

    /**
     * Constructor - initialize the 'GuardContext' instance using the
     * controller's properties file. The properties file is located using a
     * 'PolicySession' instance, but the way this mapping is done isn't
     * perfect -- it may use the wrong properties file if there is another
     * 'PolicyContainer' instance using the same 'artifactId' and 'groupId'.
     *
     * @param session the 'PolicySession' instance used to locate the associated
     *     'Properties' instance
     */
    public GuardContext(PolicySession session) {
        this(session, null);
    }

    /**
     * Constructor - initialize the 'GuardContext' instance using the
     * controller's properties file. The properties file is located using a
     * 'PolicySession' instance, but the way this mapping is done isn't
     * perfect -- it may use the wrong properties file if there is another
     * 'PolicyContainer' instance using the same 'artifactId' and 'groupId'.
     *
     * @param session the 'PolicySession' instance used to locate the associated
     *     'Properties' instance
     * @param serializableName a String name unique within the Drools session
     *     that can be used to locate the corresponding 'GuardContext' object
     *     on the remote host
     */
    public GuardContext(PolicySession session, String serializableName) {
        namedSerializable =
            (serializableName == null ? this :
             new DroolsSessionCommonSerializable(serializableName, this));

        // At present, there is no simple way to get the properties based
        // upon a 'PolicyContainer'. Instead, we search through all of the
        // 'PolicyController' instances looking for one with a matching
        // 'artifactId' and 'groupId'. Note that this may not work correctly
        // if there is more than one controller using the same or different
        // version of the same artifact.

        PolicyContainer container = session.getPolicyContainer();
        String artifactId = container.getArtifactId();
        String groupId = container.getGroupId();

        Properties properties = null;
        for (PolicyController pc : PolicyControllerConstants.getFactory().inventory()) {
            DroolsController dc = pc.getDrools();

            // don't check the version -- not sure this matches after an upgrade
            if (artifactId.equals(dc.getArtifactId())
                    && groupId.equals(dc.getGroupId())) {
                properties = pc.getProperties();
                break;
            }
        }
        init(properties);
    }

    /**
     * Constructor - initialize the 'GuardContext' instance using the
     * specified properties.
     *
     * @param properties configuration data used to initialize the 'GuardContext' instance
     */
    public GuardContext(Properties properties) {
        init(properties);
    }

    /**
     * Check if a string is null or an empty string.
     *
     * @param value the string to be tested
     * @return 'true' if the string is 'null' or has a length of 0,
     *     'false' otherwise
     */
    private static boolean nullOrEmpty(String value) {
        return value == null || value.isEmpty();
    }

    /**
     * Common initialization routine code used by both constructors.
     *
     * @param properties configuration data used to initialize the 'GuardContext' instance
     */
    private void init(Properties properties) {
        // used to store error messages
        StringBuilder sb = new StringBuilder();

        // fetch these parameters, if they exist
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
         * Decode 'guard.pdp.rest.*' parameters
         */

        // first, the default parameters
        String defaultUser = properties.getProperty("guard.pdp.rest.user");
        String defaultPassword = properties.getProperty("guard.pdp.rest.password");
        String defaultClientUser =
            properties.getProperty("guard.pdp.rest.client.user");
        String defaultClientPassword =
            properties.getProperty("guard.pdp.rest.client.password");
        String defaultEnvironment =
            properties.getProperty("guard.pdp.rest.environment");

        // now, see which numeric entries (1-9) exist
        ArrayList<UrlEntry> entries = new ArrayList<>();

        for (int index = 0 ; index < 10 ; index += 1) {
            // prefix = guard.pdp.rest.<index> (except for index 0)
            String prefix = "guard.pdp.rest.";
            if (index != 0) {
                prefix = prefix + index + ".";
            }

            // see if the associated URL exists
            String restUrlList = properties.getProperty(prefix + "url");
            if (nullOrEmpty(restUrlList)) {
                // no entry for this index
                continue;
            }

            // support a list of entries separated by semicolons. Each entry
            // can be:
            //      URL
            //      URL,user
            //      URL,user,password
            for (String restUrl : restUrlList.split("\\s*;\\s*")) {
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
                    sb.append("'").append(prefix).append("url' '")
                    .append(restUrl).append("': ")
                    .append(e).append(",");
                    continue;
                }

                if (nullOrEmpty(user)) {
                    // user id was not provided on '*.url' line --
                    // extract it from a separate property
                    user = properties.getProperty(prefix + "user", defaultUser);
                }
                if (nullOrEmpty(password)) {
                    // password was not provided on '*.url' line --
                    // extract it from a separate property
                    password = properties.getProperty(
                        prefix + "password", defaultPassword);
                }

                // see if 'user' and 'password' entries both exist
                if (!nullOrEmpty(user) && !nullOrEmpty(password)) {
                    urlEntry.authorization =
                        "Basic " + Base64.getEncoder().encodeToString(
                        (user + ":" + CryptoUtils.decrypt(password, secretKey)).getBytes());
                }

                // see if 'client.user' and 'client.password' entries both exist
                String clientUser = properties.getProperty(
                    prefix + "client.user", defaultClientUser);
                String clientPassword = properties.getProperty(
                    prefix + "client.password", defaultClientPassword);
                if (!nullOrEmpty(clientUser) && !nullOrEmpty(clientPassword)) {
                    urlEntry.clientAuth =
                        "Basic " + Base64.getEncoder().encodeToString(
                        (clientUser + ":" + CryptoUtils.decrypt(clientPassword, secretKey)).getBytes());
                }

                // see if there is an 'environment' entry
                String environment = properties.getProperty(
                    prefix + "environment", defaultEnvironment);
                if (!nullOrEmpty(environment)) {
                    urlEntry.environment = environment;
                }

                // include this UrlEntry in the list
                entries.add(urlEntry);
            }
        }

        if (entries.size() == 0) {
            sb.append("'guard.pdp.rest.*' -- no URLs specified, ");
        } else {
            restUrls = entries.toArray(new UrlEntry[0]);
        }

        String timeoutString = properties.getProperty("guard.pdp.rest.timeout");
        if (timeoutString != null) {
            try {
                // decode optional 'guard.pdp.rest.timeout' parameter
                timeout = Integer.valueOf(timeoutString);
            } catch (NumberFormatException e) {
                sb.append("'guard.pdp.rest.timeout': " + e + ", ");
            }
        }

        // extract 'guard.java.persistence.jdbc.*' parameters,
        // which are all mandatory
        dbProperties = new Properties();
        for (String propertyName :
                new String[] {"driver", "url", "user", "password"}) {
            // name of parameter used to create 'EntityManagerFactory'
            String jdbcPropertyName = "javax.persistence.jdbc." + propertyName;

            // parameter with 'guard.' prefix (as it appears in properties file)
            String guardPropertyName = "guard." + jdbcPropertyName;

            // fetch the property 'guard.javax.persistence.jdbc.*',
            // which is mandatory
            String value = properties.getProperty(guardPropertyName);
            if (propertyName == "password") {
                value = CryptoUtils.decrypt(value, secretKey);
            }
            if (value != null) {
                dbProperties.setProperty(jdbcPropertyName, value);
            } else {
                // property not defined -- append to error message
                sb.append("\"'")
                    .append(guardPropertyName)
                    .append("' is not defined, ");
            }
        }

        // if there are any errors, update 'errorMessage' & disable guard queries
        if (sb.length() != 0) {
            // remove the terminating ", ", and extract resulting error message
            sb.setLength(sb.length() - 2);
            errorMessage = sb.toString();
            disabled = true;
            logger.error("Initialization failure: " + errorMessage);
        }
    }

    /**
     * Do a synchronous (blocking) HTTP REST query to see if this operation
     * is permitted by 'guard'.
     *
     * @param actor the processor being acted upon (e.g. "APPC")
     * @param recipe otherwise known as "operation" (e.g. "Restart")
     * @param target a further qualifier on 'actor'? (e.g. "VM")
     * @param requestId the UUID string identifying the overall request
     * @return a 'PolicyGuardResponse' instance, indicating whether the
     *     operation is permitted
     */
    public PolicyGuardResponse query(
        String actor, String recipe, String target, String requestId) {
        return query(actor, recipe, target, requestId, null);
    }

    /**
     * Do a synchronous (blocking) HTTP REST query to see if this operation
     * is permitted by 'guard'.
     *
     * @param actor the processor being acted upon (e.g. "APPC")
     * @param recipe otherwise known as "operation" (e.g. "Restart")
     * @param target a further qualifier on 'actor'? (e.g. "VM")
     * @param requestId the UUID string identifying the overall request
     * @param controlLoopName the 'controlLoopName' value or 'null'
     *     (if 'null', it is ommitted from the query to 'guard')
     * @return a 'PolicyGuardResponse' instance, indicating whether the
     *     operation is permitted
     */
    public PolicyGuardResponse query(
        String actor, String recipe, String target, String requestId,
        String controlLoopName) {
        String decision = "Indeterminate";

        if (disabled) {
            if (errorMessage != null) {
                logger.error("query skipped: " + errorMessage);
            }
        } else {
            // construct JSON request
            JSONObject attributes = new JSONObject();
            attributes.put("actor", actor);
            attributes.put("recipe", recipe);
            attributes.put("target", target);
            if (controlLoopName != null) {
                attributes.put("clname", controlLoopName);
            }

            JSONObject request = new JSONObject();
            request.put("decisionAttributes", attributes);
            request.put("ecompcomponentName", "PDPD");

            String requestString = request.toString();

            logger.info("XACML Request: " + requestString);

            boolean error = false;
            String response = null;
            try {
                // Do HTTP REST request
                UrlEntry urlEntry = restUrls[restUrlIndex];
                response = callRestfulPdp(
                    requestString, urlEntry.restUrl, urlEntry.authorization,
                    urlEntry.clientAuth, urlEntry.environment);
            } catch (Exception e) {
                logger.error("Error in sending RESTful request", e);
                error = true;
            }

            logger.info("XACML Response: " + response);

            if (response == null) {
                error = true;
            } else {
                try {
                    String rawDecision =
                        new JSONObject(response).getString("decision");
                    if ("permit".equalsIgnoreCase(rawDecision)) {
                        decision = "Permit";
                    } else if ("deny".equalsIgnoreCase(rawDecision)) {
                        String details = "";
                        decision = "Deny";

                        // When guard sends a deny there is a 'details' field
                        // attached to the response. This field contains the
                        // reason guard denied (e.g. "Denied By Blacklist",
                        // "Denied By Guard", or the previous "Denied!")
                        // This reason is needed to determine whether or not
                        // we need to generate a NetCool message
                        // to inform them of the failure (eNodeB specific)
                        String rawDetails = new JSONObject(response).getString("details");
                        if (!("").equalsIgnoreCase(rawDetails) && !(rawDetails).equals(null)) {
                            details = rawDetails;
                        } else {
                            logger.error("Error determining cause of Guard deny: " + rawDetails);
                        }

                        logger.debug("Returning PolicyGuardResponse with: " + decision + ", " + details);
                        return new PolicyGuardResponse(decision, UUID.fromString(requestId), recipe);
                    }
                } catch (JSONException e) {
                    logger.error("Error decoding JSON response", e);
                    error = true;
                }
            }

            if (error) {
                // a problem has occurred --
                // advance to next URL for the next attempt
                if ((restUrlIndex += 1) >= restUrls.length) {
                    restUrlIndex = 0;
                }
            }
        }
        logger.debug("Returning PolicyGuardResponse with: " + decision);
        return new PolicyGuardResponse(decision, UUID.fromString(requestId), recipe);
    }

    /**
     * Do an asynchronous (non-blocking) HTTP REST query to see if this
     * operation is permitted by 'guard'. The response is returned by
     * inserting a 'PolicyGuardResponse' instance into Drools memory.
     *
     * @param workingMemory the Drools response is inserted here
     * @param actor the processor being acted upon (e.g. "APPC")
     * @param recipe otherwise known as "operation" (e.g. "Restart")
     * @param target a further qualifier on 'actor'? (e.g. "VM")
     * @param requestId the UUID string identifying the overall request
     */
    public void asyncQuery(
        WorkingMemory workingMemory,
        String actor, String recipe, String target,
        String requestId) {
        asyncQuery(workingMemory, actor, recipe, target, requestId, null);
    }

    /**
     * Do an asynchronous (non-blocking) HTTP REST query to see if this
     * operation is permitted by 'guard'. The response is returned by
     * inserting a 'PolicyGuardResponse' instance into Drools memory.
     *
     * @param workingMemory the Drools response is inserted here
     * @param actor the processor being acted upon (e.g. "APPC")
     * @param recipe otherwise known as "operation" (e.g. "Restart")
     * @param target a further qualifier on 'actor'? (e.g. "VM")
     * @param requestId the UUID string identifying the overall request
     * @param controlLoopName the 'controlLoopName' value or 'null'
     *     (if 'null', it is ommitted from the query to 'guard')
     */
    public void asyncQuery(
        final WorkingMemory workingMemory,
        final String actor, final String recipe, final String target,
        final String requestId, final String controlLoopName) {
        // run in the thread pool, discarding the 'Future' object returned
        executors.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    // do the synchronous query in a separate thread
                    PolicyGuardResponse guardResponse =
                        query(actor, recipe, target, requestId, controlLoopName);

                    logger.info("PolicyGuardResponse inserted with decision "
                                + guardResponse.getResult());
                    workingMemory.insert(guardResponse);
                } catch (Exception e) {
                    logger.error("GuardContext.asyncQuery", e);
                }
            }
        });
    }

    /**
     * Create an 'EntityManagerFactory', if needed, and then create a new
     * 'EntityManager' instance.
     *
     * @return a new 'EntityManager' instance
     */
    private EntityManager createEntityManager() {
        if (emf == null) {
            // 'EntityManagerFactory' does not exist yet -- create one

            // copy database properties to a 'HashMap'
            HashMap<Object,Object> propertiesMap = new HashMap<>(dbProperties);

            // use 'ClassLoader' from Drools session
            propertiesMap.put("eclipselink.classloader",
                              GuardContext.class.getClassLoader());

            // create DB tables, if needed
            propertiesMap.put("eclipselink.ddl-generation", "create-tables");

            // create entity manager factory
            emf = Persistence.createEntityManagerFactory("OperationsHistoryPU", propertiesMap);
        }

        // create and return the 'EntityManager'
        return emf.createEntityManager();
    }

    /**
     * This is a synchronous (blocking) method, which creates a database entity
     * for an in-progress request.
     *
     * @param starttime this is used as the 'starttime' timestamp in the record
     * @param endtime this is used as the 'endtime' timestamp in the record
     * @param closedLoopControlName uniquely identifies the Drools rules
     * @param actor the processor being acted upon (e.g. "APPC")
     * @param recipe otherwise known as "operation" (e.g. "Restart")
     * @param target a further qualifier on 'actor'? (e.g. "VM")
     * @param requestId the UUID string identifying the overall request
     * @param subRequestId further qualifier on 'requestId'
     * @param message indicates success status, or reason for failure
     * @param outcome 'PolicyResult' enumeration string
     * @return 'true' if the operation was successful, and 'false' if not
     */
    public boolean createDbEntry(
        Instant starttime, Instant endtime, String closedLoopControlName,
        String actor, String recipe, String target,
        String requestId, String subRequestId, String message, String outcome) {
        if (disabled) {
            if (errorMessage != null) {
                logger.error("Database update skipped: " + errorMessage);
            }
            return false;
        }

        EntityManager em = null;
        boolean rval = false;

        try {
            em = createEntityManager();

            // create the new DB table entry
            OperationsHistoryDbEntry newEntry = new OperationsHistoryDbEntry();

            // populate the new DB table entry
            newEntry.closedLoopName = closedLoopControlName;
            newEntry.requestId = requestId;
            newEntry.actor = actor;
            newEntry.operation = recipe;
            newEntry.target = target;
            newEntry.starttime = new Timestamp(starttime.toEpochMilli());;
            newEntry.subrequestId = subRequestId;

            newEntry.endtime = new Timestamp(endtime.toEpochMilli());;
            newEntry.message = message;
            newEntry.outcome = outcome;

            // store the new entry in the DB
            em.getTransaction().begin();
            em.persist(newEntry);
            em.getTransaction().commit();

            rval = true;
        } finally {
            // free EntityManager
            if (em != null) {
                em.close();
            }
        }
        return rval;
    }

    /**
     * This is an asynchronous (non-blocking) method, which creates a database
     * entity for an in-progress request.
     *
     * @param starttime this is used as the 'starttime' timestamp in the record
     * @param endtime this is used as the 'endtime' timestamp in the record
     * @param closedLoopControlName uniquely identifies the Drools rules
     * @param actor the processor being acted upon (e.g. "APPC")
     * @param recipe otherwise known as "operation" (e.g. "Restart")
     * @param target a further qualifier on 'actor'? (e.g. "VM")
     * @param requestId the UUID string identifying the overall request
     * @param subRequestId further qualifier on 'requestId'
     * @param message indicates success status, or reason for failure
     * @param outcome 'PolicyResult' enumeration string
     */
    public void asyncCreateDbEntry(
        final Instant starttime, final Instant endtime,
        final String closedLoopControlName,
        final String actor, final String recipe, final String target,
        final String requestId, final String subRequestId,
        final String message, final String outcome) {
        if (disabled) {
            if (errorMessage != null) {
                logger.error("Database update skipped: " + errorMessage);
            }
            return;
        }

        executors.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    // using a separate thread, call the synchronous 'createDbEntry'
                    // method
                    createDbEntry(starttime, endtime, closedLoopControlName,
                                  actor, recipe, target, requestId, subRequestId,
                                  message, outcome);
                } catch (Exception e) {
                    logger.error("GuardContext.asyncCreateDbEntry", e);
                }
            }
        });
    }

    /*======================================================*/
    /* support for HTTPS connections                        */
    /* (extensive borrowing from open source DroolsPDP file */
    /* 'policy-endpoints/.../JerseyClient.java')            */
    /*======================================================*/

    private static SSLSocketFactory sslSocketFactory = null;
    private static HostnameVerifier hostnameVerifier = new HostnameVerifier() {
        @Override
        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    };

    static {
        try {
            // self-signed certificates
            SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
            sslContext.init(null, new TrustManager[] {new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(
                        X509Certificate[] chain, String authType)
                        throws CertificateException {
                    }

                    @Override
                    public void checkServerTrusted(
                        X509Certificate[]  chain, String authType)
                        throws CertificateException {
                    }

                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                }
            }, new SecureRandom());
            sslSocketFactory = sslContext.getSocketFactory();
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            logger.error("Unable to construct SSLSocketFactory", e);
        }
    }

    /*=======================================================*/

    /**
     * This makes an HTTP POST call to a running PDP RESTful servlet to get a decision.
     */
    private String callRestfulPdp(String request, URL restUrl, String authorization,
        String clientauth, String environment) {
        String response = null;
        HttpURLConnection connection = null;
        try {

            //
            // Open up the connection
            //
            connection = (HttpURLConnection) restUrl.openConnection();
            if (connection instanceof HttpsURLConnection) {
                // set HTTPS parameters
                // QUESTION: is it OK to use the same 'SSLSocketFactory'
                // for every HTTPS connection?
                HttpsURLConnection conn = (HttpsURLConnection)connection;
                conn.setSSLSocketFactory(sslSocketFactory);
                conn.setHostnameVerifier(hostnameVerifier);
            }
            connection.setRequestProperty("Content-Type", "application/json");
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
                os.write(request.getBytes());
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
                        InputStream is = connection.getInputStream();
                        int contentLength = connection.getContentLength();

                        // if content length is -1, respose is chunked, and
                        // TCP connection will be dropped at the end
                        byte[] buf =
                            new byte[contentLength < 0 ? 1024 : contentLength];
                        int offset = 0;
                        for ( ; ; ) {
                            if (offset == contentLength) {
                                // all expected bytes have been read
                                response = new String(buf);
                                break;
                            }
                            int size = is.read(buf, offset,
                                               buf.length - offset);
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
                    String message = "Parsing Content-Type: " + connection.getContentType();
                    logger.error(message, e);
                }

            } else {
                logger.error(connection.getResponseCode() + " " + connection.getResponseMessage());
            }
        } catch (Exception e) {
            logger.error("Exception in 'GuardContext.callRestfulPdp'", e);
        }

        return response;
    }

    /**
     * This method is used as part of serialization -- 'namedSerializable'
     * is serialized instead of 'this'.
     *
     * @return the object to be serialized
     */
    private Object writeReplace() throws ObjectStreamException {
        return namedSerializable;
    }
}
