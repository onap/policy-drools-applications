#!/bin/bash
# ============LICENSE_START=======================================================
#  Copyright (C) 2023-2025 Nordix Foundation. All rights reserved.
# ================================================================================
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# SPDX-License-Identifier: Apache-2.0
# ============LICENSE_END=========================================================

#===MAIN===#
if [ -z "${WORKSPACE}" ]; then
    export WORKSPACE=$(git rev-parse --show-toplevel)
fi

export PROJECT="drools-pdp"
export TESTDIR=${WORKSPACE}/testsuites
export DROOLS_PERF_TEST_FILE=$TESTDIR/performance/src/main/resources/amsterdam/policyMTPerformanceTestPlan.jmx
export DROOLS_STAB_TEST_FILE=$TESTDIR/stability/src/main/resources/s3p.jmx

function run_tests() {
    local test_type=$1
    local test_file=$2

    mkdir -p automate-s3p-test
    cd automate-s3p-test || exit 1
    git clone "https://gerrit.onap.org/r/policy/docker"
    cd docker/csit || exit 1

    bash run-s3p-tests.sh test "$test_file" $PROJECT
}

function clean() {
    cd $TESTDIR/automate-s3p-test/docker/csit
    bash run-s3p-tests.sh clean
}

echo "==========================="
echo "Running tests for: $PROJECT"
echo "==========================="

case $1 in
    performance)
        run_tests "performance" "$DROOLS_PERF_TEST_FILE"
        ;;
    stability)
        run_tests "stability" "$DROOLS_STAB_TEST_FILE"
        ;;
    clean)
        clean
        ;;
    *)
        echo "Invalid arguments provided. Usage: $0 {performance | stability | clean}"
        exit 1
        ;;
esac

