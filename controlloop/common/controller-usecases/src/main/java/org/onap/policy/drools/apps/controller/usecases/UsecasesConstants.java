/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2020 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2020 Wipro Limited.
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

package org.onap.policy.drools.apps.controller.usecases;

public class UsecasesConstants {

    public static final String GET_TARGET_ENTITY_ACTOR = "pseudo/getTargetEntity";
    public static final String GET_TARGET_ENTITY_OPERATION = "pseudo/getTargetEntity";

    public static final String PROV_STATUS_ACTIVE = "ACTIVE";
    public static final String VM_NAME = "VM_NAME";
    public static final String VNF_NAME = "VNF_NAME";
    public static final String GENERIC_VNF_VNF_ID = "generic-vnf.vnf-id";
    public static final String GENERIC_VNF_VNF_NAME = "generic-vnf.vnf-name";
    public static final String VSERVER_VSERVER_NAME = "vserver.vserver-name";
    public static final String GENERIC_VNF_IS_CLOSED_LOOP_DISABLED = "generic-vnf.is-closed-loop-disabled";
    public static final String VSERVER_IS_CLOSED_LOOP_DISABLED = "vserver.is-closed-loop-disabled";
    public static final String PNF_IS_IN_MAINT = "pnf.in-maint";
    public static final String GENERIC_VNF_PROV_STATUS = "generic-vnf.prov-status";
    public static final String VSERVER_PROV_STATUS = "vserver.prov-status";
    public static final String PNF_ID = "pnf.pnf-id";
    public static final String PNF_NAME = "pnf.pnf-name";
    public static final String SERVICE_INSTANCE_NAME = "service-instance.service-instance-name";

    //  additional properties that the Operations may need

    public static final String AAI_DEFAULT_GENERIC_VNF = "AAI/default/genericVnf";

    private UsecasesConstants() {
        super();
    }
}
