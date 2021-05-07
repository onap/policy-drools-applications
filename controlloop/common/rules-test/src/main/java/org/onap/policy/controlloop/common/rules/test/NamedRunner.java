/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2020-2021 AT&T Intellectual Property. All rights reserved.
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

package org.onap.policy.controlloop.common.rules.test;

import org.junit.Ignore;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;

/**
 * Runs tests listed via the {@link TestNames} annotation.
 */
public class NamedRunner extends BlockJUnit4ClassRunner {

    /**
     * Constructs the object.
     */
    public NamedRunner(Class<?> testClass) throws InitializationError {
        super(testClass);
    }

    @Override
    protected void runChild(final FrameworkMethod method, RunNotifier notifier) {
        var description = describeChild(method);

        if (method.getAnnotation(Ignore.class) != null) {
            notifier.fireTestIgnored(description);

        } else if (!isNamed(description.getTestClass(), method.getName())) {
            notifier.fireTestIgnored(description);

        } else {
            runLeaf(methodBlock(method), description, notifier);
        }
    }

    /**
     * Determines if the test is in the list of tests to be included.
     *
     * @param testClass class under test
     * @param testName name of the test of interest
     * @return {@code true} if the test is in the list, {@code false} otherwise
     */
    private boolean isNamed(Class<?> testClass, String testName) {
        TestNames annot = testClass.getAnnotation(TestNames.class);
        if (annot == null) {
            // no annotation - everything passes
            return true;
        }

        for (String name : annot.names()) {
            if (testName.equals(name)) {
                return true;
            }
        }

        for (String prefix : annot.prefixes()) {
            if (testName.startsWith(prefix)) {
                return true;
            }
        }

        return false;
    }
}
