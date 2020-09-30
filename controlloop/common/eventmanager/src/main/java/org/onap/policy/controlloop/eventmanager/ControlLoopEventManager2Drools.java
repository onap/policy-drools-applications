/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2020 AT&T Intellectual Property. All rights reserved.
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

import org.drools.core.WorkingMemory;
import org.kie.api.runtime.rule.FactHandle;
import org.onap.policy.controlloop.ControlLoopException;
import org.onap.policy.controlloop.VirtualControlLoopEvent;
import org.onap.policy.controlloop.drl.legacy.ControlLoopParams;

/**
 * Manager for a single control loop event. Once this has been created, the event can be
 * retracted from working memory. Once this has been created, {@link #start()} should be
 * invoked, and then {@link #nextStep()} should be invoked continually until
 * {@link #isActive()} returns {@code false}, indicating that all steps have completed.
 */
public class ControlLoopEventManager2Drools extends ControlLoopEventManager2 {
    private static final long serialVersionUID = 1L;

    private final transient WorkingMemory workMem;
    private transient FactHandle factHandle;

    /**
     * Constructs the object.
     *
     * @param params control loop parameters
     * @param event event to be managed by this object
     * @param workMem working memory to update if this changes
     * @throws ControlLoopException if the event is invalid or if a YAML processor cannot
     *         be created
     */
    public ControlLoopEventManager2Drools(ControlLoopParams params, VirtualControlLoopEvent event,
        WorkingMemory workMem) throws ControlLoopException {

        super(params, event);
        this.workMem = workMem;
    }

    /**
     * This is a hook added to 'ControlLoopEventManager2.start()' --
     * here, we add an additional check.
     */
    @Override
    protected void startHook() {
        if ((factHandle = workMem.getFactHandle(this)) == null) {
            throw new IllegalStateException("manager is not in working memory");
        }
    }

    /**
     * This is a hook added to 'ControlLoopEventManager2.updated(...)' --
     * here, we mark it as updated in Drools memory.
     */
    @Override
    protected void notifyUpdate() {
        workMem.update(factHandle, this);
    }
}
