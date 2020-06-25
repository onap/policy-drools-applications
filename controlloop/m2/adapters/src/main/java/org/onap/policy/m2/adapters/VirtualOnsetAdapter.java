/*-
 * ============LICENSE_START=======================================================
 * m2/adapters
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

package org.onap.policy.m2.adapters;

import java.io.Serializable;
import org.onap.policy.controlloop.ControlLoopEvent;
import org.onap.policy.controlloop.ControlLoopNotification;
import org.onap.policy.controlloop.VirtualControlLoopEvent;
import org.onap.policy.controlloop.VirtualControlLoopNotification;
import org.onap.policy.m2.base.OnsetAdapter;

public class VirtualOnsetAdapter extends OnsetAdapter implements Serializable {
    private static final long serialVersionUID = 1L;

    private static VirtualOnsetAdapter instance = new VirtualOnsetAdapter();

    /**
     * This method is called to register the 'VirtualOnsetAdapter' instance
     * under the 'VirtualControlLoopEvent' class. This method called in the
     * static initialization code of the 'Actor' classes that use this
     * adapter -- namely, 'AppcLcmActor'.
     */
    public static void register() {
        OnsetAdapter.register(VirtualControlLoopEvent.class, instance);
    }

    /**
     * This method overrides the associated 'OnsetAdapter' method.
     */
    @Override
    public ControlLoopNotification createNotification(ControlLoopEvent event) {
        if (event instanceof VirtualControlLoopEvent) {
            return new VirtualControlLoopNotification((VirtualControlLoopEvent) event);
        }

        // Right now, the onset event from the transaction is used to locate
        // the adapter. It is expected that the 'event' passed here will
        // be of the same class, but that isn't always guaranteed. If this
        // is not the case, the appropriate adapter is located in this way.
        return OnsetAdapter.get(event).createNotification(event);
    }
}
