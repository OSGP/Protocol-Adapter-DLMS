/**
 * Copyright 2015 Smart Society Services B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.osgp.adapter.protocol.dlms.domain.commands;

import java.util.List;

import org.openmuc.jdlms.AttributeAddress;
import org.openmuc.jdlms.ObisCode;
import org.openmuc.jdlms.datatypes.DataObject;
import org.osgp.adapter.protocol.dlms.exceptions.ProtocolAdapterException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.alliander.osgp.dto.valueobjects.smartmetering.ChannelDto;
import com.alliander.osgp.dto.valueobjects.smartmetering.ChannelQueryDto;
import com.alliander.osgp.dto.valueobjects.smartmetering.DlmsUnitDto;
import com.alliander.osgp.dto.valueobjects.smartmetering.ScalerUnitDto;
import com.alliander.osgp.dto.valueobjects.smartmetering.ScalerUnitResponseDto;

/**
 * baseclass meant to simplify implementing the retrieval of scaler and unit
 * together with values. Take a look at the junit integration test for an
 * example how to use this.
 *
 * @param <R>
 */
public abstract class AbstractMeterReadsScalerUnitCommandExecutor<T extends ChannelQueryDto, R extends ScalerUnitResponseDto>
        implements ScalerUnitAwareCommandExecutor<T, R> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractMeterReadsScalerUnitCommandExecutor.class);

    private static final ObisCode REGISTER_FOR_SCALER_UNIT = new ObisCode("1.0.1.8.0.255");
    private static final int CLASS_ID = 3;
    private static final int CLASS_ID_MBUS = 4;
    private static final byte ATTRIBUTE_ID_SCALER_UNIT = 3;
    private static final ObisCode OBIS_CODE_MBUS_MASTER_SCALER_UNIT_1 = new ObisCode("0.1.24.2.1.255");
    private static final ObisCode OBIS_CODE_MBUS_MASTER_SCALER_UNIT_2 = new ObisCode("0.2.24.2.1.255");
    private static final ObisCode OBIS_CODE_MBUS_MASTER_SCALER_UNIT_3 = new ObisCode("0.3.24.2.1.255");
    private static final ObisCode OBIS_CODE_MBUS_MASTER_SCALER_UNIT_4 = new ObisCode("0.4.24.2.1.255");

    @Autowired
    private DlmsHelperService dlmsHelperService;

    private ObisCode registerForScalerUnit(final int channel) throws ProtocolAdapterException {
        switch (channel) {
        case 1:
            return OBIS_CODE_MBUS_MASTER_SCALER_UNIT_1;
        case 2:
            return OBIS_CODE_MBUS_MASTER_SCALER_UNIT_2;
        case 3:
            return OBIS_CODE_MBUS_MASTER_SCALER_UNIT_3;
        case 4:
            return OBIS_CODE_MBUS_MASTER_SCALER_UNIT_4;
        default:
            throw new ProtocolAdapterException(String.format("channel %s not supported", channel));
        }
    }

    @Override
    public ScalerUnitDto convert(final DataObject dataObject) throws ProtocolAdapterException {
        LOGGER.debug(this.dlmsHelperService.getDebugInfo(dataObject));
        if (!dataObject.isComplex()) {
            throw new ProtocolAdapterException("complex data (structure) expected while retrieving scaler and unit."
                    + this.dlmsHelperService.getDebugInfo(dataObject));
        }
        final List<DataObject> value = dataObject.value();
        if (value.size() != 2) {
            throw new ProtocolAdapterException("expected 2 values while retrieving scaler and unit."
                    + this.dlmsHelperService.getDebugInfo(dataObject));
        }
        final DataObject scaler = value.get(0);
        final DataObject unit = value.get(1);

        return new ScalerUnitDto(DlmsUnitDto.fromDlmsEnum(this.dlmsHelperService.readLongNotNull(unit, "unit value")
                .intValue()), this.dlmsHelperService.readLongNotNull(scaler, "scaler value").intValue());
    }

    @Override
    public AttributeAddress getScalerUnitAttributeAddress(final T channelQuery) throws ProtocolAdapterException {
        final ObisCode obisCodeRegister = channelQuery.getChannel().equals(ChannelDto.NONE) ? REGISTER_FOR_SCALER_UNIT
                : this.registerForScalerUnit(channelQuery.getChannel().getChannelNumber());

        return channelQuery.getChannel().equals(ChannelDto.NONE) ? new AttributeAddress(CLASS_ID, obisCodeRegister,
                ATTRIBUTE_ID_SCALER_UNIT) : new AttributeAddress(CLASS_ID_MBUS, obisCodeRegister,
                        ATTRIBUTE_ID_SCALER_UNIT);
    }

}
