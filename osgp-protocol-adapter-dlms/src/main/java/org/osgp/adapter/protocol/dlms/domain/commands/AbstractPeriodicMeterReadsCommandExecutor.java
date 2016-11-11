/**
 * Copyright 2016 Smart Society Services B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.osgp.adapter.protocol.dlms.domain.commands;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.osgp.adapter.protocol.dlms.exceptions.BufferedDateTimeValidationException;

import com.alliander.osgp.dto.valueobjects.smartmetering.CosemDateTimeDto;
import com.alliander.osgp.dto.valueobjects.smartmetering.PeriodicMeterReadsRequestDataDto;

public abstract class AbstractPeriodicMeterReadsCommandExecutor<T, R> extends AbstractCommandExecutor<T, R> {

    public AbstractPeriodicMeterReadsCommandExecutor(Class<? extends PeriodicMeterReadsRequestDataDto> clazz) {
        super(clazz);
    }

    protected void validateBufferedDateTime(final DateTime bufferedDateTime, final CosemDateTimeDto cosemDateTime,
            final DateTime beginDateTime, final DateTime endDateTime) throws BufferedDateTimeValidationException {

        if (bufferedDateTime == null) {
            final DateTimeFormatter dtf = ISODateTimeFormat.dateTime();
            throw new BufferedDateTimeValidationException("Not using an object from capture buffer (clock="
                    + cosemDateTime
                    + "), because the date does not match the given period, since it is not fully specified: ["
                    + dtf.print(beginDateTime) + " .. " + dtf.print(endDateTime) + "].");
        }
        if (bufferedDateTime.isBefore(beginDateTime) || bufferedDateTime.isAfter(endDateTime)) {
            final DateTimeFormatter dtf = ISODateTimeFormat.dateTime();
            throw new BufferedDateTimeValidationException("Not using an object from capture buffer (clock="
                    + dtf.print(bufferedDateTime) + "), because the date does not match the given period: ["
                    + dtf.print(beginDateTime) + " .. " + dtf.print(endDateTime) + "].");
        }
    }
}
