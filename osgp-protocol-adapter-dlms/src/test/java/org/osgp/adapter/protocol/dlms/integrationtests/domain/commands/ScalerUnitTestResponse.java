package org.osgp.adapter.protocol.dlms.integrationtests.domain.commands;

import com.alliander.osgp.dto.valueobjects.smartmetering.ScalerUnitDto;
import com.alliander.osgp.dto.valueobjects.smartmetering.ScalerUnitResponseDto;

public class ScalerUnitTestResponse implements ScalerUnitResponseDto {

    private final ScalerUnitDto scalerUnit;

    public ScalerUnitTestResponse(final ScalerUnitDto scalerUnit) {
        this.scalerUnit = scalerUnit;
    }

    @Override
    public ScalerUnitDto getScalerUnit() {
        return this.scalerUnit;
    }

}
