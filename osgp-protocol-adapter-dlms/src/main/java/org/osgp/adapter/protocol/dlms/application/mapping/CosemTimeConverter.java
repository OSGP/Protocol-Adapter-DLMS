package org.osgp.adapter.protocol.dlms.application.mapping;

import ma.glasnost.orika.converter.BidirectionalConverter;
import ma.glasnost.orika.metadata.Type;

import org.openmuc.jdlms.datatypes.CosemDateFormat;

import com.alliander.osgp.dto.valueobjects.smartmetering.CosemTimeDto;

public class CosemTimeConverter extends BidirectionalConverter<CosemTimeDto, org.openmuc.jdlms.datatypes.CosemTime> {

    @Override
    public org.openmuc.jdlms.datatypes.CosemTime convertTo(final CosemTimeDto source,
            final Type<org.openmuc.jdlms.datatypes.CosemTime> destinationType) {

        return new org.openmuc.jdlms.datatypes.CosemTime(source.getHour(), source.getMinute(), source.getSecond(),
                source.getHundredths());
    }

    @Override
    public CosemTimeDto convertFrom(final org.openmuc.jdlms.datatypes.CosemTime source,
            final Type<CosemTimeDto> destinationType) {
        if (source == null) {
            return null;
        }

        final int hour = source.valueFor(CosemDateFormat.Field.HOUR);
        final int minute = source.valueFor(CosemDateFormat.Field.MINUTE);
        final int second = source.valueFor(CosemDateFormat.Field.SECOND);
        final int hundredths = source.valueFor(CosemDateFormat.Field.HUNDREDTHS);
        return new com.alliander.osgp.dto.valueobjects.smartmetering.CosemTimeDto(hour, minute, second, hundredths);
    }

}
