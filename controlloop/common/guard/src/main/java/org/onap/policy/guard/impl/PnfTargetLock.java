/*-
 * ============LICENSE_START=======================================================
 * guard
 * ================================================================================
 * Copyright (C) 2017-2019 AT&T Intellectual Property. All rights reserved.
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

package org.onap.policy.guard.impl;

import java.util.UUID;
import org.onap.policy.controlloop.policy.TargetType;
import org.onap.policy.drools.core.lock.Lock;

public class PnfTargetLock extends TargetLockImpl {

    private static final long serialVersionUID = 2335897394577202732L;

    /**
     * Construct an instance.
     *
     * @param type the target type
     * @param target the target
     * @param requestId the request Id
     * @param lock the actual lock
     */
    public PnfTargetLock(TargetType type, String target, UUID requestId, Lock lock) {
        super(type, target, requestId, lock);
    }

    @Override
    public String toString() {
        return "PnfTargetLock [" + super.toString() + "]";
    }
}
