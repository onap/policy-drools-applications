/*-
 * ============LICENSE_START=======================================================
 * guard
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

package org.onap.policy.guard;

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



public class PIPEngineGetHistory extends StdConfigurableEngine{
	
	private interface DateUtil{
		public class DateUtilException extends Exception {
			private static final long serialVersionUID = 2612662650481443076L;

			public DateUtilException(String message) {
				super(message);
			}
			
		}
		
        public long getMs();
        public DateUtil init(String sqlValUnit) throws DateUtilException;
    }

	private static final Logger logger = LoggerFactory.getLogger(PIPEngineGetHistory.class);

	public static final String DEFAULT_DESCRIPTION  = "PIP for retrieving Operations History from DB";
	
	

	//
	// Base issuer string. The issuer in the policy will also contain time window information
	// E.g., "com:att:research:xacml:guard:historydb:tw:10:min"
	//
	public static final String DEFAULT_ISSUER			= "com:att:research:xacml:guard:historydb";


	private static final PIPRequest PIP_REQUEST_ACTOR	= new StdPIPRequest(
					new IdentifierImpl("urn:oasis:names:tc:xacml:1.0:subject-category:access-subject"),
					new IdentifierImpl("urn:oasis:names:tc:xacml:1.0:actor:actor-id"),
					new IdentifierImpl("http://www.w3.org/2001/XMLSchema#string"));

	private static final PIPRequest PIP_REQUEST_RECIPE		= new StdPIPRequest(
					new IdentifierImpl("urn:oasis:names:tc:xacml:3.0:attribute-category:action"),
					new IdentifierImpl("urn:oasis:names:tc:xacml:1.0:operation:operation-id"),
					new IdentifierImpl("http://www.w3.org/2001/XMLSchema#string"));

	private static final PIPRequest PIP_REQUEST_TARGET		= new StdPIPRequest(
			new IdentifierImpl("urn:oasis:names:tc:xacml:3.0:attribute-category:resource"),
			new IdentifierImpl("urn:oasis:names:tc:xacml:1.0:target:target-id"),
			new IdentifierImpl("http://www.w3.org/2001/XMLSchema#string"));


	private void addIntegerAttribute(StdMutablePIPResponse stdPIPResponse, Identifier category, Identifier attributeId, int value, PIPRequest pipRequest) {
		AttributeValue<BigInteger> attributeValue	= null;
		try {
			attributeValue	= DataTypes.DT_INTEGER.createAttributeValue(value);
		} catch (Exception ex) {
			logger.error("Failed to convert {} to an AttributeValue<Boolean>",value, ex);
		}
		if (attributeValue != null) {
			stdPIPResponse.addAttribute(new StdMutableAttribute(category, attributeId, attributeValue, pipRequest.getIssuer()/*this.getIssuer()*/, false));
		}
	}



	public PIPEngineGetHistory() {
		super();
		// TODO Auto-generated constructor stub
	}



	@Override
	public Collection<PIPRequest> attributesRequired() {
		// TODO Auto-generated method stub
	    return Collections.emptySet();
	}

	@Override
	public Collection<PIPRequest> attributesProvided() {
		// TODO Auto-generated method stub
	    return Collections.emptySet();
	}

	@Override
	public PIPResponse getAttributes(PIPRequest pipRequest, PIPFinder pipFinder) throws PIPException {
		// TODO Auto-generated method stub
		logger.debug("Entering FeqLimiter PIP");

		/*
		 * First check to see if the issuer is set and then match it
		 */
		String string;
		if ((string = pipRequest.getIssuer()) == null) {

			logger.debug("No issuer in the request...");
			logger.debug("FeqLimiter PIP - No issuer in the request!");
			return StdPIPResponse.PIP_RESPONSE_EMPTY;
		}
		else{
			//Notice, we are checking here for the base issuer prefix.
			if (!string.contains(this.getIssuer())) {
				logger.debug("Requested issuer '{}' does not match {}", string, getIssuer());
				logger.debug("FeqLimiter PIP - Issuer {}  does not match with: ", string, this.getIssuer());
				return StdPIPResponse.PIP_RESPONSE_EMPTY;
			}
		}

		String[] s1 = string.split("tw:");
		String[] s2 = s1[1].split(":");
		String timeWindowVal = s2[0];// number [of minutes, hours, days...]
		String timeWindowScale = s2[1];//e.g., minute, hour, day, week, month, year

		String actor = getActor(pipFinder).iterator().next();
		String operation = getRecipe(pipFinder).iterator().next();
		String target = getTarget(pipFinder).iterator().next();

		String timeWindow = timeWindowVal + " " + timeWindowScale;

		logger.debug("Going to query DB about: {} {} {} {}", actor, operation, target, timeWindow);
		int countFromDB = getCountFromDB(actor, operation, target, timeWindow);

		StdMutablePIPResponse stdPIPResponse	= new StdMutablePIPResponse();

		this.addIntegerAttribute(stdPIPResponse,
				new IdentifierImpl("urn:oasis:names:tc:xacml:3.0:attribute-category:resource"),
				new IdentifierImpl("com:att:research:xacml:test:sql:resource:operations:count"),
				countFromDB,
				pipRequest);

		return new StdPIPResponse(stdPIPResponse);
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
		PIPResponse pipResponse	= null;

		try {
			pipResponse	= pipFinder.getMatchingAttributes(pipRequest, this);
		} catch (PIPException ex) {
			logger.error("getAttribute threw:", ex);
			return null;
		}
		if (pipResponse == null) {
			return null;
		}
		if (pipResponse.getStatus() != null && !pipResponse.getStatus().isOk()) {
			logger.warn("Error retrieving {}: {}", pipRequest.getAttributeId().stringValue(), pipResponse.getStatus().toString());
			return null;
		}
		if (pipResponse.getAttributes() != null && pipResponse.getAttributes().isEmpty()) {
			logger.warn("Error retrieving {}: {}", pipRequest.getAttributeId().stringValue(), pipResponse.getStatus().toString());
			logger.warn("Error retrieving {}: {}", pipRequest.getAttributeId().stringValue(), pipResponse.getStatus());
			return null;
		}
		if (pipResponse.getAttributes() != null && pipResponse.getAttributes().isEmpty()) {
			logger.warn("Error retrieving {}: {}", pipRequest.getAttributeId().stringValue(), pipResponse.getStatus());
			return null;
		}
		return pipResponse;
	}


	private Set<String> getActor(PIPFinder pipFinder) {
		/*
		 * Get the AT&T UID from either the subject id or the attuid property
		 */
		PIPResponse pipResponseATTUID	= this.getAttribute(PIP_REQUEST_ACTOR, pipFinder);
		if (pipResponseATTUID == null) {
			return null;
		}

		/*
		 * Iterate over all of the returned results and do the LDAP requests
		 */
		Collection<Attribute> listATTUIDs	= pipResponseATTUID.getAttributes();
		Set<String> setATTUIDs			= new HashSet<String>();
		for (Attribute attributeATTUID: listATTUIDs) {
			Iterator<AttributeValue<String>> iterAttributeValues	= attributeATTUID.findValues(DataTypes.DT_STRING);
			if (iterAttributeValues != null) {
				while (iterAttributeValues.hasNext()) {
					String attuid	= iterAttributeValues.next().getValue();
					if (attuid != null) {
						setATTUIDs.add(attuid);
					}
				}
			}
		}

		return setATTUIDs;
	}

	private Set<String> getRecipe(PIPFinder pipFinder) {
		/*
		 * Get the AT&T UID from either the subject id or the attuid property
		 */
		PIPResponse pipResponseATTUID	= this.getAttribute(PIP_REQUEST_RECIPE, pipFinder);
		if (pipResponseATTUID == null) {
			return null;
		}

		/*
		 * Iterate over all of the returned results and do the LDAP requests
		 */
		Collection<Attribute> listATTUIDs	= pipResponseATTUID.getAttributes();
		Set<String> setATTUIDs			= new HashSet<String>();
		for (Attribute attributeATTUID: listATTUIDs) {
			Iterator<AttributeValue<String>> iterAttributeValues	= attributeATTUID.findValues(DataTypes.DT_STRING);
			if (iterAttributeValues != null) {
				while (iterAttributeValues.hasNext()) {
					String attuid	= iterAttributeValues.next().getValue();
					if (attuid != null) {
						setATTUIDs.add(attuid);
					}
				}
			}
		}

		return setATTUIDs;
	}


	private Set<String> getTarget(PIPFinder pipFinder) {
		/*
		 * Get the AT&T UID from either the subject id or the attuid property
		 */
		PIPResponse pipResponseATTUID	= this.getAttribute(PIP_REQUEST_TARGET, pipFinder);
		if (pipResponseATTUID == null) {
			return null;
		}

		/*
		 * Iterate over all of the returned results and do the LDAP requests
		 */
		Collection<Attribute> listATTUIDs	= pipResponseATTUID.getAttributes();
		Set<String> setATTUIDs			= new HashSet<String>();
		for (Attribute attributeATTUID: listATTUIDs) {
			Iterator<AttributeValue<String>> iterAttributeValues	= attributeATTUID.findValues(DataTypes.DT_STRING);
			if (iterAttributeValues != null) {
				while (iterAttributeValues.hasNext()) {
					String attuid	= iterAttributeValues.next().getValue();
					if (attuid != null) {
						setATTUIDs.add(attuid);
					}
				}
			}
		}

		return setATTUIDs;
	}

	private static int getCountFromDB(String actor, String operation, String target, String timeWindow){

		// DB Properties
		Properties props = new Properties();
		props.put(Util.ECLIPSE_LINK_KEY_URL, PolicyEngine.manager.getEnvironmentProperty(Util.ONAP_KEY_URL));
		props.put(Util.ECLIPSE_LINK_KEY_USER, PolicyEngine.manager.getEnvironmentProperty(Util.ONAP_KEY_USER));
		props.put(Util.ECLIPSE_LINK_KEY_PASS, PolicyEngine.manager.getEnvironmentProperty(Util.ONAP_KEY_PASS));
		

		EntityManager em = null;
		String OpsHistPU = System.getProperty("OperationsHistoryPU");
		if(OpsHistPU == null || !OpsHistPU.equals("TestOperationsHistoryPU")){
			OpsHistPU = "OperationsHistoryPU";
		}
		else{
			props.clear();
		}
		try{
			em = Persistence.createEntityManagerFactory(OpsHistPU, props).createEntityManager();
		}catch(Exception ex){
			logger.error("PIP thread got Exception. Can't connect to Operations History DB -- {}", OpsHistPU);
			logger.error("getCountFromDB threw: ", ex);
			return 0;
		}

		DateUtil dateUtil = new DateUtil(){
			private long ms = 0;
			private double multiplier = 0;

			@Override
			public DateUtil init(String sqlValUnit) throws DateUtilException{
				String[] split = sqlValUnit.split(" ");
				if(split.length != 2){
					throw new DateUtilException("Invalid Value Unit pair for SQL");
				}

				ms = Long.parseLong(split[0]);

				if("SECOND".compareToIgnoreCase(split[1]) == 0){
					multiplier = 1000;
				}
				else if("MINUTE".compareToIgnoreCase(split[1]) == 0){
					multiplier = 60000;
				}
				else if("HOUR".compareToIgnoreCase(split[1]) == 0){
					multiplier = 3.6e+6;
				}
				else if("DAY".compareToIgnoreCase(split[1]) == 0){
					multiplier = 8.64e+7;
				}
				else if("WEEK".compareToIgnoreCase(split[1]) == 0){
					multiplier = 6.048e+8;
				}
				else if("MONTH".compareToIgnoreCase(split[1]) == 0){
					multiplier = 2.628e+9;
				}
				else if("QUARTER".compareToIgnoreCase(split[1]) == 0){
					multiplier = 2.628e+9 * 3;
				}
				else if("YEAR".compareToIgnoreCase(split[1]) == 0){
					multiplier = 3.154e+10;
				}
				else{
					logger.error("{} not supported", split[1]);
				}

				ms *= multiplier;
				return this;
			}
			public long getMs(){
				return ms;
			}
		};

		long now = new Date().getTime();
		long diff;
		try {
			diff = now - dateUtil.init(timeWindow).getMs();
		} catch (Exception ex) {
			logger.error("PIP thread got Exception " + ex);
			return -1;
		}

		String sql = "select count(*) as count from operationshistory10 where outcome<>'Failure_Guard'"
				+ " and actor= ?"
				+ " and operation= ?"
				+ " and target= ?"
				+ " and endtime between '" + new Timestamp(diff) + "' and '" + new Timestamp(now) + "'";

		Query nq = em.createNativeQuery(sql);
		nq.setParameter(0, actor);
		nq.setParameter(1, operation);
		nq.setParameter(2, target);

		int ret = -1;
		try{
			ret = ((Number)nq.getSingleResult()).intValue();
		}
		catch(NoResultException | NonUniqueResultException ex){
			logger.error("getCountFromDB threw: ", ex);
			return -1;
		}

		em.close();

		return ret;

	}


}
