/**
 * Copyright 2015 Smart Society Services B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.osgp.adapter.protocol.dlms.domain.commands;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.openmuc.jdlms.AccessResultCode;
import org.openmuc.jdlms.AttributeAddress;
import org.openmuc.jdlms.ObisCode;
import org.openmuc.jdlms.SetParameter;
import org.openmuc.jdlms.datatypes.DataObject;
import org.osgp.adapter.protocol.dlms.domain.entities.DlmsDevice;
import org.osgp.adapter.protocol.dlms.domain.factories.DlmsConnectionHolder;
import org.osgp.adapter.protocol.dlms.exceptions.ConnectionException;
import org.osgp.adapter.protocol.dlms.exceptions.ProtocolAdapterException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alliander.osgp.dto.valueobjects.smartmetering.ActionRequestDto;
import com.alliander.osgp.dto.valueobjects.smartmetering.ActionResponseDto;
import com.alliander.osgp.dto.valueobjects.smartmetering.SpecialDayDto;
import com.alliander.osgp.dto.valueobjects.smartmetering.SpecialDaysRequestDataDto;

@Component()
public class SetSpecialDaysCommandExecutor extends AbstractCommandExecutor<List<SpecialDayDto>, AccessResultCode> {

    private static final int CLASS_ID = 11;
    private static final ObisCode OBIS_CODE = new ObisCode("0.0.11.0.0.255");
    private static final int ATTRIBUTE_ID = 2;

    @Autowired
    private DlmsHelperService dlmsHelperService;

    public SetSpecialDaysCommandExecutor() {
        super(SpecialDaysRequestDataDto.class);
    }

    @Override
    public List<SpecialDayDto> fromBundleRequestInput(final ActionRequestDto bundleInput)
            throws ProtocolAdapterException {

        this.checkActionRequestType(bundleInput);
        final SpecialDaysRequestDataDto specialDaysRequestDataDto = (SpecialDaysRequestDataDto) bundleInput;

        return specialDaysRequestDataDto.getSpecialDays();
    }

    @Override
    public ActionResponseDto asBundleResponse(final AccessResultCode executionResult) throws ProtocolAdapterException {

        this.checkAccessResultCode(executionResult);

        return new ActionResponseDto("Set special days was successful");
    }

    @Override
    public AccessResultCode execute(final DlmsConnectionHolder conn, final DlmsDevice device,
            final List<SpecialDayDto> specialDays) throws ProtocolAdapterException {

        final StringBuilder specialDayData = new StringBuilder();

        final List<DataObject> specialDayEntries = new ArrayList<>();
        int i = 0;
        for (final SpecialDayDto specialDay : specialDays) {

            specialDayData.append(", " + specialDay.getDayId() + " => " + specialDay.getSpecialDayDate());

            final List<DataObject> specDayEntry = new ArrayList<>();
            specDayEntry.add(DataObject.newUInteger16Data(i));
            specDayEntry.add(this.dlmsHelperService.asDataObject(specialDay.getSpecialDayDate()));
            specDayEntry.add(DataObject.newUInteger8Data((short) specialDay.getDayId()));

            final DataObject dayStruct = DataObject.newStructureData(specDayEntry);
            specialDayEntries.add(dayStruct);
            i += 1;
        }

        final AttributeAddress specialDaysTableEntries = new AttributeAddress(CLASS_ID, OBIS_CODE, ATTRIBUTE_ID);
        final DataObject entries = DataObject.newArrayData(specialDayEntries);

        final SetParameter request = new SetParameter(specialDaysTableEntries, entries);

        String specialDayValues;
        if (specialDayData.length() == 0) {
            specialDayValues = "";
        } else {
            specialDayValues = ", values [" + specialDayData.substring(2) + "]";
        }
        conn.getDlmsMessageListener().setDescription("SetSpecialDays" + specialDayValues + ", set attribute: "
                + JdlmsObjectToStringUtil.describeAttributes(specialDaysTableEntries));

        try {
            return conn.getConnection().set(request);
        } catch (final IOException e) {
            throw new ConnectionException(e);
        }
    }

}
