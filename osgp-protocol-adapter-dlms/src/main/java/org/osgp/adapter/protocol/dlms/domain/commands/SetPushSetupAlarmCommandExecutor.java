/**
 * Copyright 2015 Smart Society Services B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.osgp.adapter.protocol.dlms.domain.commands;

import java.io.IOException;

import org.openmuc.jdlms.AccessResultCode;
import org.openmuc.jdlms.AttributeAddress;
import org.openmuc.jdlms.ObisCode;
import org.openmuc.jdlms.SetParameter;
import org.openmuc.jdlms.datatypes.DataObject;
import org.osgp.adapter.protocol.dlms.domain.entities.DlmsDevice;
import org.osgp.adapter.protocol.dlms.domain.factories.DlmsConnectionHolder;
import org.osgp.adapter.protocol.dlms.exceptions.ConnectionException;
import org.osgp.adapter.protocol.dlms.exceptions.ProtocolAdapterException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.alliander.osgp.dto.valueobjects.smartmetering.ActionRequestDto;
import com.alliander.osgp.dto.valueobjects.smartmetering.ActionResponseDto;
import com.alliander.osgp.dto.valueobjects.smartmetering.PushSetupAlarmDto;
import com.alliander.osgp.dto.valueobjects.smartmetering.SetPushSetupAlarmRequestDto;

@Component()
public class SetPushSetupAlarmCommandExecutor extends SetPushSetupCommandExecutor<PushSetupAlarmDto, AccessResultCode> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SetPushSetupAlarmCommandExecutor.class);
    private static final ObisCode OBIS_CODE = new ObisCode("0.1.25.9.0.255");

    public SetPushSetupAlarmCommandExecutor() {
        super(SetPushSetupAlarmRequestDto.class);
    }

    @Override
    public PushSetupAlarmDto fromBundleRequestInput(final ActionRequestDto bundleInput) throws ProtocolAdapterException {

        this.checkActionRequestType(bundleInput);
        final SetPushSetupAlarmRequestDto setPushSetupAlarmRequestDto = (SetPushSetupAlarmRequestDto) bundleInput;

        return setPushSetupAlarmRequestDto.getPushSetupAlarm();
    }

    @Override
    public ActionResponseDto asBundleResponse(final AccessResultCode executionResult) throws ProtocolAdapterException {

        this.checkAccessResultCode(executionResult);

        return new ActionResponseDto("Setting push setup alarm was successful");
    }

    @Override
    public AccessResultCode execute(final DlmsConnectionHolder conn, final DlmsDevice device,
            final PushSetupAlarmDto pushSetupAlarm) throws ProtocolAdapterException {

        final SetParameter setParameterSendDestinationAndMethod = this.getSetParameter(pushSetupAlarm);

        conn.getDlmsMessageListener()
                .setDescription("SetPushSetupAlarm configure send destination and method, set attribute: "
                        + JdlmsObjectToStringUtil.describeAttributes(
                                new AttributeAddress(CLASS_ID, OBIS_CODE, ATTRIBUTE_ID_SEND_DESTINATION_AND_METHOD)));

        AccessResultCode resultCode;
        try {
            resultCode = conn.getConnection().set(setParameterSendDestinationAndMethod);
        } catch (final IOException e) {
            throw new ConnectionException(e);
        }

        if (resultCode != null) {
            return resultCode;
        } else {
            throw new ProtocolAdapterException("Error setting Alarm push setup data.");
        }
    }

    private SetParameter getSetParameter(final PushSetupAlarmDto pushSetupAlarm) throws ProtocolAdapterException {

        this.checkPushSetupAlarm(pushSetupAlarm);

        final AttributeAddress sendDestinationAndMethodAddress = new AttributeAddress(CLASS_ID, OBIS_CODE,
                ATTRIBUTE_ID_SEND_DESTINATION_AND_METHOD);
        final DataObject value = this.buildSendDestinationAndMethodObject(pushSetupAlarm.getSendDestinationAndMethod());
        return new SetParameter(sendDestinationAndMethodAddress, value);
    }

    private void checkPushSetupAlarm(final PushSetupAlarmDto pushSetupAlarm) throws ProtocolAdapterException {
        if (!pushSetupAlarm.hasSendDestinationAndMethod()) {
            LOGGER.error("Send Destination and Method of the Push Setup Alarm is expected to be set.");
            throw new ProtocolAdapterException("Error setting Alarm push setup data. No destination and method data");
        }

        if (pushSetupAlarm.hasPushObjectList()) {
            LOGGER.warn("Setting Push Object List of Push Setup Alarm not implemented: {}",
                    pushSetupAlarm.getPushObjectList());
        }

        if (pushSetupAlarm.hasCommunicationWindow()) {
            LOGGER.warn("Setting Communication Window of Push Setup Alarm not implemented: {}",
                    pushSetupAlarm.getCommunicationWindow());
        }
        if (pushSetupAlarm.hasRandomisationStartInterval()) {
            LOGGER.warn("Setting Randomisation Start Interval of Push Setup Alarm not implemented: {}",
                    pushSetupAlarm.getRandomisationStartInterval());
        }
        if (pushSetupAlarm.hasNumberOfRetries()) {
            LOGGER.warn("Setting Number of Retries of Push Setup Alarm not implemented: {}",
                    pushSetupAlarm.getNumberOfRetries());
        }
        if (pushSetupAlarm.hasRepetitionDelay()) {
            LOGGER.warn("Setting Repetition Delay of Push Setup Alarm not implemented: {}",
                    pushSetupAlarm.getRepetitionDelay());
        }
    }
}
