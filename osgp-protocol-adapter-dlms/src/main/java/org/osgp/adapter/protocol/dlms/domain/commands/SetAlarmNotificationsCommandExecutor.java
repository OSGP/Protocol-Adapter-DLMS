/**
 * Copyright 2015 Smart Society Services B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.osgp.adapter.protocol.dlms.domain.commands;

import java.io.IOException;
import java.util.BitSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeoutException;

import org.openmuc.jdlms.AccessResultCode;
import org.openmuc.jdlms.AttributeAddress;
import org.openmuc.jdlms.GetResult;
import org.openmuc.jdlms.ObisCode;
import org.openmuc.jdlms.SetParameter;
import org.openmuc.jdlms.datatypes.DataObject;
import org.osgp.adapter.protocol.dlms.domain.entities.DlmsDevice;
import org.osgp.adapter.protocol.dlms.domain.factories.DlmsConnectionHolder;
import org.osgp.adapter.protocol.dlms.exceptions.ConnectionException;
import org.osgp.adapter.protocol.dlms.exceptions.ProtocolAdapterException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.alliander.osgp.dto.valueobjects.smartmetering.ActionRequestDto;
import com.alliander.osgp.dto.valueobjects.smartmetering.ActionResponseDto;
import com.alliander.osgp.dto.valueobjects.smartmetering.AlarmNotificationDto;
import com.alliander.osgp.dto.valueobjects.smartmetering.AlarmNotificationsDto;
import com.alliander.osgp.dto.valueobjects.smartmetering.AlarmTypeDto;
import com.alliander.osgp.dto.valueobjects.smartmetering.SetAlarmNotificationsRequestDto;

@Component()
public class SetAlarmNotificationsCommandExecutor extends
        AbstractCommandExecutor<AlarmNotificationsDto, AccessResultCode> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SetAlarmNotificationsCommandExecutor.class);

    private static final int CLASS_ID = 1;
    private static final ObisCode OBIS_CODE = new ObisCode("0.0.97.98.10.255");
    private static final int ATTRIBUTE_ID = 2;

    private static final int NUMBER_OF_BITS_IN_ALARM_FILTER = 32;

    private final AlarmHelperService alarmHelperService = new AlarmHelperService();

    public SetAlarmNotificationsCommandExecutor() {
        super(SetAlarmNotificationsRequestDto.class);
    }

    @Override
    public AlarmNotificationsDto fromBundleRequestInput(final ActionRequestDto bundleInput)
            throws ProtocolAdapterException {

        this.checkActionRequestType(bundleInput);
        final SetAlarmNotificationsRequestDto setAlarmNotificationsRequestDto = (SetAlarmNotificationsRequestDto) bundleInput;

        return setAlarmNotificationsRequestDto.getAlarmNotifications();
    }

    @Override
    public ActionResponseDto asBundleResponse(final AccessResultCode executionResult) throws ProtocolAdapterException {

        this.checkAccessResultCode(executionResult);

        return new ActionResponseDto("Set alarm notifications was successful");
    }

    @Override
    public AccessResultCode execute(final DlmsConnectionHolder conn, final DlmsDevice device,
            final AlarmNotificationsDto alarmNotifications) throws ProtocolAdapterException {

        try {
            final AlarmNotificationsDto alarmNotificationsOnDevice = this.retrieveCurrentAlarmNotifications(conn);

            LOGGER.info("Alarm Filter on device before setting notifications: {}", alarmNotificationsOnDevice);

            final long alarmFilterLongValue = this.calculateAlarmFilterLongValue(alarmNotificationsOnDevice,
                    alarmNotifications);

            LOGGER.info("Modified Alarm Filter long value for device: {}", alarmFilterLongValue);

            return this.writeUpdatedAlarmNotifications(conn, alarmFilterLongValue);
        } catch (IOException | TimeoutException e) {
            throw new ConnectionException(e);
        }
    }

    public AlarmNotificationsDto retrieveCurrentAlarmNotifications(final DlmsConnectionHolder conn)
            throws IOException, TimeoutException, ProtocolAdapterException {

        final AttributeAddress alarmFilterValue = new AttributeAddress(CLASS_ID, OBIS_CODE, ATTRIBUTE_ID);

        conn.getDlmsMessageListener()
        .setDescription("SetAlarmNotifications retrieve current value, retrieve attribute: "
                + JdlmsObjectToStringUtil.describeAttributes(alarmFilterValue));

        LOGGER.info(
                "Retrieving current alarm filter by issuing get request for class id: {}, obis code: {}, attribute id: {}",
                CLASS_ID, OBIS_CODE, ATTRIBUTE_ID);
        final GetResult getResult = conn.getConnection().get(alarmFilterValue);

        if (getResult == null) {
            throw new ProtocolAdapterException("No GetResult received while retrieving current alarm filter.");
        }

        return this.alarmNotifications(getResult.getResultData());
    }

    public AccessResultCode writeUpdatedAlarmNotifications(final DlmsConnectionHolder conn,
            final long alarmFilterLongValue) throws IOException, TimeoutException {

        final AttributeAddress alarmFilterValue = new AttributeAddress(CLASS_ID, OBIS_CODE, ATTRIBUTE_ID);
        final DataObject value = DataObject.newUInteger32Data(alarmFilterLongValue);

        final SetParameter setParameter = new SetParameter(alarmFilterValue, value);

        conn.getDlmsMessageListener().setDescription("SetAlarmNotifications write updated value " + alarmFilterLongValue
                + ", set attribute: " + JdlmsObjectToStringUtil.describeAttributes(alarmFilterValue));

        return conn.getConnection().set(setParameter);
    }

    public AlarmNotificationsDto alarmNotifications(final DataObject alarmFilter) throws ProtocolAdapterException {

        if (alarmFilter == null) {
            throw new ProtocolAdapterException("DataObject expected to contain an alarm filter is null.");
        }

        if (!alarmFilter.isNumber()) {
            throw new ProtocolAdapterException("DataObject isNumber is expected to be true for alarm notifications.");
        }

        if (!(alarmFilter.getValue() instanceof Number)) {
            throw new ProtocolAdapterException("Value in DataObject is not a java.lang.Number: "
                    + alarmFilter.getValue().getClass().getName());
        }

        return this.alarmNotifications(((Number) alarmFilter.getValue()).longValue());

    }

    public long calculateAlarmFilterLongValue(final AlarmNotificationsDto alarmNotificationsOnDevice,
            final AlarmNotificationsDto alarmNotificationsToSet) {

        /*
         * Create a new (modifiable) set of alarm notifications, based on the
         * notifications to set.
         *
         * Next, add all notifications on the device. These will only really be
         * added to the new set of notifications if it did not contain a
         * notification for the alarm type for which the notification is added.
         *
         * This works because of the specification of addAll for the set,
         * claiming elements will only be added if not already present, and the
         * definition of equals on the AlarmNotification, ensuring only a single
         * setting per AlarmType.
         */

        final Set<AlarmNotificationDto> notificationsToSet = new TreeSet<>(
                alarmNotificationsToSet.getAlarmNotificationsSet());

        notificationsToSet.addAll(alarmNotificationsOnDevice.getAlarmNotificationsSet());

        return this.alarmFilterLongValue(new AlarmNotificationsDto(notificationsToSet));
    }

    public AlarmNotificationsDto alarmNotifications(final long alarmFilterLongValue) {

        final BitSet bitSet = BitSet.valueOf(new long[] { alarmFilterLongValue });
        final Set<AlarmNotificationDto> notifications = new TreeSet<>();

        final AlarmTypeDto[] alarmTypes = AlarmTypeDto.values();
        for (final AlarmTypeDto alarmType : alarmTypes) {
            final boolean enabled = bitSet.get(this.alarmHelperService.getAlarmRegisterBitIndexPerAlarmType().get(
                    alarmType));
            notifications.add(new AlarmNotificationDto(alarmType, enabled));
        }

        return new AlarmNotificationsDto(notifications);
    }

    public long alarmFilterLongValue(final AlarmNotificationsDto alarmNotifications) {

        final BitSet bitSet = new BitSet(NUMBER_OF_BITS_IN_ALARM_FILTER);
        final Set<AlarmNotificationDto> notifications = alarmNotifications.getAlarmNotificationsSet();
        for (final AlarmNotificationDto alarmNotification : notifications) {
            bitSet.set(
                    this.alarmHelperService.getAlarmRegisterBitIndexPerAlarmType()
                    .get(alarmNotification.getAlarmType()), alarmNotification.isEnabled());
        }

        return bitSet.toLongArray()[0];
    }

}
