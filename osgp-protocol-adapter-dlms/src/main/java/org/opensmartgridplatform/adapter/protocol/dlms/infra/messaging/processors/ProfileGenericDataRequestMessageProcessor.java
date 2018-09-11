/**
 * Copyright 2017 Smart Society Services B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.opensmartgridplatform.adapter.protocol.dlms.infra.messaging.processors;

import java.io.Serializable;

import org.opensmartgridplatform.adapter.protocol.dlms.application.services.MonitoringService;
import org.opensmartgridplatform.adapter.protocol.dlms.domain.entities.DlmsDevice;
import org.opensmartgridplatform.adapter.protocol.dlms.domain.factories.DlmsConnectionHolder;
import org.opensmartgridplatform.adapter.protocol.dlms.infra.messaging.DeviceRequestMessageProcessor;
import org.opensmartgridplatform.adapter.protocol.dlms.infra.messaging.DeviceRequestMessageType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.opensmartgridplatform.dto.valueobjects.smartmetering.ProfileGenericDataRequestDto;
import org.opensmartgridplatform.shared.exceptionhandling.OsgpException;

/**
 * Class for processing Periodic Meter Request messages
 */
@Component
public class ProfileGenericDataRequestMessageProcessor extends DeviceRequestMessageProcessor {

    @Autowired
    private MonitoringService monitoringService;

    public ProfileGenericDataRequestMessageProcessor() {
        super(DeviceRequestMessageType.GET_PROFILE_GENERIC_DATA);
    }

    @Override
    protected Serializable handleMessage(final DlmsConnectionHolder conn, final DlmsDevice device,
            final Serializable requestObject) throws OsgpException {

        this.assertRequestObjectType(ProfileGenericDataRequestDto.class, requestObject);

        final ProfileGenericDataRequestDto periodicMeterReadsQuery = (ProfileGenericDataRequestDto) requestObject;

        return this.monitoringService.requestProfileGenericData(conn, device, periodicMeterReadsQuery);
    }
}
