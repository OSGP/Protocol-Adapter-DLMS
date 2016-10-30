/**
 * Copyright 2015 Smart Society Services B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.osgp.adapter.protocol.dlms.domain.commands;

import java.io.IOException;

import org.openmuc.jdlms.AttributeAddress;
import org.openmuc.jdlms.GetResult;
import org.openmuc.jdlms.ObisCode;
import org.openmuc.jdlms.datatypes.DataObject;
import org.osgp.adapter.protocol.dlms.application.mapping.ConfigurationMapper;
import org.osgp.adapter.protocol.dlms.domain.entities.DlmsDevice;
import org.osgp.adapter.protocol.dlms.domain.factories.DlmsConnectionHolder;
import org.osgp.adapter.protocol.dlms.exceptions.ConnectionException;
import org.osgp.adapter.protocol.dlms.exceptions.ProtocolAdapterException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alliander.osgp.dto.valueobjects.smartmetering.ActionRequestDto;
import com.alliander.osgp.dto.valueobjects.smartmetering.ActionResponseDto;
import com.alliander.osgp.dto.valueobjects.smartmetering.AdministrativeStatusTypeDto;
import com.alliander.osgp.dto.valueobjects.smartmetering.AdministrativeStatusTypeResponseDto;
import com.alliander.osgp.dto.valueobjects.smartmetering.GetAdministrativeStatusDataDto;

@Component()
public class GetAdministrativeStatusCommandExecutor extends AbstractCommandExecutor<Void, AdministrativeStatusTypeDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(GetAdministrativeStatusCommandExecutor.class);

    private static final int CLASS_ID = 1;
    private static final ObisCode OBIS_CODE = new ObisCode("0.1.94.31.0.255");
    private static final int ATTRIBUTE_ID = 2;

    @Autowired
    private ConfigurationMapper configurationMapper;

    public GetAdministrativeStatusCommandExecutor() {
        super(GetAdministrativeStatusDataDto.class);
    }

    @Override
    public Void fromBundleRequestInput(final ActionRequestDto bundleInput) throws ProtocolAdapterException {

        this.checkActionRequestType(bundleInput);

        return null;
    }

    @Override
    public ActionResponseDto asBundleResponse(final AdministrativeStatusTypeDto executionResult)
            throws ProtocolAdapterException {

        return new AdministrativeStatusTypeResponseDto(executionResult);
    }

    @Override
    public AdministrativeStatusTypeDto execute(final DlmsConnectionHolder conn, final DlmsDevice device,
            final Void useless) throws ProtocolAdapterException {

        final AttributeAddress getParameter = new AttributeAddress(CLASS_ID, OBIS_CODE, ATTRIBUTE_ID);

        conn.getDlmsMessageListener().setDescription("GetAdministrativeStatus, retrieve attribute: "
                + JdlmsObjectToStringUtil.describeAttributes(getParameter));

        LOGGER.info(
                "Retrieving current administrative status by issuing get request for class id: {}, obis code: {}, attribute id: {}",
                CLASS_ID, OBIS_CODE, ATTRIBUTE_ID);

        GetResult getResult = null;
        try {
            getResult = conn.getConnection().get(getParameter);
        } catch (final IOException e) {
            throw new ConnectionException(e);
        }

        if (getResult == null) {
            throw new ProtocolAdapterException("No GetResult received while retrieving administrative status.");
        }

        final DataObject dataObject = getResult.getResultData();
        if (!dataObject.isNumber()) {
            throw new ProtocolAdapterException("Received unexpected result data.");
        }

        return this.configurationMapper.map((Integer) dataObject.getValue(), AdministrativeStatusTypeDto.class);
    }
}
