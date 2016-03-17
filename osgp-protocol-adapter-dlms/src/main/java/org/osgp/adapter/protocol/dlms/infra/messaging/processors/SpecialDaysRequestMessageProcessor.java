/**
 * Copyright 2015 Smart Society Services B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.osgp.adapter.protocol.dlms.infra.messaging.processors;

import java.io.Serializable;

import org.openmuc.jdlms.ClientConnection;
import org.osgp.adapter.protocol.dlms.application.jasper.sessionproviders.exceptions.SessionProviderException;
import org.osgp.adapter.protocol.dlms.application.services.ConfigurationService;
import org.osgp.adapter.protocol.dlms.domain.entities.DlmsDevice;
import org.osgp.adapter.protocol.dlms.exceptions.ProtocolAdapterException;
import org.osgp.adapter.protocol.dlms.infra.messaging.DeviceRequestMessageProcessor;
import org.osgp.adapter.protocol.dlms.infra.messaging.DeviceRequestMessageType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alliander.osgp.dto.valueobjects.smartmetering.SpecialDaysRequestDto;
import com.alliander.osgp.shared.exceptionhandling.OsgpException;

/**
 * Class for processing Special Days Request messages
 */
@Component("dlmsSpecialDaysRequestMessageProcessor")
public class SpecialDaysRequestMessageProcessor extends DeviceRequestMessageProcessor {

    @Autowired
    private ConfigurationService configurationService;

    public SpecialDaysRequestMessageProcessor() {
        super(DeviceRequestMessageType.REQUEST_SPECIAL_DAYS);
    }

    @Override
    protected Serializable handleMessage(final ClientConnection conn, final DlmsDevice device,
            final Serializable requestObject) throws OsgpException, ProtocolAdapterException, SessionProviderException {
        final SpecialDaysRequestDto specialDaysRequest = (SpecialDaysRequestDto) requestObject;

        this.configurationService.requestSpecialDays(conn, device, specialDaysRequest);
        return null;
    }
}
