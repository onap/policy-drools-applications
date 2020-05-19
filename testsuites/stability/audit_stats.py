# ================================================================================
# Copyright (c) 2020 AT&T Intellectual Property. All rights reserved.
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
# ============LICENSE_END=========================================================

import pandas as pd
import matplotlib.pyplot as plt

def stats(name, file):
    ets = pd.read_csv(file, sep='|')['EllapsedTime']
    print(name, ":")
    print(ets.describe(percentiles=[]), "\n")
    plt.clf()
    plt.title(name)
    plt.hist(ets, range=[ets.min(), ets.max()], align='mid', density=False, bins=100)
    plt.ylabel('Frequency')
    plt.xlabel('Transaction Processing Time');
    plt.savefig(name + '.png')

# This script is to be used in conjuction with the s3p.jmx jmeter script
# Please refer to the ONAP readthedocs for additional information.

stats("ControlLoop vCPE Success", "ControlLoop-vCPE-48f0c2c3-a172-4192-9ae3-052274181b6e.log")
stats("ControlLoop vCPE Fail", "ControlLoop-vCPE-Fail.log")
stats("ControlLoop vDNS Success", "ControlLoop-vDNS-6f37f56d-a87d-4b85-b6a9-cc953cf779b3.log")
stats("ControlLoop vDNS Fail", "ControlLoop-vDNS-Fail.log")
stats("ControlLoop vFirewall Success", "ControlLoop-vFirewall-d0a1dfc6-94f5-4fd4-a5b5-4630b438850a.log")
