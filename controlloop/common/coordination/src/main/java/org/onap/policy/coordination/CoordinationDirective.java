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

package org.onap.policy.coordination;

public class CoordinationDirective {

    private String controlLoopOne;
    private String controlLoopTwo;
    private String coordinationFunction;

    public String getControlLoopOne() {
        return controlLoopOne;
    }

    public void setControlLoopOne(String controlLoopOne) {
        this.controlLoopOne = controlLoopOne;
    }

    public String getControlLoopTwo() {
        return controlLoopTwo;
    }

    public void setControlLoopTwo(String controlLoopTwo) {
        this.controlLoopTwo = controlLoopTwo;
    }

    public String getCoordinationFunction() {
        return coordinationFunction;
    }

    public void setCoordinationFunction(String coordinationFunction) {
        this.coordinationFunction = coordinationFunction;
    }

    /**
     * toString.
     *
     * @return the CoordinationDirective's string representation
     */
    public String toString() {
        return "CoordinationDirective[ "
            + controlLoopOne + " / "
            + controlLoopTwo + " / "
            + coordinationFunction + "]";
    }

}
