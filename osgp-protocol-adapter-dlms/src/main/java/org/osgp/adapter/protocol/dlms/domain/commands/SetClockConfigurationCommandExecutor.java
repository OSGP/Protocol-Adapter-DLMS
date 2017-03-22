/**
 * Copyright 2017 Smart Society Services B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.osgp.adapter.protocol.dlms.domain.commands;

import java.io.IOException;

import org.openmuc.jdlms.AccessResultCode;
import org.openmuc.jdlms.AttributeAddress;
import org.openmuc.jdlms.GetResult;
import org.openmuc.jdlms.ObisCode;
import org.openmuc.jdlms.SetParameter;
import org.openmuc.jdlms.datatypes.CosemDateTime;
import org.openmuc.jdlms.datatypes.DataObject;
import org.openmuc.jdlms.interfaceclass.InterfaceClass;
import org.openmuc.jdlms.interfaceclass.attribute.ClockAttribute;
import org.osgp.adapter.protocol.dlms.domain.entities.DlmsDevice;
import org.osgp.adapter.protocol.dlms.domain.factories.DlmsConnectionHolder;
import org.osgp.adapter.protocol.dlms.exceptions.ConnectionException;
import org.osgp.adapter.protocol.dlms.exceptions.ProtocolAdapterException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alliander.osgp.dto.valueobjects.smartmetering.ActionResponseDto;
import com.alliander.osgp.dto.valueobjects.smartmetering.SetClockConfigurationRequestDto;

import ma.glasnost.orika.MapperFacade;

@Component
public class SetClockConfigurationCommandExecutor
        extends AbstractCommandExecutor<SetClockConfigurationRequestDto, Void> {

    private static final ObisCode LOGICAL_NAME = new ObisCode("0.0.1.0.0.255");

    private static final AttributeAddress ATTRIBUTE_TIME_ZONE = new AttributeAddress(InterfaceClass.CLOCK.id(),
            LOGICAL_NAME, ClockAttribute.TIME_ZONE.attributeId());

    private static final AttributeAddress ATTRIBUTE_DAYLIGHT_SAVINGS_BEGIN = new AttributeAddress(
            InterfaceClass.CLOCK.id(), LOGICAL_NAME, ClockAttribute.DAYLIGHT_SAVINGS_BEGIN.attributeId());

    private static final AttributeAddress ATTRIBUTE_DAYLIGHT_SAVINGS_END = new AttributeAddress(
            InterfaceClass.CLOCK.id(), LOGICAL_NAME, ClockAttribute.DAYLIGHT_SAVINGS_END.attributeId());

    private static final AttributeAddress ATTRIBUTE_DAYLIGHT_SAVINGS_DEVIATION = new AttributeAddress(
            InterfaceClass.CLOCK.id(), LOGICAL_NAME, ClockAttribute.DAYLIGHT_SAVINGS_DEVIATION.attributeId());

    private static final AttributeAddress ATTRIBUTE_DAYLIGHT_SAVINGS_ENABLED = new AttributeAddress(
            InterfaceClass.CLOCK.id(), LOGICAL_NAME, ClockAttribute.DAYLIGHT_SAVINGS_ENABLED.attributeId());

    @Autowired
    private MapperFacade configurationMapper;

    public SetClockConfigurationCommandExecutor() {
        super(SetClockConfigurationRequestDto.class);
    }

    @Override
    public ActionResponseDto asBundleResponse(final Void executionResult) throws ProtocolAdapterException {
        // Always successful, otherwise a ProtocolAdapterException was thrown
        // before.
        return new ActionResponseDto("Set clock configuration was successful");
    }

    @Override
    public Void execute(final DlmsConnectionHolder conn, final DlmsDevice device,
            final SetClockConfigurationRequestDto object) throws ProtocolAdapterException {

        try {
            this.dlmsLogWrite(conn, ATTRIBUTE_TIME_ZONE);
            this.writeAttribute(conn,
                    new SetParameter(ATTRIBUTE_TIME_ZONE, DataObject.newInteger16Data(object.getTimeZoneOffset())),
                    "Timezone");

            final CosemDateTime daylightSavingsBegin = this.configurationMapper.map(object.getDaylightSavingsBegin(),
                    CosemDateTime.class);
            this.dlmsLogWrite(conn, ATTRIBUTE_DAYLIGHT_SAVINGS_BEGIN);
            this.writeAttribute(conn, new SetParameter(ATTRIBUTE_DAYLIGHT_SAVINGS_BEGIN,
                    DataObject.newOctetStringData(daylightSavingsBegin.encode())), "Daylight savings begin");

            final CosemDateTime daylightSavingsEnd = this.configurationMapper.map(object.getDaylightSavingsEnd(),
                    CosemDateTime.class);
            this.dlmsLogWrite(conn, ATTRIBUTE_DAYLIGHT_SAVINGS_END);
            this.writeAttribute(conn, new SetParameter(ATTRIBUTE_DAYLIGHT_SAVINGS_END,
                    DataObject.newOctetStringData(daylightSavingsEnd.encode())), "Daylight savinds end");

            // Read value, if it's the same don't try to set it, to avoid
            // READ_WRITE access error because this attribute is read-only in
            // DSMR.
            final GetResult dstDeviationResult = conn.getConnection().get(ATTRIBUTE_DAYLIGHT_SAVINGS_DEVIATION);
            final byte currentDstDeviation = (byte) dstDeviationResult.getResultData().getValue();
            if (currentDstDeviation != object.getDaylightSavingsDeviation()) {
                this.dlmsLogWrite(conn, ATTRIBUTE_DAYLIGHT_SAVINGS_DEVIATION);
                this.writeAttribute(conn,
                        new SetParameter(ATTRIBUTE_DAYLIGHT_SAVINGS_DEVIATION,
                                DataObject.newInteger8Data(object.getDaylightSavingsDeviation())),
                        "Daylight savings deviation");
            }

            this.dlmsLogWrite(conn, ATTRIBUTE_DAYLIGHT_SAVINGS_ENABLED);
            this.writeAttribute(conn, new SetParameter(ATTRIBUTE_DAYLIGHT_SAVINGS_ENABLED,
                    DataObject.newBoolData(object.isDaylightSavingsEnabled())), "Daylight savings enabled");
        } catch (final IOException e) {
            throw new ConnectionException(e);
        }
        return null;
    }

    private void writeAttribute(final DlmsConnectionHolder conn, final SetParameter parameter,
            final String attributeName) throws ProtocolAdapterException {
        try {
            final AccessResultCode result = conn.getConnection().set(parameter);
            if (!result.equals(AccessResultCode.SUCCESS)) {
                throw new ProtocolAdapterException(String
                        .format("Attribute '%s' of the clock configuration could be set successfully.", attributeName));
            }

        } catch (final IOException e) {
            throw new ConnectionException(e);
        }
    }

    private void dlmsLogWrite(final DlmsConnectionHolder conn, final AttributeAddress attribute) {
        conn.getDlmsMessageListener().setDescription("SetClockConfiguration, preparing to write attribute: "
                + JdlmsObjectToStringUtil.describeAttributes(attribute));
    }
}
