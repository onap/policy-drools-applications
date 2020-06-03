/*-
 * ============LICENSE_START=======================================================
 * guard
 * ================================================================================
 * Copyright (C) 2020 AT&T Intellectual Property. All rights reserved.
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

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.Properties;
import java.util.UUID;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import org.drools.core.WorkingMemory;
import org.eclipse.persistence.config.PersistenceUnitProperties;
import org.onap.policy.drools.core.PolicyContainer;
import org.onap.policy.drools.core.PolicySession;
import org.onap.policy.drools.system.PolicyControllerConstants;
import org.onap.policy.drools.system.PolicyEngineConstants;
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

    // object that should be serialized
    private Object namedSerializable;

    /*==================================*/
    /* fields extracted from properties */
    /*==================================*/
    // contains the four database properties, 'javax.persistence.jdbc.*',
    private Properties dbProperties = null;

    // initialized from 'guard.disabled', but may also be set to 'true' if
    // there is an initialization error
    private boolean disabled = false;

    // errors that forced 'disabled' to be set to 'true'
    private String errorMessage = null;

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

        Properties properties =
            PolicyControllerConstants.getFactory().get(groupId, artifactId).getProperties();
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
     * Common initialization routine code used by both constructors.
     *
     * @param properties configuration data used to initialize the 'GuardContext' instance
     */
    private void init(Properties properties) {
        // used to store error messages
        StringBuilder sb = new StringBuilder();

        // fetch these parameters, if they exist
        String disabledString =
            PolicyEngineConstants.getManager().getEnvironmentProperty(Util.PROP_GUARD_DISABLED);

        if (disabledString != null) {
            // decode optional 'guard.disabled' parameter
            disabled = Boolean.valueOf(disabledString);
            if (disabled) {
                // skip everything else
                return;
            }
        }

        // extract 'guard.java.persistence.jdbc.*' parameters,
        // which are all mandatory
        dbProperties = new Properties();
        setProperty(dbProperties, Util.ONAP_KEY_URL, PersistenceUnitProperties.JDBC_URL, sb);
        setProperty(dbProperties, Util.ONAP_KEY_USER, PersistenceUnitProperties.JDBC_USER, sb);
        setProperty(dbProperties, Util.ONAP_KEY_PASS, PersistenceUnitProperties.JDBC_PASSWORD, sb);
        String driver = properties.getProperty("guard." + PersistenceUnitProperties.JDBC_DRIVER);
        if (driver != null) {
            dbProperties.setProperty(PersistenceUnitProperties.JDBC_DRIVER, driver);
        }
        dbProperties.setProperty(Util.PROP_GUARD_PERSISTENCE_UNIT,
                        properties.getProperty(Util.PROP_GUARD_PERSISTENCE_UNIT, Util.PU_KEY));

        // if there are any errors, update 'errorMessage' & disable guard queries
        if (sb.length() != 0) {
            // remove the terminating ", ", and extract resulting error message
            sb.setLength(sb.length() - 2);
            errorMessage = sb.toString();
            disabled = true;
            logger.error("Initialization failure: {}", errorMessage);
        }
    }

    /**
     * Fetch a property from the PolicyEngine environment, and store it in
     * a corresponding property in 'properties'.
     *
     * @param properties the location to store the properties
     * @param srcName source environment property name
     * @param destName destination property name
     * @param log a 'StringBuilder' used to construct an error message, if needed
     */
    private void setProperty(Properties properties, String srcName, String destName, StringBuilder log) {
        String value =
            PolicyEngineConstants.getManager().getEnvironmentProperty(srcName);
        if (value == null) {
            log.append("'").append(srcName).append("' is not defined, ");
        } else {
            properties.setProperty(destName, value);
        }
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

        if (disabled) {
            logger.error("query skipped: {}", errorMessage);
            workingMemory.insert(
                new PolicyGuardResponse("Deny", UUID.fromString(requestId), recipe));
            return;
        }

        CallGuardTask cgt = new CallGuardTask(workingMemory, controlLoopName,
            actor, recipe, target, requestId, () -> null);

        PolicyEngineConstants.getManager().getExecutorService().execute(cgt);
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
            HashMap<Object, Object> propertiesMap = new HashMap<>(dbProperties);

            // use 'ClassLoader' from Drools session
            propertiesMap.put("eclipselink.classloader",
                              GuardContext.class.getClassLoader());

            // create DB tables, if needed
            propertiesMap.put("eclipselink.ddl-generation", "create-tables");

            // create entity manager factory
            String persistenceUnit = dbProperties.getProperty(Util.PROP_GUARD_PERSISTENCE_UNIT);
            emf = Persistence.createEntityManagerFactory(persistenceUnit, propertiesMap);
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
                logger.error("Database update skipped: {}", errorMessage);
            }
            return false;
        }

        EntityManager em = null;
        boolean rval = false;

        try {
            em = createEntityManager();

            // create the new DB table entry
            OperationsHistory newEntry = new OperationsHistory();

            // populate the new DB table entry
            newEntry.setClosedLoopName(closedLoopControlName);
            newEntry.setRequestId(requestId);
            newEntry.setActor(actor);
            newEntry.setOperation(recipe);
            newEntry.setTarget(target);
            newEntry.setStarttime(new Timestamp(starttime.toEpochMilli()));
            newEntry.setSubrequestId(subRequestId);

            newEntry.setEndtime(new Timestamp(endtime.toEpochMilli()));
            newEntry.setMessage(message);
            newEntry.setOutcome(outcome);

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
                logger.error("Database update skipped: {}", errorMessage);
            }
            return;
        }

        PolicyEngineConstants.getManager().getExecutorService().execute(() -> {
            try {
                // using a separate thread, call the synchronous 'createDbEntry'
                // method
                createDbEntry(starttime, endtime, closedLoopControlName,
                              actor, recipe, target, requestId, subRequestId,
                              message, outcome);
            } catch (Exception e) {
                logger.error("GuardContext.asyncCreateDbEntry", e);
            }
        });
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
