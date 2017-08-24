/*-
 * ============LICENSE_START=======================================================
 * ActorService
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

package org.onap.policy.controlloop.actorServiceProvider;

import java.util.Iterator;
import java.util.ServiceLoader;

import org.onap.policy.controlloop.actorServiceProvider.spi.Actor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.collect.ImmutableList;

public class ActorService {

	private static final Logger logger = LoggerFactory.getLogger(ActorService.class);
	private static ActorService service;
	
	private ServiceLoader<Actor> loader;
	
	private ActorService() {
		loader = ServiceLoader.load(Actor.class);
	}
	
	public static synchronized ActorService getInstance() {
		if (service == null) {
			service = new ActorService();
		}
		return service;
	}
	
	public ImmutableList<Actor> actors() {
		Iterator<Actor> iter = loader.iterator();
		logger.debug("returning actors");
		while (iter.hasNext()) {
			logger.debug("Got {}", iter.next().actor());
		}
		
		return ImmutableList.copyOf(loader.iterator());
	}

}
