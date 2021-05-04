/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2021 AT&T Intellectual Property. All rights reserved.
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

import java.util.HashMap;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.drools.core.WorkingMemory;
import org.onap.policy.controlloop.ControlLoopEventStatus;
import org.onap.policy.controlloop.ControlLoopException;
import org.onap.policy.controlloop.ControlLoopResponse;
import org.onap.policy.controlloop.VirtualControlLoopEvent;
import org.onap.policy.controlloop.VirtualControlLoopNotification;
import org.onap.policy.controlloop.actorserviceprovider.OperationOutcome;
import org.onap.policy.controlloop.drl.legacy.ControlLoopParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manager for a single control loop event. Once this has been created, the event can be
 * retracted from working memory.
 */
public abstract class ClEventManagerWithEvent<T extends Step> extends ClEventManagerWithOutcome<T>
                implements StepContext {

    private static final Logger logger = LoggerFactory.getLogger(ClEventManagerWithEvent.class);
    private static final long serialVersionUID = -1216568161322872641L;

    public enum NewEventStatus {
        FIRST_ONSET, SUBSEQUENT_ONSET, FIRST_ABATEMENT, SUBSEQUENT_ABATEMENT, SYNTAX_ERROR
    }

    @Getter
    private final VirtualControlLoopEvent event;

    @Getter
    @Setter(AccessLevel.PROTECTED)
    private VirtualControlLoopEvent abatement = null;


    /**
     * Constructs the object.
     *
     * @param services services the manager should use when processing the event
     * @param params control loop parameters
     * @param event event to be managed by this object
     * @param workMem working memory to update if this changes
     * @throws ControlLoopException if the event is invalid or if a YAML processor cannot
     *         be created
     */
    public ClEventManagerWithEvent(EventManagerServices services, ControlLoopParams params,
                    VirtualControlLoopEvent event, WorkingMemory workMem) throws ControlLoopException {

        super(services, params, event.getRequestId(), workMem);

        checkEventSyntax(event);

        this.event = event;
    }

    @Override
    protected void populateNotification(VirtualControlLoopNotification notif) {
        super.populateNotification(notif);

        notif.setClosedLoopControlName(event.getClosedLoopControlName());
        notif.setRequestId(event.getRequestId());
        notif.setClosedLoopEventClient(event.getClosedLoopEventClient());
        notif.setTargetType(event.getTargetType());
        notif.setTarget(event.getTarget());

        if (event.getAai() != null) {
            notif.setAai(new HashMap<>(event.getAai()));
        }
        notif.setClosedLoopAlarmStart(event.getClosedLoopAlarmStart());
        notif.setClosedLoopAlarmEnd(event.getClosedLoopAlarmEnd());
    }

    /**
     * Stores an operation outcome in the DB.
     *
     * @param outcome operation outcome to store
     * @param targetEntity target entity
     */
    protected void storeInDataBase(OperationOutcome2 outcome, String targetEntity) {
        getDataManager().store(getRequestIdStr(), event.getClosedLoopControlName(), event, targetEntity,
                        outcome.getClOperation());
    }

    @Override
    public ControlLoopResponse makeControlLoopResponse(OperationOutcome outcome) {
        ControlLoopResponse clRsp = super.makeControlLoopResponse(outcome);
        clRsp.setTarget("DCAE");

        clRsp.setClosedLoopControlName(event.getClosedLoopControlName());
        clRsp.setPolicyName(event.getPolicyName());
        clRsp.setPolicyVersion(event.getPolicyVersion());
        clRsp.setRequestId(event.getRequestId());
        clRsp.setVersion(event.getVersion());

        return clRsp;
    }

    /**
     * An event onset/abatement.
     *
     * @param newEvent the event
     * @return the status
     */
    public NewEventStatus onNewEvent(VirtualControlLoopEvent newEvent) {
        try {
            checkEventSyntax(newEvent);

            if (newEvent.getClosedLoopEventStatus() == ControlLoopEventStatus.ONSET) {
                if (newEvent.equals(event)) {
                    return NewEventStatus.FIRST_ONSET;
                }

                bumpOffsets();
                return NewEventStatus.SUBSEQUENT_ONSET;

            } else {
                if (abatement == null) {
                    abatement = newEvent;
                    bumpAbatements();
                    return NewEventStatus.FIRST_ABATEMENT;
                } else {
                    bumpAbatements();
                    return NewEventStatus.SUBSEQUENT_ABATEMENT;
                }
            }
        } catch (ControlLoopException e) {
            logger.error("{}: onNewEvent threw an exception", this, e);
            return NewEventStatus.SYNTAX_ERROR;
        }
    }

    /**
     * Check an event syntax.
     *
     * @param event the event syntax
     * @throws ControlLoopException if an error occurs
     */
    protected void checkEventSyntax(VirtualControlLoopEvent event) throws ControlLoopException {
        validateStatus(event);
        if (StringUtils.isBlank(event.getClosedLoopControlName())) {
            throw new ControlLoopException("No control loop name");
        }
        if (event.getRequestId() == null) {
            throw new ControlLoopException("No request ID");
        }
        if (event.getClosedLoopEventStatus() == ControlLoopEventStatus.ABATED) {
            return;
        }
        validateTarget(event);
    }

    /**
     * Verifies that the event status is valid.
     *
     * @param event event to check
     * @throws ControlLoopException if the status is invalid
     */
    protected void validateStatus(VirtualControlLoopEvent event) throws ControlLoopException {
        if (event.getClosedLoopEventStatus() != ControlLoopEventStatus.ONSET
                        && event.getClosedLoopEventStatus() != ControlLoopEventStatus.ABATED) {
            throw new ControlLoopException("Invalid value in closedLoopEventStatus");
        }
    }

    /**
     * Verifies that the event target is valid.
     *
     * @param event event to check
     * @throws ControlLoopException if the status is invalid
     */
    protected void validateTarget(VirtualControlLoopEvent event) throws ControlLoopException {
        if (StringUtils.isBlank(event.getTarget())) {
            throw new ControlLoopException("No target field");
        }
    }
}
