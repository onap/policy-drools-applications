/*-
 * ============LICENSE_START=======================================================
 * actor test
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

package org.onap.policy.controlloop.actor.test;

import static org.junit.Assert.*;

import org.onap.policy.controlloop.actorServiceProvider.ActorService;
import org.onap.policy.controlloop.actorServiceProvider.spi.Actor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Test {
	private static final Logger logger = LoggerFactory.getLogger(Test.class);
	
	@org.junit.Test
	public void test() {
		logger.debug("Dumping actors");
		ActorService actorService = ActorService.getInstance();
		assertNotNull(actorService);
		int num = 0;
		for (Actor actor : actorService.actors()) {
			logger.debug(actor.actor());
			for (String recipe : actor.recipes()) {
				logger.debug("\t {} {} {}", recipe, actor.recipeTargets(recipe), actor.recipePayloads(recipe));
			}
			num++;
		}
		logger.debug("Found {} actors", num);
	}

}
