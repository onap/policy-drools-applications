/*-
 * ============LICENSE_START=======================================================
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
 *
 * SPDX-License-Identifier: Apache-2.0
 * ============LICENSE_END=========================================================
 */

package org.onap.policy.database.std;

import com.att.research.xacml.api.Attribute;
import com.att.research.xacml.api.AttributeValue;
import com.att.research.xacml.api.Identifier;
import com.att.research.xacml.api.XACML3;
import com.att.research.xacml.api.pip.PIPException;
import com.att.research.xacml.api.pip.PIPFinder;
import com.att.research.xacml.api.pip.PIPRequest;
import com.att.research.xacml.api.pip.PIPResponse;
import com.att.research.xacml.std.StdMutableAttribute;
import com.att.research.xacml.std.datatypes.DataTypes;
import com.att.research.xacml.std.pip.StdMutablePIPResponse;
import com.att.research.xacml.std.pip.StdPIPRequest;
import com.att.research.xacml.std.pip.engines.StdConfigurableEngine;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Properties;
import javax.persistence.EntityManager;
import javax.persistence.Persistence;
import org.apache.commons.lang3.StringUtils;
import org.onap.policy.database.ToscaDictionary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public abstract class StdOnapPip extends StdConfigurableEngine {
    protected static Logger logger = LoggerFactory.getLogger(StdOnapPip.class);

    protected static final PIPRequest PIP_REQUEST_ACTOR   = new StdPIPRequest(
            XACML3.ID_ATTRIBUTE_CATEGORY_RESOURCE,
            ToscaDictionary.ID_RESOURCE_GUARD_ACTOR,
            XACML3.ID_DATATYPE_STRING);

    protected static final PIPRequest PIP_REQUEST_RECIPE  = new StdPIPRequest(
            XACML3.ID_ATTRIBUTE_CATEGORY_RESOURCE,
            ToscaDictionary.ID_RESOURCE_GUARD_RECIPE,
            XACML3.ID_DATATYPE_STRING);

    protected static final PIPRequest PIP_REQUEST_TARGET  = new StdPIPRequest(
            XACML3.ID_ATTRIBUTE_CATEGORY_RESOURCE,
            ToscaDictionary.ID_RESOURCE_GUARD_TARGETID,
            XACML3.ID_DATATYPE_STRING);

    protected Properties properties;
    protected EntityManager em;

    public StdOnapPip() {
        super();
    }

    @Override
    public Collection<PIPRequest> attributesProvided() {
        return Collections.emptyList();
    }

    /**
     * Configures this object and initializes {@link #em}.
     *
     * @param id name of this engine
     * @param properties configuration properties
     * @param issuerName name of this issuer, used to identify the persistence unit
     * @throws PIPException if an error occurs
     */
    protected void configure(String id, Properties properties, String issuerName) throws PIPException {
        super.configure(id, properties);
        logger.debug("Configuring historyDb PIP {}", properties);
        this.properties = properties;

        //
        // Create our entity manager
        //
        em = null;
        try {
            //
            // In case there are any overloaded properties for the JPA
            //
            Properties emProperties = new Properties(properties);
            //
            // Create the entity manager factory
            //
            em = Persistence.createEntityManagerFactory(
                    properties.getProperty(issuerName + ".persistenceunit"),
                    emProperties).createEntityManager();
        } catch (Exception e) {
            logger.error("Persistence failed {} operations history db {}", e.getLocalizedMessage(), e);
        }
    }

    /**
     * Determines if a request is valid.
     *
     * @param pipRequest request to validate
     * @return {@code true} if the request is <i>NOT</i> valid, {@code false} if it is
     */
    protected boolean isRequestInvalid(PIPRequest pipRequest) {
        //
        // Determine if the issuer is correct
        //
        if (StringUtils.isBlank(pipRequest.getIssuer())) {
            logger.debug("issuer is null - returning empty response");
            //
            // We only respond to ourself as the issuer
            //
            return true;
        }
        if (! pipRequest.getIssuer().startsWith(ToscaDictionary.GUARD_ISSUER_PREFIX)) {
            logger.debug("Issuer does not start with guard");
            //
            // We only respond to ourself as the issuer
            //
            return true;
        }

        return false;
    }

    protected String getActor(PIPFinder pipFinder) {
        //
        // Get the actor value
        //
        PIPResponse pipResponse = this.getAttribute(PIP_REQUEST_ACTOR, pipFinder);
        if (pipResponse == null) {
            logger.error("Need actor attribute which is not found");
            return null;
        }
        //
        // Find the actor
        //
        return findFirstAttributeValue(pipResponse);
    }

    protected String getRecipe(PIPFinder pipFinder) {
        //
        // Get the actor value
        //
        PIPResponse pipResponse = this.getAttribute(PIP_REQUEST_RECIPE, pipFinder);
        if (pipResponse == null) {
            logger.error("Need recipe attribute which is not found");
            return null;
        }
        //
        // Find the actor
        //
        return findFirstAttributeValue(pipResponse);
    }

    protected String getTarget(PIPFinder pipFinder) {
        //
        // Get the actor value
        //
        PIPResponse pipResponse = this.getAttribute(PIP_REQUEST_TARGET, pipFinder);
        if (pipResponse == null) {
            logger.error("Need target attribute which is not found");
            return null;
        }
        //
        // Find the actor
        //
        return findFirstAttributeValue(pipResponse);
    }

    protected PIPResponse getAttribute(PIPRequest pipRequest, PIPFinder pipFinder) {
        PIPResponse pipResponse = null;
        try {
            pipResponse = pipFinder.getMatchingAttributes(pipRequest, this);
            if (pipResponse.getStatus() != null && !pipResponse.getStatus().isOk()) {
                if (logger.isInfoEnabled()) {
                    logger.info("get attribute error retrieving {}: {}", pipRequest.getAttributeId().stringValue(),
                        pipResponse.getStatus());
                }
                pipResponse = null;
            }
            if (pipResponse != null && pipResponse.getAttributes().isEmpty()) {
                if (logger.isInfoEnabled()) {
                    logger.info("No value for {}", pipRequest.getAttributeId().stringValue());
                }
                pipResponse = null;
            }
        } catch (PIPException ex) {
            logger.error("PIPException getting subject-id attribute: " + ex.getMessage(), ex);
        }
        return pipResponse;
    }

    protected String findFirstAttributeValue(PIPResponse pipResponse) {
        for (Attribute attribute : pipResponse.getAttributes()) {
            Iterator<AttributeValue<String>> iterAttributeValues = attribute.findValues(DataTypes.DT_STRING);
            if (iterAttributeValues == null) {
                continue;
            }

            while (iterAttributeValues.hasNext()) {
                String value = iterAttributeValues.next().getValue();
                if (value != null) {
                    return value;
                }
            }
        }
        return null;
    }

    protected void addIntegerAttribute(StdMutablePIPResponse stdPipResponse, Identifier category,
                    Identifier attributeId, int value, PIPRequest pipRequest) {
        try {
            AttributeValue<BigInteger> attributeValue = DataTypes.DT_INTEGER.createAttributeValue(value);
            stdPipResponse.addAttribute(new StdMutableAttribute(category, attributeId, attributeValue,
                            pipRequest.getIssuer(), false));
        } catch (Exception e) {
            logger.error("Failed to convert {} to integer {}", value, e);
        }
    }

    protected void addStringAttribute(StdMutablePIPResponse stdPipResponse, Identifier category, Identifier attributeId,
                    String value, PIPRequest pipRequest) {
        try {
            AttributeValue<String> attributeValue = DataTypes.DT_STRING.createAttributeValue(value);
            stdPipResponse.addAttribute(new StdMutableAttribute(category, attributeId, attributeValue,
                            pipRequest.getIssuer(), false));
        } catch (Exception ex) {
            logger.error("Failed to convert {} to an AttributeValue<String>", value, ex);
        }
    }

}
