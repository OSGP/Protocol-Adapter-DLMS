/**
 * Copyright 2015 Smart Society Services B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.osgp.adapter.protocol.dlms.application.services;

import java.io.Serializable;

import org.openmuc.jdlms.AccessResultCode;
import org.osgp.adapter.protocol.dlms.domain.commands.GetAssociationLnObjectsCommandExecutor;
import org.osgp.adapter.protocol.dlms.domain.commands.GetSpecificAttributeValueCommandExecutor;
import org.osgp.adapter.protocol.dlms.domain.commands.RetrieveAllAttributeValuesCommandExecutor;
import org.osgp.adapter.protocol.dlms.domain.commands.SynchronizeTimeCommandExecutor;
import org.osgp.adapter.protocol.dlms.domain.entities.DlmsDevice;
import org.osgp.adapter.protocol.dlms.domain.factories.DlmsConnectionHolder;
import org.osgp.adapter.protocol.dlms.exceptions.ProtocolAdapterException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alliander.osgp.dto.valueobjects.smartmetering.AssociationLnListTypeDto;
import com.alliander.osgp.dto.valueobjects.smartmetering.SpecificAttributeValueRequestDto;
import com.alliander.osgp.dto.valueobjects.smartmetering.SynchronizeTimeRequestDto;

@Service(value = "dlmsAdhocService")
public class AdhocService {

    @Autowired
    private SynchronizeTimeCommandExecutor synchronizeTimeCommandExecutor;

    @Autowired
    private RetrieveAllAttributeValuesCommandExecutor retrieveAllAttributeValuesCommandExecutor;

    @Autowired
    private GetSpecificAttributeValueCommandExecutor getSpecificAttributeValueCommandExecutor;

    @Autowired
    private GetAssociationLnObjectsCommandExecutor getAssociationLnObjectsCommandExecutor;

    // === REQUEST Synchronize Time DATA ===

    public void synchronizeTime(final DlmsConnectionHolder conn, final DlmsDevice device,
            final SynchronizeTimeRequestDto synchronizeTimeRequestDataDto) throws ProtocolAdapterException {
        final AccessResultCode accessResultCode = this.synchronizeTimeCommandExecutor.execute(conn, device,
                synchronizeTimeRequestDataDto);

        if (!AccessResultCode.SUCCESS.equals(accessResultCode)) {
            throw new ProtocolAdapterException("AccessResultCode for synchronizeTime: " + accessResultCode);
        }
    }

    public String retrieveAllAttributeValues(final DlmsConnectionHolder conn, final DlmsDevice device)
            throws ProtocolAdapterException {

        return this.retrieveAllAttributeValuesCommandExecutor.execute(conn, device, null);
    }

    public AssociationLnListTypeDto getAssociationLnObjects(final DlmsConnectionHolder conn, final DlmsDevice device)
            throws ProtocolAdapterException {
        return this.getAssociationLnObjectsCommandExecutor.execute(conn, device, null);
    }

    public Serializable getSpecificAttributeValue(final DlmsConnectionHolder conn, final DlmsDevice device,
            final SpecificAttributeValueRequestDto specificAttributeValueRequestDataDto)
                    throws ProtocolAdapterException {
        return this.getSpecificAttributeValueCommandExecutor.execute(conn, device,
                specificAttributeValueRequestDataDto);
    }
}
