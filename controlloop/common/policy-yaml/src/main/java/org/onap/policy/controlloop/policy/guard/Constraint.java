/*-
 * ============LICENSE_START=======================================================
 * policy-yaml
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

package org.onap.policy.controlloop.policy.guard;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class Constraint {

    private Integer freqLimitPerTarget;
    private Map<String,String> timeWindow;
    private Map<String, String> activeTimeRange;
    
    private List<String> blacklist;
    
    public Constraint() {
        // Do Nothing empty constructor. 
    }

    public Integer getFreq_limit_per_target() {
        return freqLimitPerTarget;
    }


    public void setFreq_limit_per_target(Integer freqLimitPerTarget) {
        this.freqLimitPerTarget = freqLimitPerTarget;
    }


    public Map<String, String> getTime_window() {
        return timeWindow;
    }


    public void setTime_window(Map<String, String> timeWindow) {
        this.timeWindow = timeWindow;
    }


    public Map<String, String> getActive_time_range() {
        return activeTimeRange;
    }


    public void setActive_time_range(Map<String, String> activeTimeRange) {
        this.activeTimeRange = activeTimeRange;
    }


    public List<String> getBlacklist() {
        return blacklist;
    }

    public void setBlacklist(List<String> blacklist) {
        this.blacklist = blacklist;
    }

    public Constraint(Integer freqLimitPerTarget, Map<String, String> timeWindow) {
        this.freqLimitPerTarget = freqLimitPerTarget;
        if(timeWindow!=null){
            this.timeWindow = Collections.unmodifiableMap(timeWindow);
        }
    }
    
    public Constraint(List<String> blacklist) {
        this.blacklist = new LinkedList<>(blacklist);
    }
    
    public Constraint(Integer freqLimitPerTarget, Map<String, String> timeWindow, List<String> blacklist) {
        this.freqLimitPerTarget = freqLimitPerTarget;
        this.timeWindow = Collections.unmodifiableMap(timeWindow);
        this.blacklist = new LinkedList<>(blacklist);
    }
    
    public Constraint(Integer freqLimitPerTarget, Map<String, String> timeWindow, Map<String, String> activeTimeRange) {
        this(freqLimitPerTarget, timeWindow);
        if (activeTimeRange != null) {
            this.activeTimeRange = Collections.unmodifiableMap(activeTimeRange);
        }
    }
    
    public Constraint(Integer freqLimitPerTarget, Map<String, String> timeWindow, Map<String, String> activeTimeRange, List<String> blacklist) {
        this(freqLimitPerTarget, timeWindow);
        if (activeTimeRange != null) {
            this.activeTimeRange = Collections.unmodifiableMap(activeTimeRange);
        }
        if(blacklist!=null){
            this.blacklist = new LinkedList<>(blacklist);
        }
    }
    
    public Constraint(Constraint constraint) {
        this.freqLimitPerTarget = constraint.freqLimitPerTarget;
        this.timeWindow = constraint.timeWindow;
        if (constraint.activeTimeRange != null) {
            this.activeTimeRange = Collections.unmodifiableMap(constraint.activeTimeRange);
        }
        this.blacklist = new LinkedList<>(constraint.blacklist);
    }
    
    public boolean isValid() {
        return ((freqLimitPerTarget == null && timeWindow != null)|| (timeWindow == null && freqLimitPerTarget != null))? false : true;
    }
    
    @Override
    public String toString() {
        return "Constraint [freq_limit_per_target=" + freqLimitPerTarget + ", time_window=" + timeWindow + ", active_time_range=" + activeTimeRange + ", blacklist=" + blacklist + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((freqLimitPerTarget == null) ? 0 : freqLimitPerTarget.hashCode());
        result = prime * result + ((timeWindow == null) ? 0 : timeWindow.hashCode());
        result = prime * result + ((activeTimeRange == null) ? 0 : activeTimeRange.hashCode());
        result = prime * result + ((blacklist == null) ? 0 : blacklist.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Constraint other = (Constraint) obj;
        return equalsMayBeNull(freqLimitPerTarget, other.freqLimitPerTarget)
        		&& equalsMayBeNull(timeWindow, other.timeWindow)
        		&& equalsMayBeNull(activeTimeRange, other.activeTimeRange)
        		&& equalsMayBeNull(blacklist, other.blacklist);
    }
    
    private boolean equalsMayBeNull(final Object obj1, final Object obj2){
    	if ( obj1 == null ) {
            return obj2 == null;
        } 
    	return obj1.equals(obj2);
    }
}
