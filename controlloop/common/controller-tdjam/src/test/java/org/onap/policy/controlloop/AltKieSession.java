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

package org.onap.policy.controlloop;

import java.util.Collection;
import java.util.Map;
import org.kie.api.KieBase;
import org.kie.api.command.Command;
import org.kie.api.event.process.ProcessEventListener;
import org.kie.api.event.rule.AgendaEventListener;
import org.kie.api.event.rule.RuleRuntimeEventListener;
import org.kie.api.logger.KieRuntimeLogger;
import org.kie.api.runtime.Calendars;
import org.kie.api.runtime.Channel;
import org.kie.api.runtime.Environment;
import org.kie.api.runtime.Globals;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.KieSessionConfiguration;
import org.kie.api.runtime.ObjectFilter;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.api.runtime.process.WorkItemManager;
import org.kie.api.runtime.rule.Agenda;
import org.kie.api.runtime.rule.AgendaFilter;
import org.kie.api.runtime.rule.EntryPoint;
import org.kie.api.runtime.rule.FactHandle;
import org.kie.api.runtime.rule.LiveQuery;
import org.kie.api.runtime.rule.QueryResults;
import org.kie.api.runtime.rule.ViewChangedEventListener;
import org.kie.api.time.SessionClock;
import org.onap.policy.drools.system.PolicyController;
import org.onap.policy.drools.system.PolicyControllerConstants;

/**
 * This class simulates a KieSession to the degree needed to pass the Junit
 * tests. There are many methods in 'KieSession', but the vast majority are
 * stubbed off -- doing nothing, and returning '0' or 'null'. The only
 * methods that are really implemented are the ones for adding and removing
 * 'RuleRuntimeEventListener' objects -- those are passed on to
 * 'AltTdjamController' for processing.
 */
public class AltKieSession implements KieSession {
    private AltTdjamController controller = null;

    /**
     * Constructor - initialize an 'AltKieSession', given the controller name.
     *
     * @param controllerName the name of the 'PolicyController'
     */
    public AltKieSession(String controllerName) {
        PolicyController controller =
            PolicyControllerConstants.getFactory().get(controllerName);
        this.controller = AltTdjamController.class.cast(controller);
    }

    /*==========================================*/
    /* interface org.kie.api.runtime.KieSession */
    /*==========================================*/

    public int getId() {
        return 0;
    }

    public long getIdentifier() {
        return 0;
    }

    public void dispose() {
    }

    public void destroy() {
    }

    public void submit(KieSession.AtomicAction action) {
    }

    public <T> T getKieRuntime(Class<T> cls) {
        return null;
    }

    /*========================================================*/
    /* interface org.kie.api.runtime.rule.StatefulRuleSession */
    /*========================================================*/

    public int fireAllRules() {
        return 0;
    }

    public int fireAllRules(int max) {
        return 0;
    }

    public int fireAllRules(AgendaFilter agendaFilter) {
        return 0;
    }

    public int fireAllRules(AgendaFilter agendaFilter, int max) {
        return 0;
    }

    public void fireUntilHalt() {
    }

    public void fireUntilHalt(AgendaFilter agendaFilter) {
    }

    /*==============================================================*/
    /* interface org.kie.api.runtime.process.StatefulProcessSession */
    /*==============================================================*/

    /*===============================================*/
    /* interface org.kie.api.runtime.CommandExecutor */
    /*===============================================*/

    public <T> T execute(Command<T> command) {
        return null;
    }

    /*==========================================*/
    /* interface org.kie.api.runtime.KieRuntime */
    /*==========================================*/

    public <T extends SessionClock> T getSessionClock() {
        return null;
    }

    public void setGlobal(String identifier, Object value) {
    }

    public Object getGlobal(String identifier) {
        return null;
    }

    public Globals getGlobals() {
        return null;
    }

    public Calendars getCalendars() {
        return null;
    }

    public Environment getEnvironment() {
        return null;
    }

    public KieBase getKieBase() {
        return null;
    }

    public void registerChannel(String name, Channel channel) {
    }

    public void unregisterChannel(String name) {
    }

    public Map<String, Channel> getChannels() {
        return null;
    }

    public KieSessionConfiguration getSessionConfiguration() {
        return null;
    }

    /*================================================*/
    /* interface org.kie.api.runtime.rule.RuleRuntime */
    /*================================================*/

    public void halt() {
    }

    public Agenda getAgenda() {
        return null;
    }

