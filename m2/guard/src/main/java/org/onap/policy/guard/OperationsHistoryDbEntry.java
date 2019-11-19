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

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Date;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.UniqueConstraint;


@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames =  {"outcome", "actor", "operation", "target", "rowid"}),
    name = "operationshistory10")
public class OperationsHistoryDbEntry implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id@GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "ROWID")
    public long rowid;

    @Column(name = "CLNAME")
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

