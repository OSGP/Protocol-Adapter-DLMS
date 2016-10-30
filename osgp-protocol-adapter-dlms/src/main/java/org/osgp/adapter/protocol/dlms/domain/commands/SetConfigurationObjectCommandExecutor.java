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
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeoutException;

import org.openmuc.jdlms.AccessResultCode;
import org.openmuc.jdlms.AttributeAddress;
import org.openmuc.jdlms.GetResult;
import org.openmuc.jdlms.ObisCode;
import org.openmuc.jdlms.SetParameter;
import org.openmuc.jdlms.datatypes.BitString;
import org.openmuc.jdlms.datatypes.DataObject;
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
import com.alliander.osgp.dto.valueobjects.smartmetering.ConfigurationFlagDto;
import com.alliander.osgp.dto.valueobjects.smartmetering.ConfigurationFlagTypeDto;
import com.alliander.osgp.dto.valueobjects.smartmetering.ConfigurationFlagsDto;
import com.alliander.osgp.dto.valueobjects.smartmetering.ConfigurationObjectDto;
import com.alliander.osgp.dto.valueobjects.smartmetering.GprsOperationModeTypeDto;
import com.alliander.osgp.dto.valueobjects.smartmetering.SetConfigurationObjectRequestDataDto;

@Component()
public class SetConfigurationObjectCommandExecutor extends
        AbstractCommandExecutor<ConfigurationObjectDto, AccessResultCode> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SetConfigurationObjectCommandExecutor.class);

    private static final int CLASS_ID = 1;
    private static final ObisCode OBIS_CODE = new ObisCode("0.1.94.31.3.255");
    private static final int ATTRIBUTE_ID = 2;

    private static final List<ConfigurationFlagTypeDto> FLAGS_TYPES_FORBIDDEN_TO_SET = new ArrayList<>();
    static {
        FLAGS_TYPES_FORBIDDEN_TO_SET.add(ConfigurationFlagTypeDto.PO_ENABLE);
        FLAGS_TYPES_FORBIDDEN_TO_SET.add(ConfigurationFlagTypeDto.HLS_3_ON_P_3_ENABLE);
        FLAGS_TYPES_FORBIDDEN_TO_SET.add(ConfigurationFlagTypeDto.HLS_4_ON_P_3_ENABLE);
        FLAGS_TYPES_FORBIDDEN_TO_SET.add(ConfigurationFlagTypeDto.HLS_5_ON_P_3_ENABLE);
        FLAGS_TYPES_FORBIDDEN_TO_SET.add(ConfigurationFlagTypeDto.HLS_3_ON_PO_ENABLE);
        FLAGS_TYPES_FORBIDDEN_TO_SET.add(ConfigurationFlagTypeDto.HLS_4_ON_PO_ENABLE);
        FLAGS_TYPES_FORBIDDEN_TO_SET.add(ConfigurationFlagTypeDto.HLS_5_ON_PO_ENABLE);
    }

    @Autowired
    private ConfigurationObjectHelperService configurationObjectHelperService;

    @Autowired
    private DlmsHelperService dlmsHelperService;

    public SetConfigurationObjectCommandExecutor() {
        super(SetConfigurationObjectRequestDataDto.class);
    }

    @Override
    public ConfigurationObjectDto fromBundleRequestInput(final ActionRequestDto bundleInput)
            throws ProtocolAdapterException {

        this.checkActionRequestType(bundleInput);
        final SetConfigurationObjectRequestDataDto setConfigurationObjectRequestDataDto = (SetConfigurationObjectRequestDataDto) bundleInput;

        return setConfigurationObjectRequestDataDto.getConfigurationObject();
    }

    @Override
    public ActionResponseDto asBundleResponse(final AccessResultCode executionResult) throws ProtocolAdapterException {

        this.checkAccessResultCode(executionResult);

        return new ActionResponseDto("Set configuration object was successful");
    }

    @Override
    public AccessResultCode execute(final DlmsConnectionHolder conn, final DlmsDevice device,
            final ConfigurationObjectDto configurationObject) throws ProtocolAdapterException {

        try {
            final ConfigurationObjectDto configurationObjectOnDevice = this.retrieveConfigurationObject(conn);

            final SetParameter setParameter = this.buildSetParameter(configurationObject, configurationObjectOnDevice);

            conn.getDlmsMessageListener().setDescription("SetConfigurationObject, set attribute: "
                    + JdlmsObjectToStringUtil.describeAttributes(new AttributeAddress(CLASS_ID, OBIS_CODE, ATTRIBUTE_ID)));

            return conn.getConnection().set(setParameter);
        } catch (IOException | TimeoutException e) {
            throw new ConnectionException(e);
        }
    }

    private SetParameter buildSetParameter(final ConfigurationObjectDto configurationObject,
            final ConfigurationObjectDto configurationObjectOnDevice) {

        final AttributeAddress configurationObjectValue = new AttributeAddress(CLASS_ID, OBIS_CODE, ATTRIBUTE_ID);
        final DataObject complexData = this.buildSetParameterData(configurationObject, configurationObjectOnDevice);
        LOGGER.info("Configuration object complex data: {}", this.dlmsHelperService.getDebugInfo(complexData));

        return new SetParameter(configurationObjectValue, complexData);
    }

    private DataObject buildSetParameterData(final ConfigurationObjectDto configurationObject,
            final ConfigurationObjectDto configurationObjectOnDevice) {

        final List<DataObject> linkedList = new LinkedList<>();
        if (GprsOperationModeTypeDto.ALWAYS_ON.equals(configurationObject.getGprsOperationMode())) {
            linkedList.add(DataObject.newEnumerateData(1));
        } else if (GprsOperationModeTypeDto.TRIGGERED.equals(configurationObject.getGprsOperationMode())) {
            linkedList.add(DataObject.newEnumerateData(0));
        } else {
            // copy from meter if there is a set gprsoperationmode
            if (configurationObjectOnDevice.getGprsOperationMode() != null) {
                linkedList.add(DataObject
                        .newEnumerateData(configurationObjectOnDevice.getGprsOperationMode().ordinal()));
            }
        }

        final BitString bitString = this.getMergedFlags(configurationObject, configurationObjectOnDevice);
        final DataObject newBitStringData = DataObject.newBitStringData(bitString);
        linkedList.add(newBitStringData);

        return DataObject.newStructureData(linkedList);
    }

    /*
     * Merging flags to a new list of flags is done according the rule of (1) a
     * new flag setting in the request, overrules existing flag setting on the
     * meter (2) flag settings not present in the request are copied from the
     * flag settings on the meter
     */
    private BitString getMergedFlags(final ConfigurationObjectDto configurationObject,
            final ConfigurationObjectDto configurationObjectOnDevice) {
        final List<ConfigurationFlagDto> configurationFlags = this.getNewFlags(configurationObject);
        this.mergeOldFlags(configurationObjectOnDevice, configurationFlags);

        final byte[] newConfigurationObjectFlagsByteArray = this.configurationObjectHelperService
                .toByteArray(configurationFlags);

        return new BitString(newConfigurationObjectFlagsByteArray, 16);
    }

    private void mergeOldFlags(final ConfigurationObjectDto configurationObjectOnDevice,
            final List<ConfigurationFlagDto> configurationFlags) {
        if (configurationObjectOnDevice != null) {
            for (final ConfigurationFlagDto configurationFlagOnDevice : configurationObjectOnDevice
                    .getConfigurationFlags().getConfigurationFlag()) {
                final ConfigurationFlagDto configurationFlag = this.getConfigurationFlag(configurationFlags,
                        configurationFlagOnDevice.getConfigurationFlagType());
                if (configurationFlag == null) {
                    configurationFlags.add(configurationFlagOnDevice);
                }
            }
        }
    }

    private List<ConfigurationFlagDto> getNewFlags(final ConfigurationObjectDto configurationObject) {
        final List<ConfigurationFlagDto> configurationFlags = new ArrayList<>();
        for (final ConfigurationFlagDto configurationFlag : configurationObject.getConfigurationFlags()
                .getConfigurationFlag()) {
            if (!this.isForbidden(configurationFlag.getConfigurationFlagType())) {
                configurationFlags.add(configurationFlag);
            }
        }
        return configurationFlags;
    }

    /**
     * Check if the configuratioFlag is forbidden. Check is done against the
     * list of forbidden flag types
     *
     * @param configurationFlag
     *            the flag to check
     * @return true if the flag is forbidden, else false
     */
    private boolean isForbidden(final ConfigurationFlagTypeDto configurationFlagType) {
        for (final ConfigurationFlagTypeDto forbiddenFlagType : FLAGS_TYPES_FORBIDDEN_TO_SET) {
            if (forbiddenFlagType.equals(configurationFlagType)) {
                return true;
            }
        }
        return false;
    }

    private ConfigurationFlagDto getConfigurationFlag(final Collection<ConfigurationFlagDto> flags,
            final ConfigurationFlagTypeDto flagType) {
        for (final ConfigurationFlagDto configurationFlag : flags) {
            if (configurationFlag.getConfigurationFlagType().equals(flagType)) {
                return configurationFlag;
            }
        }
        return null;
    }

    private ConfigurationObjectDto retrieveConfigurationObject(final DlmsConnectionHolder conn)
            throws IOException, TimeoutException, ProtocolAdapterException {

        final AttributeAddress configurationObjectValue = new AttributeAddress(CLASS_ID, OBIS_CODE, ATTRIBUTE_ID);

        conn.getDlmsMessageListener()
                .setDescription("SetConfigurationObject retrieve current value, retrieve attribute: "
                        + JdlmsObjectToStringUtil.describeAttributes(configurationObjectValue));

        LOGGER.info(
                "Retrieving current configuration object by issuing get request for class id: {}, obis code: {}, attribute id: {}",
                CLASS_ID, OBIS_CODE, ATTRIBUTE_ID);
        final GetResult getResult = conn.getConnection().get(configurationObjectValue);

        if (getResult == null) {
            throw new ProtocolAdapterException("No result received while retrieving current configuration object.");
        }

        return this.getConfigurationObject(getResult);
    }

    private ConfigurationObjectDto getConfigurationObject(final GetResult result) throws ProtocolAdapterException {

        final DataObject resultData = result.getResultData();
        LOGGER.info("Configuration object current complex data: {}", this.dlmsHelperService.getDebugInfo(resultData));

        final List<DataObject> linkedList = resultData.getValue();

        if (linkedList == null || linkedList.isEmpty()) {
            throw new ProtocolAdapterException(
                    "Expected data in result while retrieving current configuration object, but got nothing");
        }

        // get gprsOperationMode and configurationFlags from List<DataObject>
        // linkedList, use them to build a ConfigurationObjectDto and return
        // that.
        final GprsOperationModeTypeDto gprsOperationMode = this.getGprsOperationModeType(linkedList);
        final ConfigurationFlagsDto configurationFlags = this.getConfigurationFlags(linkedList, resultData);
        return new ConfigurationObjectDto(gprsOperationMode, configurationFlags);
    }

    private GprsOperationModeTypeDto getGprsOperationModeType(final List<DataObject> linkedList)
            throws ProtocolAdapterException {

        final DataObject gprsOperationModeData = linkedList.get(0);
        if (gprsOperationModeData == null) {
            throw new ProtocolAdapterException(
                    "Expected Gprs operation mode data in result while retrieving current configuration object, but got nothing");
        }
        GprsOperationModeTypeDto gprsOperationMode = null;
        if (((Number) gprsOperationModeData.getValue()).longValue() == 1) {
            gprsOperationMode = GprsOperationModeTypeDto.ALWAYS_ON;
        } else if (((Number) gprsOperationModeData.getValue()).longValue() == 2) {
            gprsOperationMode = GprsOperationModeTypeDto.TRIGGERED;
        }

        return gprsOperationMode;
    }

    private ConfigurationFlagsDto getConfigurationFlags(final List<DataObject> linkedList, final DataObject resultData)
            throws ProtocolAdapterException {
        final DataObject flagsData = linkedList.get(1);

        if (flagsData == null) {
            throw new ProtocolAdapterException(
                    "Expected flag bit data in result while retrieving current configuration object, but got nothing");
        }
        if (!(flagsData.getValue() instanceof BitString)) {
            throw new ProtocolAdapterException("Value in DataObject is not a BitString: "
                    + resultData.getValue().getClass().getName());
        }
        final byte[] flagByteArray = ((BitString) flagsData.getValue()).bitString();

        final List<ConfigurationFlagDto> listConfigurationFlag = this.configurationObjectHelperService
                .toConfigurationFlags(flagByteArray);

        return new ConfigurationFlagsDto(listConfigurationFlag);
    }
}
