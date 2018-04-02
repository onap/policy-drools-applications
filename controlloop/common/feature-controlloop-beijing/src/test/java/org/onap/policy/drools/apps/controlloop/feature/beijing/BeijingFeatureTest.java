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
package org.onap.policy.drools.apps.controlloop.feature.beijing;

import static org.junit.Assert.*;

import org.onap.policy.drools.apps.controlloop.feature.beijing.BeijingFeature;

public class BeijingFeatureTest {

    @org.junit.Test
    public void getSequenceNumber() {
        assertTrue(new BeijingFeature().getSequenceNumber() == BeijingFeature.SEQNO);
    }
}
