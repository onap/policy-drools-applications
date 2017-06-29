/*-
 * ============LICENSE_START=======================================================
 * controlloop
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

package org.onap.policy.controlloop.eventmanager;

import java.io.Serializable;
import java.sql.Timestamp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;


@Entity
@Table(name="operationshistory10")
public class OperationsHistoryDbEntry implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Id@GeneratedValue
	@Column(name="ROWID")
	public long rowid;
	
	@Column(name="CLNAME")
	public String closedLoopName;
	
	public String requestId;
	
	public String actor;
	
	public String operation;
	
	public String target;
	
	public Timestamp starttime;
	
	public Timestamp endtime;
	
	public String subrequestId;
	
	public String outcome;
	
	public String message;

}


