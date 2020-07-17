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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides a way to handle synchronization, with minimal blocking.
 */
public class SerialWorkQueue {
    private static Logger logger = LoggerFactory.getLogger(SerialWorkQueue.class);

    // current work list
    private LinkedList<Runnable> workQueue;

    // the thread creating the SerialWorkQueue
    // (only set if an initial 'Runnable' is specified)
    private Thread initialRunThread = null;

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
        initialRunThread = Thread.currentThread();
    }

    /**
     * Called by the thread creating the SerialWorkQueue when an initial
     * 'Runnable' is specified -- it starts the first job.
     */
    public void initialRun() {
        if (Thread.currentThread() == initialRunThread) {
            initialRunThread = null;
            processQueue();
        }
    }

    /**
     * Called to add a 'Runnable' to the work queue. If the queue was empty,
     * the current thread is used to process the queue.
     *
     * @param work the Runnable to be queued, and eventually run
     */
    public void queueAndRun(Runnable work) {
        synchronized (this) {
            boolean wasEmpty = workQueue.isEmpty();
            workQueue.add(work);
            if (!wasEmpty) {
                // there was already work in the queue, so presumably there is
                // already an associated thread running
                return;
            }
            // if we reach this point, the queue was empty when this method was
            // called, so this thread will process the queue
        }

        processQueue();
    }

    /**
     * Internal method to process the work queue until it is empty. Note that
     * entries could be added by this thread or another one while we are working.
     */
    private void processQueue() {
        Runnable next = workQueue.peekFirst();
        for ( ; ; ) {
            try {
                next.run();
            } catch (Exception e) {
                logger.error("SerialWorkQueue.processQueue exception", e);
            } finally {
                synchronized (this) {
                    // remove the job we just ran
                    workQueue.removeFirst();
                    if ((next = workQueue.peekFirst()) == null) {
                        // no jobs in the queue
                        return;
                    }
                }
            }
        }
    }
}
