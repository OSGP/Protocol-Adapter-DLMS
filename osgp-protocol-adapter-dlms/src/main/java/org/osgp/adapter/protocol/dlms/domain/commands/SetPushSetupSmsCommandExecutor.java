/**
 * Copyright 2016 Smart Society Services B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.osgp.adapter.protocol.dlms.domain.commands;

import java.io.IOException;
import java.util.List;

import org.openmuc.jdlms.AccessResultCode;
import org.openmuc.jdlms.AttributeAddress;
import org.openmuc.jdlms.ClientConnection;
import org.openmuc.jdlms.ObisCode;
import org.openmuc.jdlms.SetParameter;
import org.openmuc.jdlms.datatypes.DataObject;
import org.osgp.adapter.protocol.dlms.domain.entities.DlmsDevice;
import org.osgp.adapter.protocol.dlms.exceptions.ConnectionException;
import org.osgp.adapter.protocol.dlms.exceptions.ProtocolAdapterException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.alliander.osgp.dto.valueobjects.smartmetering.PushSetupSmsDto;

@Component()
public class SetPushSetupSmsCommandExecutor extends SetPushSetupCommandExecutor implements
        CommandExecutor<PushSetupSmsDto, AccessResultCode> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SetPushSetupSmsCommandExecutor.class);
    private static final ObisCode OBIS_CODE = new ObisCode("0.1.25.9.0.255");

    @Override
    public AccessResultCode execute(final ClientConnection conn, final DlmsDevice device,
            final PushSetupSmsDto pushSetupSms) throws ProtocolAdapterException {

        final SetParameter setParameterSendDestinationAndMethod = this.getSetParameter(pushSetupSms);

        List<AccessResultCode> resultCodes;
        try {
            resultCodes = conn.set(setParameterSendDestinationAndMethod);
        } catch (final IOException e) {
            throw new ConnectionException(e);
        }
        if (resultCodes != null && !resultCodes.isEmpty()) {
            return resultCodes.get(0);
        } else {
            throw new ProtocolAdapterException("Error setting Sms push setup data.");
        }
    }

    private SetParameter getSetParameter(final PushSetupSmsDto pushSetupSms) throws ProtocolAdapterException {

        this.checkPushSetupSms(pushSetupSms);

        final AttributeAddress sendDestinationAndMethodAddress = new AttributeAddress(CLASS_ID, OBIS_CODE,
                ATTRIBUTE_ID_SEND_DESTINATION_AND_METHOD);
        final DataObject value = this.buildSendDestinationAndMethodObject(pushSetupSms.getSendDestinationAndMethod());
        return new SetParameter(sendDestinationAndMethodAddress, value);

    }

    private void checkPushSetupSms(final PushSetupSmsDto pushSetupSms) throws ProtocolAdapterException {
        if (!pushSetupSms.hasSendDestinationAndMethod()) {
            LOGGER.error("Send Destination and Method of the Push Setup Sms is expected to be set.");
            throw new ProtocolAdapterException("Error setting Sms push setup data. No destination and method data");
        }

        if (pushSetupSms.hasPushObjectList()) {
            LOGGER.warn("Setting Push Object List of Push Setup Sms not implemented: {}",
                    pushSetupSms.getPushObjectList());
        }

        if (pushSetupSms.hasCommunicationWindow()) {
            LOGGER.warn("Setting Communication Window of Push Setup Sms not implemented: {}",
                    pushSetupSms.getCommunicationWindow());
        }
        if (pushSetupSms.hasRandomisationStartInterval()) {
            LOGGER.warn("Setting Randomisation Start Interval of Push Setup Sms not implemented: {}",
                    pushSetupSms.getRandomisationStartInterval());
        }
        if (pushSetupSms.hasNumberOfRetries()) {
            LOGGER.warn("Setting Number of Retries of Push Setup Sms not implemented: {}",
                    pushSetupSms.getNumberOfRetries());
        }
        if (pushSetupSms.hasRepetitionDelay()) {
            LOGGER.warn("Setting Repetition Delay of Push Setup Sms not implemented: {}",
                    pushSetupSms.getRepetitionDelay());
        }
    }
}
