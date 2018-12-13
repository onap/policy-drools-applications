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

package org.onap.policy.drools.apps.controlloop.feature.management;

import org.junit.Assert;
import org.junit.Test;

/**
 * Control Loop Management Feature Test.
 */
public class ControlLoopManagementFeatureTest {

    /**
     * Sequence Number Test.
     */
    @Test
    public void getSequenceNumber() {
        Assert.assertEquals(1000, new ControlLoopManagementFeature().getSequenceNumber());
    }

    /**
     * Name Test.
     */
    @Test
    public void getName() {
        Assert.assertEquals("controlloop-management", new ControlLoopManagementFeature().getName());
    }
}