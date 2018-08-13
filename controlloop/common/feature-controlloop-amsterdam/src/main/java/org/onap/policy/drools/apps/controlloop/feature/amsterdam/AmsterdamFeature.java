/*
 * ============LICENSE_START=======================================================
 * ONAP
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

package org.onap.policy.drools.apps.controlloop.feature.amsterdam;

import org.onap.policy.drools.features.PolicyEngineFeatureAPI;

/**
 * Amsterdam installation as a feature saves time
 * loading the Amsterdam controller at runtime over the
 * usual installation from nexus.  It also reduces
 * potential for errors in the pom.xml generated in
 * the brmsgw side.
 *
 * <p>There is no impact on other components as the brmsgw
 * etc .. they will continue operating as usual.
 *
 * <p>This class will be expanded in the future for additional
 * functionality
 *
 */
public class AmsterdamFeature implements PolicyEngineFeatureAPI {

    public static final int SEQNO = 1000;

    @Override
    public int getSequenceNumber() {
        return SEQNO;
    }

}
