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
import java.sql.PreparedStatement;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.Persistence;
import javax.persistence.Query;

import com.att.research.xacml.api.pip.PIPException;
import com.att.research.xacml.api.pip.PIPFinder;
import com.att.research.xacml.api.pip.PIPRequest;
import com.att.research.xacml.api.pip.PIPResponse;
import com.att.research.xacml.std.IdentifierImpl;
import com.att.research.xacml.std.StdMutableAttribute;
import com.att.research.xacml.std.pip.StdMutablePIPResponse;
import com.att.research.xacml.std.pip.StdPIPRequest;
import com.att.research.xacml.std.pip.StdPIPResponse;
import com.att.research.xacml.std.pip.engines.StdConfigurableEngine;
import com.att.research.xacml.api.Attribute;
import com.att.research.xacml.api.AttributeValue;
import com.att.research.xacml.api.Identifier;
import com.att.research.xacml.std.datatypes.DataTypes;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;



public class PIPEngineGetHistory extends StdConfigurableEngine{

	private Log logger							= LogFactory.getLog(this.getClass());
	
	//private static EntityManager em;
	
	public static final String DEFAULT_DESCRIPTION		= "PIP for retrieving Operations History from DB";
	
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
			this.logger.error("Failed to convert " + value + " to an AttributeValue<Boolean>", ex);
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
		return null;
	}

	@Override
	public Collection<PIPRequest> attributesProvided() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PIPResponse getAttributes(PIPRequest pipRequest, PIPFinder pipFinder) throws PIPException {
		// TODO Auto-generated method stub
		System.out.println("Entering FeqLimiter PIP");
		
		/*
		 * First check to see if the issuer is set and then match it
		 */
		String string;
		if ((string = pipRequest.getIssuer()) == null) {
			this.logger.debug("No issuer in the request...");
			System.out.println("FeqLimiter PIP - No issuer in the request!");
			return StdPIPResponse.PIP_RESPONSE_EMPTY;
		}
		else{
			//Notice, we are checking here for the base issuer prefix.
			if (!string.contains(this.getIssuer())) {
				this.logger.debug("Requested issuer '" + string + "' does not match " + (this.getIssuer() == null ? "null" : "'" + this.getIssuer() + "'"));
				System.out.println("FeqLimiter PIP - Issuer "+ string +" does not match with: "+this.getIssuer());
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
		
		System.out.println("Going to query DB about: "+actor + " " + operation + " " + target + " " + timeWindow);
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
			if (pipResponse.getStatus() != null && !pipResponse.getStatus().isOk()) {
				System.out.println("Error retrieving " + pipRequest.getAttributeId().stringValue() + ": " + pipResponse.getStatus().toString());
				pipResponse	= null;
			}
			if (pipResponse.getAttributes().size() == 0) {
				System.out.println("No value for " + pipRequest.getAttributeId().stringValue());
				pipResponse	= null;
			}
		} catch (PIPException ex) {
			System.err.println("PIPException getting subject-id attribute: " + ex.getMessage());			
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
		
		//long startTime = System.nanoTime();
	
		EntityManager em;
		try{
			em = Persistence.createEntityManagerFactory("OperationsHistoryPU").createEntityManager();
		}catch(Exception e){
			System.err.println("PIP thread got Exception " + e.getLocalizedMessage() + " Can't connect to Operations History DB.");
			return -1;
		}
		
		String sql = "select count(*) as count from operationshistory10 where outcome<>'Failure_Guard'"
				+ " and actor=:actor" 
				+ " and operation=:operation" 
				+ " and target=:target" 
				+ " and endtime between date_sub(now(),interval :timeWindow) and now()"; 
 
		Query nq = em.createNativeQuery(sql); 
		nq = nq.setParameter("actor", actor); 
		nq = nq.setParameter("operation", operation); 
		nq = nq.setParameter("target", target); 
		nq = nq.setParameter("timeWindow", timeWindow);
		
		int ret = -1;
		try{
			ret = ((Number)nq.getSingleResult()).intValue();
		}
		catch(NoResultException | NonUniqueResultException ex){
			System.err.println("PIP thread got Exception " + ex.getLocalizedMessage());
			return -1;
		}
		
		//System.out.println("###########************** History count: " + ret);
		
		//long estimatedTime = System.nanoTime() - startTime;
		//System.out.println("time took: " + (double)estimatedTime/1000/1000 + " mili sec.");

		em.close();
		
		return ret;	
	
	}


}
