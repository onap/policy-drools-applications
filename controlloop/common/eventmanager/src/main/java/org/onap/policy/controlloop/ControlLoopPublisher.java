/*-
 * ============LICENSE_START=======================================================
 * controlloop
 * ================================================================================
 * Copyright (C) 2017-2018 AT&T Intellectual Property. All rights reserved.
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

package org.onap.policy.controlloop;

import java.lang.reflect.Constructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface ControlLoopPublisher {

    public void publish(Object object);

    public static class Factory {
        private static final Logger logger = LoggerFactory.getLogger(Factory.class);

        /**
         * Construct an instance.
         * 
         * @param className name of the class
         * @return the instance
         * @throws ControlLoopException if an error occurs
         */
        public ControlLoopPublisher buildLogger(String className) throws ControlLoopException {
            try {
                Constructor<?> constr = Class.forName(className).getConstructor();
                return (ControlLoopPublisher) constr.newInstance();
            } catch (Exception e) {
                logger.error("ControlLoopPublisher.buildLogger threw: ", e);
                throw new ControlLoopException("Cannot load class " + className + " as a control loop publisher");
            }
        }
    }
}
