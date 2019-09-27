/*-
 * ============LICENSE_START=======================================================
 * ONAP
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

package org.onap.policy.controlloop.eventmanager;

import lombok.Getter;
import org.drools.core.WorkingMemory;
import org.kie.api.runtime.rule.FactHandle;
import org.onap.policy.drools.core.lock.Lock;
import org.onap.policy.drools.core.lock.LockCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lock call-back that updates working memory.
 */
@Getter
public class LockCallbackWorkingMemory implements LockCallback {
    private static final Logger logger = LoggerFactory.getLogger(LockCallbackWorkingMemory.class);

    /**
     * Name to be logged when the lock is updated.
     */
    private final String name;

    /**
     * Working memory to be updated when the lock is notified.
     */
    private final WorkingMemory workingMemory;


    /**
     * Constructs the object.
     *
     * @param name name to be logged when the lock is updated
     * @param workingMemory working memory to be updated when the lock is notified
     */
    public LockCallbackWorkingMemory(String name, WorkingMemory workingMemory) {
        this.name = name;
        this.workingMemory = workingMemory;
    }

    @Override
    public void lockAvailable(Lock lock) {
        notifySession(lock);
    }

    @Override
    public void lockUnavailable(Lock lock) {
        notifySession(lock);
    }

    /**
     * Notifies the session that the lock has been updated.
     */
    private void notifySession(Lock lock) {
        FactHandle fact = workingMemory.getFactHandle(lock);
        if (fact != null) {
            logger.debug("{}: updating lock={}", name, lock);
            workingMemory.update(fact, lock);
        }
    }
}
