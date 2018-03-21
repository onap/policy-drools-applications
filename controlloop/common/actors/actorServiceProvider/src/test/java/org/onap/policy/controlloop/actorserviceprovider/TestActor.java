/*-
 * ============LICENSE_START=======================================================
 * TestActorServiceProvider
 * ================================================================================
 * Copyright (C) 2018 Ericsson. All rights reserved.
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

package org.onap.policy.controlloop.actorserviceprovider;

import java.util.ArrayList;
import java.util.List;

import org.onap.policy.controlloop.actorserviceprovider.spi.Actor;

public class TestActor implements Actor {
    @Override
    public String actor() {
        return this.getClass().getSimpleName();
    }

    @Override
    public List<String> recipes() {
        List<String> recipieList = new ArrayList<>();
        recipieList.add("Dorothy");
        recipieList.add("Wizard");

        return recipieList;
    }

    @Override
    public List<String> recipeTargets(String recipe) {
        List<String> recipieTargetList = new ArrayList<>();
        recipieTargetList.add("Wicked Witch");
        recipieTargetList.add("Wizard of Oz");

        return recipieTargetList;
    }

    @Override
    public List<String> recipePayloads(String recipe) {
        List<String> recipiePayloadList = new ArrayList<>();
        recipiePayloadList.add("Dorothy");
        recipiePayloadList.add("Toto");

        return recipiePayloadList;
    }
}
