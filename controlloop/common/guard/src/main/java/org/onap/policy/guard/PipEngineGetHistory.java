/*-
 * ============LICENSE_START=======================================================
 * guard
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

package org.onap.policy.guard;

import com.att.research.xacml.api.Attribute;
import com.att.research.xacml.api.AttributeValue;
import com.att.research.xacml.api.Identifier;
import com.att.research.xacml.api.pip.PIPException;
import com.att.research.xacml.api.pip.PIPFinder;
import com.att.research.xacml.api.pip.PIPRequest;
import com.att.research.xacml.api.pip.PIPResponse;
import com.att.research.xacml.std.IdentifierImpl;
import com.att.research.xacml.std.StdMutableAttribute;
import com.att.research.xacml.std.datatypes.DataTypes;
import com.att.research.xacml.std.pip.StdMutablePIPResponse;
import com.att.research.xacml.std.pip.StdPIPRequest;
import com.att.research.xacml.std.pip.StdPIPResponse;
import com.att.research.xacml.std.pip.engines.StdConfigurableEngine;

import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.Persistence;
import javax.persistence.Query;

import org.onap.policy.drools.system.PolicyEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PipEngineGetHistory extends StdConfigurableEngine {
    private static final Logger logger = LoggerFactory.getLogger(PipEngineGetHistory.class);

    //
    // Base issuer string. The issuer in the policy will also contain time window information
    // E.g., "com:att:research:xacml:guard:historydb:tw:10:min"
    //
    public static final String DEFAULT_ISSUER = "com:att:research:xacml:guard:historydb";
    public static final String DEFAULT_DESCRIPTION = "PIP for retrieving Operations History from DB";

    private static final String XML_SCHEMA_STRING = "http://www.w3.org/2001/XMLSchema#string";

    private static final String XACML_SUBJECT_CATEGORY_ACCESS_SUBJECT =
            "urn:oasis:names:tc:xacml:1.0:subject-category:access-subject";
    private static final String XACML_ACTOR_ACTOR_ID = "urn:oasis:names:tc:xacml:1.0:actor:actor-id";
    private static final String XACML_ATTRIBUTE_CATEGORY_ACTION =
            "urn:oasis:names:tc:xacml:3.0:attribute-category:action";
    private static final String XACML_OPERATION_OPERATION_ID = "urn:oasis:names:tc:xacml:1.0:operation:operation-id";
    private static final String XACML_ATTRIBUTE_CATEGORY_RESOURCE =
            "urn:oasis:names:tc:xacml:3.0:attribute-category:resource";
    private static final String XACML_TARGET_TARGET_ID = "urn:oasis:names:tc:xacml:1.0:target:target-id";
    private static final String XACML_TEST_SQL_RESOURCE_OPERATIONS_COUNT =
            "com:att:research:xacml:test:sql:resource:operations:count";

    private static final PIPRequest PIP_REQUEST_ACTOR =
            new StdPIPRequest(new IdentifierImpl(XACML_SUBJECT_CATEGORY_ACCESS_SUBJECT),
                    new IdentifierImpl(XACML_ACTOR_ACTOR_ID), new IdentifierImpl(XML_SCHEMA_STRING));

    private static final PIPRequest PIP_REQUEST_RECIPE =
            new StdPIPRequest(new IdentifierImpl(XACML_ATTRIBUTE_CATEGORY_ACTION),
                    new IdentifierImpl(XACML_OPERATION_OPERATION_ID), new IdentifierImpl(XML_SCHEMA_STRING));

    private static final PIPRequest PIP_REQUEST_TARGET =
            new StdPIPRequest(new IdentifierImpl(XACML_ATTRIBUTE_CATEGORY_RESOURCE),
                    new IdentifierImpl(XACML_TARGET_TARGET_ID), new IdentifierImpl(XML_SCHEMA_STRING));

    public PipEngineGetHistory() {
        super();
    }

    @Override
    public Collection<PIPRequest> attributesRequired() {
        return Collections.emptySet();
    }

    @Override
    public Collection<PIPRequest> attributesProvided() {
        return Collections.emptySet();
    }

    @Override
    public PIPResponse getAttributes(PIPRequest pipRequest, PIPFinder pipFinder) throws PIPException {
        logger.debug("Entering FeqLimiter PIP");

        /*
         * First check to see if the issuer is set and then match it
         */
        String string;
        if ((string = pipRequest.getIssuer()) == null) {

            logger.debug("No issuer in the request...");
            logger.debug("FeqLimiter PIP - No issuer in the request!");
            return StdPIPResponse.PIP_RESPONSE_EMPTY;
        } else {
            // Notice, we are checking here for the base issuer prefix.
            if (!string.contains(this.getIssuer())) {
                logger.debug("Requested issuer '{}' does not match {}", string, getIssuer());
                logger.debug("FeqLimiter PIP - Issuer {}  does not match with: {}", string, this.getIssuer());
                return StdPIPResponse.PIP_RESPONSE_EMPTY;
            }
        }

        String[] s1 = string.split("tw:");
        String[] s2 = s1[1].split(":");
        String timeWindowVal = s2[0];// number [of minutes, hours, days...]
        String timeWindowScale = s2[1];// e.g., minute, hour, day, week, month, year

        String actor = null;
        String operation = null;
        String target = null;
        try {
            actor = getActor(pipFinder).iterator().next();
            operation = getRecipe(pipFinder).iterator().next();
            target = getTarget(pipFinder).iterator().next();
        } catch (Exception e) {
            logger.debug("could not retrieve actor, operation, or target from PIP finder", e);
            return StdPIPResponse.PIP_RESPONSE_EMPTY;
        }

        String timeWindow = timeWindowVal + " " + timeWindowScale;

        logger.debug("Going to query DB about: {} {} {} {}", actor, operation, target, timeWindow);
        int countFromDb = getCountFromDb(actor, operation, target, timeWindow);

        StdMutablePIPResponse stdPipResponse = new StdMutablePIPResponse();

        this.addIntegerAttribute(stdPipResponse, new IdentifierImpl(XACML_ATTRIBUTE_CATEGORY_RESOURCE),
                new IdentifierImpl(XACML_TEST_SQL_RESOURCE_OPERATIONS_COUNT), countFromDb, pipRequest);

        return new StdPIPResponse(stdPipResponse);
    }

    @Override
    public void configure(String id, Properties properties) throws PIPException {
        super.configure(id, properties);

        if (this.getDescription() == null) {
            this.setDescription(DEFAULT_DESCRIPTION);
        }
        if (this.getIssuer() == null) {
            this.setIssuer(DEFAULT_ISSUER);
        }
    }

    private PIPResponse getAttribute(PIPRequest pipRequest, PIPFinder pipFinder) {
        PIPResponse pipResponse = null;

        try {
            pipResponse = pipFinder.getMatchingAttributes(pipRequest, this);
        } catch (PIPException ex) {
            logger.error("getAttribute threw:", ex);
            return null;
        }
        if (pipResponse == null) {
            return null;
        }
        if (pipResponse.getStatus() != null && !pipResponse.getStatus().isOk()) {
            if (logger.isWarnEnabled()) {
                logger.warn("PIP response error {}: {}", pipRequest.getAttributeId().stringValue(),
                        pipResponse.getStatus());
            }
            return null;
        }
        if (pipResponse.getAttributes() != null && pipResponse.getAttributes().isEmpty()) {
            if (logger.isWarnEnabled()) {
                logger.warn("No attributes in POP response {}: {}", pipRequest.getAttributeId().stringValue(),
                        pipResponse.getStatus());
            }
            return null;
        }
        return pipResponse;
    }

    private Set<String> getActor(PIPFinder pipFinder) {
        /*
         * Get the AT&T UID from either the subject id or the attuid property
         */
        PIPResponse pipResponseAttUid = this.getAttribute(PIP_REQUEST_ACTOR, pipFinder);
        if (pipResponseAttUid == null) {
            return new HashSet<>();
        }

        /*
         * Iterate over all of the returned results and do the LDAP requests
         */
        Collection<Attribute> listAttUids = pipResponseAttUid.getAttributes();
        Set<String> setAttUids = new HashSet<>();
        for (Attribute attributeAttUid : listAttUids) {
            Iterator<AttributeValue<String>> iterAttributeValues = attributeAttUid.findValues(DataTypes.DT_STRING);
            if (iterAttributeValues != null) {
                while (iterAttributeValues.hasNext()) {
                    String attuid = iterAttributeValues.next().getValue();
                    if (attuid != null) {
                        setAttUids.add(attuid);
                    }
                }
            }
        }

        return setAttUids;
    }

    private Set<String> getRecipe(PIPFinder pipFinder) {
        /*
         * Get the AT&T UID from either the subject id or the attuid property
         */
        PIPResponse pipResponseAttUid = this.getAttribute(PIP_REQUEST_RECIPE, pipFinder);
        if (pipResponseAttUid == null) {
            return new HashSet<>();
        }

        /*
         * Iterate over all of the returned results and do the LDAP requests
         */
        Collection<Attribute> listAttUids = pipResponseAttUid.getAttributes();
        Set<String> setAttUids = new HashSet<>();
        for (Attribute attributeAttUid : listAttUids) {
            Iterator<AttributeValue<String>> iterAttributeValues = attributeAttUid.findValues(DataTypes.DT_STRING);
            if (iterAttributeValues != null) {
                while (iterAttributeValues.hasNext()) {
                    String attuid = iterAttributeValues.next().getValue();
                    if (attuid != null) {
                        setAttUids.add(attuid);
                    }
                }
            }
        }

        return setAttUids;
    }

    private void addIntegerAttribute(StdMutablePIPResponse stdPipResponse, Identifier category, Identifier attributeId,
            int value, PIPRequest pipRequest) {
        AttributeValue<BigInteger> attributeValue = null;
        try {
            attributeValue = DataTypes.DT_INTEGER.createAttributeValue(value);
        } catch (Exception ex) {
            logger.error("Failed to convert {} to an AttributeValue<Boolean>", value, ex);
        }
        if (attributeValue != null) {
            stdPipResponse.addAttribute(new StdMutableAttribute(category, attributeId, attributeValue,
                    pipRequest.getIssuer()/* this.getIssuer() */, false));
        }
    }

    private Set<String> getTarget(PIPFinder pipFinder) {
        /*
         * Get the AT&T UID from either the subject id or the attuid property
         */
        PIPResponse pipResponseAttUid = this.getAttribute(PIP_REQUEST_TARGET, pipFinder);
        if (pipResponseAttUid == null) {
            return new HashSet<>();
        }

        /*
         * Iterate over all of the returned results and do the LDAP requests
         */
        Collection<Attribute> listAttUids = pipResponseAttUid.getAttributes();
        Set<String> setAttUids = new HashSet<>();
        for (Attribute attributeAttUid : listAttUids) {
            Iterator<AttributeValue<String>> iterAttributeValues = attributeAttUid.findValues(DataTypes.DT_STRING);
            if (iterAttributeValues != null) {
                while (iterAttributeValues.hasNext()) {
                    String attuid = iterAttributeValues.next().getValue();
                    if (attuid != null) {
                        setAttUids.add(attuid);
                    }
                }
            }
        }

        return setAttUids;
    }

    private static int getCountFromDb(String actor, String operation, String target, String timeWindow) {
        // DB Properties
        Properties props = new Properties();
        props.put(Util.ECLIPSE_LINK_KEY_URL, PolicyEngine.manager.getEnvironmentProperty(Util.ONAP_KEY_URL));
        props.put(Util.ECLIPSE_LINK_KEY_USER, PolicyEngine.manager.getEnvironmentProperty(Util.ONAP_KEY_USER));
        props.put(Util.ECLIPSE_LINK_KEY_PASS, PolicyEngine.manager.getEnvironmentProperty(Util.ONAP_KEY_PASS));


        EntityManager em = null;
        String opsHistPu = System.getProperty("OperationsHistoryPU");
        if (!"TestOperationsHistoryPU".equals(opsHistPu)) {
            opsHistPu = "OperationsHistoryPU";
        } else {
            props.clear();
        }

        try {
            em = Persistence.createEntityManagerFactory(opsHistPu, props).createEntityManager();
        } catch (Exception ex) {
            logger.error("PIP thread got Exception. Can't connect to Operations History DB -- {}", opsHistPu);
            logger.error("getCountFromDb threw: ", ex);
            return -1;
        }

        long now = new Date().getTime();
        long diff;
        try {
            diff = now - getMsFromTimeWindow(timeWindow);
        } catch (Exception ex) {
            logger.error("PIP thread got Exception ", ex);
            return -1;
        }

        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("select count(*) as count from operationshistory10 where outcome<>'Failure_Guard'");
        sqlBuilder.append(" and actor= ?");
        sqlBuilder.append(" and operation= ?");
        sqlBuilder.append(" and target= ?");
        sqlBuilder.append(" and endtime between '");
        sqlBuilder.append(new Timestamp(diff));
        sqlBuilder.append("' and '");
        sqlBuilder.append(new Timestamp(now));
        sqlBuilder.append('\'');

        Query nq = em.createNativeQuery(sqlBuilder.toString());
        nq.setParameter(1, actor);
        nq.setParameter(2, operation);
        nq.setParameter(3, target);

        int ret = -1;
        try {
            ret = ((Number) nq.getSingleResult()).intValue();
        } catch (NoResultException | NonUniqueResultException ex) {
            logger.error("getCountFromDb threw: ", ex);
            return -1;
        }

        em.close();

        return ret;
    }

    /**
     * Get the Millisecond time from a time window string.
     * 
     * @param timeWindow the time window string to parse
     * @return the millisecond time from the time window string
     * @throws PIPException On invalid time window strings
     */
    private static long getMsFromTimeWindow(String timeWindowString) throws PIPException {
        long ms = 0;
        double multiplier = 0;

        String[] split = timeWindowString.split(" ");
        if (split.length != 2) {
            throw new PIPException("Invalid Value Unit pair for SQL");
        }

        ms = Long.parseLong(split[0]);

        if ("SECOND".compareToIgnoreCase(split[1]) == 0) {
            multiplier = 1000;
        } else if ("MINUTE".compareToIgnoreCase(split[1]) == 0) {
            multiplier = 60000;
        } else if ("HOUR".compareToIgnoreCase(split[1]) == 0) {
            multiplier = 3.6e+6;
        } else if ("DAY".compareToIgnoreCase(split[1]) == 0) {
            multiplier = 8.64e+7;
        } else if ("WEEK".compareToIgnoreCase(split[1]) == 0) {
            multiplier = 6.048e+8;
        } else if ("MONTH".compareToIgnoreCase(split[1]) == 0) {
            multiplier = 2.628e+9;
        } else if ("QUARTER".compareToIgnoreCase(split[1]) == 0) {
            multiplier = 2.628e+9 * 3;
        } else if ("YEAR".compareToIgnoreCase(split[1]) == 0) {
            multiplier = 3.154e+10;
        } else {
            logger.error("{} not supported", split[1]);
        }

        ms *= multiplier;
        return ms;
    }
}
