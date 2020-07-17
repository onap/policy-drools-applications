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

package org.onap.policy.controlloop.tdjam;

import java.util.LinkedList;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides a way to handle synchronization, with minimal blocking. Requests
 * are queued until {@link #start()} is invoked.
 */
public class SerialWorkQueue {
    private static Logger logger = LoggerFactory.getLogger(SerialWorkQueue.class);

    // current work list
    private LinkedList<Runnable> workQueue;

    @Getter
    private boolean running = false;

    /**
     * Constructor - no initial Runnable.
     */
    public SerialWorkQueue() {
        workQueue = new LinkedList<>();
    }

    /**
     * Constructor - initial 'Runnable' is specified.
     *
     * @param runnable an initial 'Runnnable' to run
     */
    public SerialWorkQueue(Runnable runnable) {
        workQueue = new LinkedList<>();
        workQueue.add(runnable);
    }

    /**
     * Starts the queue. If the current thread is the first to start it, then the current
     * thread will process any requests in the queue before returning.
     */
    public void start() {
        Runnable item;

        synchronized (this) {
            if (running) {
                // already running
                return;
            }

            running = true;
            item = workQueue.peekFirst();
        }

        if (item != null) {
            processQueue(item);
        }
    }

    /**
     * Called to add a 'Runnable' to the work queue. If the queue was empty, the current
     * thread is used to process the queue.
     *
     * @param work the Runnable to be queued, and eventually run
     */
    public void queueAndRun(Runnable work) {
        synchronized (this) {
            workQueue.add(work);
            if (!running || workQueue.size() > 1) {
                // there was already work in the queue, so presumably there is
                // already an associated thread running
                return;
            }
            // if we reach this point, the queue was empty when this method was
            // called, so this thread will process the queue
        }

        processQueue(work);
    }

    /**
     * Internal method to process the work queue until it is empty. Note that entries
     * could be added by this thread or another one while we are working.
     *
     * @param firstItem the first item in the queue
     */
    private void processQueue(Runnable firstItem) {
        Runnable next = firstItem;
        while (next != null) {
            try {
                next.run();
            } catch (Exception e) {
                logger.error("SerialWorkQueue.processQueue exception", e);
            }

            synchronized (this) {
                // remove the job we just ran
                workQueue.removeFirst();
                next = workQueue.peekFirst();
            }
        }
    }
}
