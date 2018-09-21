/*-
 * ============LICENSE_START=======================================================
 * guard
 * ================================================================================
 * Copyright (C) 2018 AT&T Intellectual Property. All rights reserved.
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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.NoResultException;
import javax.persistence.Persistence;
import javax.persistence.Query;

import org.onap.policy.drools.system.PolicyEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PipEngineGetStatus extends StdConfigurableEngine {
    private static final Logger logger = LoggerFactory.getLogger(PipEngineGetStatus.class);

    //
    // Base issuer string. The issuer in the policy will contain the operations
    // E.g., "org:onap:policy:guard:status:clname:testcl"
    //
    public static final String DEFAULT_ISSUER = "org:onap:policy:guard:status";
    public static final String DEFAULT_DESCRIPTION = "PIP for retrieving Operation Status from DB";

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
    private static final String XACML_TEST_SQL_RESOURCE_OPERATIONS_STATUS =
            "com:att:research:xacml:test:sql:resource:operations:status";

    private static final PIPRequest PIP_REQUEST_TARGET =
            new StdPIPRequest(new IdentifierImpl(XACML_ATTRIBUTE_CATEGORY_RESOURCE),
                    new IdentifierImpl(XACML_TARGET_TARGET_ID), new IdentifierImpl(XML_SCHEMA_STRING));

    public PipEngineGetStatus() {
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
        logger.debug("Entering Status PIP");

        /*
         * First check to see if the issuer is set and then match it
         */
        String issuer;
        if ((issuer = pipRequest.getIssuer()) == null) {

            logger.debug("No issuer in the request...");
            logger.debug("Status PIP - No issuer in the request!");
            return StdPIPResponse.PIP_RESPONSE_EMPTY;
         
        } else if (!issuer.contains(this.getIssuer())) {
            // Notice, we are checking here for the base issuer prefix.
            logger.debug("Requested issuer '{}' does not match {}", issuer, getIssuer());
            logger.debug("Status PIP - Issuer {}  does not match with: ", issuer, this.getIssuer());
            return StdPIPResponse.PIP_RESPONSE_EMPTY;
        }

        String[] s1 = issuer.split("clname:");
        String clname = s1[1];
        String target = null;
        try {
            target = getTarget(pipFinder).iterator().next();
        } catch (Exception e) {
            logger.debug("could not retrieve target from PIP finder", e);
            return StdPIPResponse.PIP_RESPONSE_EMPTY;
        }

        logger.debug("Going to query DB about: clname={}, target={}", clname, target);
        String statusFromDb = getStatusFromDb(clname, target);

        StdMutablePIPResponse stdPipResponse = new StdMutablePIPResponse();

        this.addStringAttribute(stdPipResponse, new IdentifierImpl(XACML_ATTRIBUTE_CATEGORY_RESOURCE),
                new IdentifierImpl(XACML_TEST_SQL_RESOURCE_OPERATIONS_STATUS), statusFromDb, pipRequest);

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
            logger.warn("PIP response error {}: {}", pipRequest.getAttributeId().stringValue(),
                        pipResponse.getStatus());
            return null;
        }
        if (pipResponse.getAttributes() != null && pipResponse.getAttributes().isEmpty()) {
            logger.warn("No attributes in POP response {}: {}", pipRequest.getAttributeId().stringValue(),
                        pipResponse.getStatus());
            return null;
        }
        return pipResponse;
    }

    private void addStringAttribute(StdMutablePIPResponse stdPipResponse, Identifier category, Identifier attributeId,
            String value, PIPRequest pipRequest) {
        AttributeValue<String> attributeValue = null;
        try {
            attributeValue = DataTypes.DT_STRING.createAttributeValue(value);
        } catch (Exception ex) {
            logger.error("Failed to convert {} to an AttributeValue<String>", value, ex);
        }
        if (attributeValue != null) {
            stdPipResponse.addAttribute(new StdMutableAttribute(category, attributeId, attributeValue,
                    pipRequest.getIssuer()/* this.getIssuer() */, false));
        }
    }

    private Set<String> getTarget(PIPFinder pipFinder) {
        /*
         * Get the UID from either the subject id or the uid property
         */
        PIPResponse pipResponseUid = this.getAttribute(PIP_REQUEST_TARGET, pipFinder);
        if (pipResponseUid == null) {
            return new HashSet<>();
        }

        /*
         * Iterate over all of the returned results and do the LDAP requests
         */
        Collection<Attribute> listUids = pipResponseUid.getAttributes();
        Set<String> setUids = new HashSet<>();
        for (Attribute attributeUid : listUids) {
            Iterator<AttributeValue<String>> iterAttributeValues = attributeUid.findValues(DataTypes.DT_STRING);
            if (iterAttributeValues == null) {
                continue;
            }
            while (iterAttributeValues.hasNext()) {
                String uid = iterAttributeValues.next().getValue();
                if (uid == null) {
                    continue;
                }
                setUids.add(uid);
            }
        }
        return setUids;
    }

    private static String getStatusFromDb(String clname, String target) {
        //
        // DB Properties
        //
        Properties props = new Properties();
        try {
            props.put(Util.ECLIPSE_LINK_KEY_URL, PolicyEngine.manager.getEnvironmentProperty(Util.ONAP_KEY_URL));
            props.put(Util.ECLIPSE_LINK_KEY_USER, PolicyEngine.manager.getEnvironmentProperty(Util.ONAP_KEY_USER));
            props.put(Util.ECLIPSE_LINK_KEY_PASS, PolicyEngine.manager.getEnvironmentProperty(Util.ONAP_KEY_PASS));
        } catch (NullPointerException e) {
            logger.error("getStatusFromDb: {} when setting properties", e.getMessage());
        }
        //
        // Set opsHistPu to the correct value and clear properties if necessary.
        //
        String opsHistPu = System.getProperty("OperationsHistoryPU");
        if (opsHistPu == null || !"TestOperationsHistoryPU".equals(opsHistPu)) {
            opsHistPu = "OperationsHistoryPU";
        } else {
            props.clear();
        }
        //
        // Set up the EntityManager
        //
        EntityManagerFactory emf = null;
        EntityManager em = null;
        try {
            emf = Persistence.createEntityManagerFactory(opsHistPu, props);
        } catch (Exception ex) {
            logger.error("PIP thread got Exception. Can't connect to Operations History DB -- {}", opsHistPu);
            logger.error("getStatusFromDb threw: ", ex);
            return null;
        }
        try {
            em = emf.createEntityManager();
        } catch (Exception ex) {
            logger.error("PIP thread got Exception. Problem creating EntityManager");
            logger.error("getStatusFromDb threw: ", ex);
            emf.close();
            return null;
        } 
        //
        // Create the query
        //
        String sql = "select outcome from operationshistory10 where"
            + " clname= ?"
            + " and target= ?"
            + " order by endtime desc limit 1";
        Query nq = em.createNativeQuery(sql);
        nq.setParameter(1, clname);
        nq.setParameter(2, target);
        logger.debug("SQL query: {}, {}, {}", sql, clname, target);
        //
        // Run the query
        //
        String ret = null;
        try{
            ret = ((String)nq.getSingleResult());
        } catch(NoResultException ex) {
            logger.debug("NoResultException for getSingleResult()");
            ret = "NO_MATCHING_ENTRY";
        } catch(Exception ex){
            logger.error("getStatusFromDB threw: ", ex);
        }
        if (ret != null) {
            logger.debug("SQL query result: {}", ret);
        }
        //
        // Clean up and return the result
        //
        try {
            em.close();
        } catch(Exception ex){
            logger.error("getStatusFromDB threw: ", ex);
        }
        try {
            emf.close();
        } catch(Exception ex){
            logger.error("getStatusFromDB threw: ", ex);
        }
        return ret;
    }
}