    public EntryPoint getEntryPoint(String name) {
        return null;
    }

    public Collection<? extends EntryPoint> getEntryPoints() {
        return null;
    }

    public QueryResults getQueryResults(String query, Object... arguments) {
        return null;
    }

    public LiveQuery openLiveQuery(String query, Object[] arguments, ViewChangedEventListener listener) {
        return null;
    }

    /*===============================================*/
    /* interface org.kie.api.runtime.rule.EntryPoint */
    /*===============================================*/

    public String getEntryPointId() {
        return null;
    }

    public FactHandle insert(Object object) {
        return null;
    }

    public void retract(FactHandle factHandle) {
    }

    public void delete(FactHandle factHandle) {
    }

    public void delete(FactHandle factHandle, FactHandle.State fhState) {
    }

    public void update(FactHandle factHandle, Object object) {
    }

    public void update(FactHandle factHandle, Object object, String... modifiedProperties) {
    }

    public FactHandle getFactHandle(Object object) {
        return null;
    }

    public Object getObject(FactHandle factHandle) {
        return null;
    }

    public Collection<? extends Object> getObjects() {
        return null;
    }

    public Collection<? extends Object> getObjects(ObjectFilter filter) {
        return null;
    }

    public <T extends FactHandle> Collection<T> getFactHandles() {
        return null;
    }

    public <T extends FactHandle> Collection<T> getFactHandles(ObjectFilter filter) {
        return null;
    }

    public long getFactCount() {
        return 0;
    }

    /*======================================================*/
    /* interface org.kie.api.runtime.process.ProcessRuntime */
    /*======================================================*/

    public ProcessInstance startProcess(String processId) {
        return null;
    }

    public ProcessInstance startProcess(String processId, Map<String, Object> parameters) {
        return null;
    }

    public ProcessInstance createProcessInstance(String processId, Map<String, Object> parameters) {
        return null;
    }

    public ProcessInstance startProcessInstance(long processInstanceId) {
        return null;
    }

    public void signalEvent(String type, Object event) {
    }

    public void signalEvent(String type, Object event, long processInstanceId) {
    }

    public Collection<ProcessInstance> getProcessInstances() {
        return null;
    }

    public ProcessInstance getProcessInstance(long processInstanceId) {
        return null;
    }

    public ProcessInstance getProcessInstance(long processInstanceId, boolean readonly) {
        return null;
    }

    public void abortProcessInstance(long processInstanceId) {
    }

    public WorkItemManager getWorkItemManager() {
        return null;
    }

    /*====================================================*/
    /* interface org.kie.api.event.KieRuntimeEventManager */
    /*====================================================*/

    public KieRuntimeLogger getLogger() {
        return null;
    }

    /*==========================================================*/
    /* interface org.kie.api.event.rule.RuleRuntimeEventManager */
    /*==========================================================*/

    //public void addEventListener(RuleRuntimeEventListener listener)

    //public void removeEventListener(RuleRuntimeEventListener listener)

    public Collection<RuleRuntimeEventListener> getRuleRuntimeEventListeners() {
        return null;
    }

    //public void addEventListener(AgendaEventListener listener)

    //public void removeEventListener(AgendaEventListener listener)

    public Collection<AgendaEventListener> getAgendaEventListeners() {
        return null;
    }

    /*=========================================================*/
    /* interface org.kie.api.event.process.ProcessEventManager */
    /*=========================================================*/

    //public void addEventListener(ProcessEventListener listener)

    //public void removeEventListener(ProcessEventListener listener)

    public Collection<ProcessEventListener> getProcessEventListeners() {
        return null;
    }

    /*===============================================================*/
    /* Unfortunately, 'Checkstyle' has a rule stating                */
    /* 'Overload methods should not be split' -- this keeps you from */
    /* organizing them according to interface.                       */
    /*===============================================================*/

    @Override
    public void addEventListener(RuleRuntimeEventListener listener) {
        if (controller != null) {
            controller.addEventListener(listener);
        }
    }

    public void addEventListener(AgendaEventListener listener) {
    }

    public void addEventListener(ProcessEventListener listener) {
    }

    @Override
    public void removeEventListener(RuleRuntimeEventListener listener) {
        if (controller != null) {
            controller.removeEventListener(listener);
        }
    }

    public void removeEventListener(AgendaEventListener listener) {
    }

    public void removeEventListener(ProcessEventListener listener) {
    }
}
