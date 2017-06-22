/*-
 * ============LICENSE_START=======================================================
 * MSOActorServiceProvider
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

package org.onap.policy.controlloop.actor.mso;

import java.util.Collections;
import java.util.List;

import org.onap.policy.controlloop.actorServiceProvider.spi.Actor;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class MSOActorServiceProvider implements Actor {

	private static final ImmutableList<String> recipes = ImmutableList.of(
			"VF Module Create");
	private static final ImmutableMap<String, List<String>> targets = new ImmutableMap.Builder<String, List<String>>()
			.put("VF Module Create", ImmutableList.of("VFC"))
			.build();
	
	@Override
	public String actor() {
		return "MSO";
	}

	@Override
	public List<String> recipes() {
		return ImmutableList.copyOf(recipes);
	}

	@Override
	public List<String> recipeTargets(String recipe) {
		return ImmutableList.copyOf(targets.getOrDefault(recipe, Collections.emptyList()));
	}

	@Override
	public List<String> recipePayloads(String recipe) {
		return Collections.emptyList();
	}

}
