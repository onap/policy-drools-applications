/*-
 * ============LICENSE_START=======================================================
 * so
 * ================================================================================
 * 
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

package org.onap.policy.so;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TestSoRequestDetails {

    @Test
    public void testConstructor() {
        SORequestDetails obj = new SORequestDetails();

        assertTrue(obj.getCloudConfiguration() == null);
        assertTrue(obj.getModelInfo() == null);
        assertTrue(obj.getRequestInfo() == null);
        assertTrue(obj.getRequestParameters() == null);
        assertTrue(obj.getSubscriberInfo() == null);

        assertTrue(obj.getRelatedInstanceList() != null);
        assertEquals(0, obj.getRelatedInstanceList().size());
    }

    @Test
    public void testSetGet() {
        SORequestDetails obj = new SORequestDetails();

        SOCloudConfiguration cloudConfiguration = new SOCloudConfiguration();
        obj.setCloudConfiguration(cloudConfiguration);
        assertEquals(cloudConfiguration, obj.getCloudConfiguration());

        SOModelInfo modelInfo = new SOModelInfo();
        obj.setModelInfo(modelInfo);
        assertEquals(modelInfo, obj.getModelInfo());

        SORequestInfo requestInfo = new SORequestInfo();
        obj.setRequestInfo(requestInfo);
        assertEquals(requestInfo, obj.getRequestInfo());

        SORequestParameters requestParameters = new SORequestParameters();
        obj.setRequestParameters(requestParameters);
        assertEquals(requestParameters, obj.getRequestParameters());

        SOSubscriberInfo subscriberInfo = new SOSubscriberInfo();
        obj.setSubscriberInfo(subscriberInfo);
        assertEquals(subscriberInfo, obj.getSubscriberInfo());
    }
}
