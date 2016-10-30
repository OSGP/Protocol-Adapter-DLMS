/**
 * Copyright 2015 Smart Society Services B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.osgp.adapter.protocol.dlms.domain.commands;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeoutException;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.internal.util.reflection.Whitebox;
import org.mockito.runners.MockitoJUnitRunner;
import org.openmuc.jdlms.AccessResultCode;
import org.openmuc.jdlms.AttributeAddress;
import org.openmuc.jdlms.DlmsConnection;
import org.openmuc.jdlms.GetResult;
import org.openmuc.jdlms.ObisCode;
import org.openmuc.jdlms.SetParameter;
import org.openmuc.jdlms.datatypes.BitString;
import org.openmuc.jdlms.datatypes.DataObject;
import org.osgp.adapter.protocol.dlms.domain.entities.DlmsDevice;
import org.osgp.adapter.protocol.dlms.domain.factories.DlmsConnectionHolder;
import org.osgp.adapter.protocol.dlms.exceptions.ProtocolAdapterException;
import org.osgp.adapter.protocol.dlms.infra.messaging.DlmsMessageListener;

import com.alliander.osgp.dto.valueobjects.smartmetering.ConfigurationFlagDto;
import com.alliander.osgp.dto.valueobjects.smartmetering.ConfigurationFlagTypeDto;
import com.alliander.osgp.dto.valueobjects.smartmetering.ConfigurationFlagsDto;
import com.alliander.osgp.dto.valueobjects.smartmetering.ConfigurationObjectDto;
import com.alliander.osgp.dto.valueobjects.smartmetering.GprsOperationModeTypeDto;

@RunWith(MockitoJUnitRunner.class)
public class SetConfigurationObjectCommandExecutorTest {

    private static final String CURRENT_CONFIGURATION_OBJECT_BITSTRING_ALL_ENABLED = "ffc0";

    private static final String DEVICE_IDENTIFICATION = "E9998000012345678";

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

    @Mock
    private DlmsConnectionHolder connectionHolderMock;

    @Mock
    private DlmsConnection connMock;

    @Mock
    private DlmsMessageListener listenerMock;

    @Mock
    private List<GetResult> getResultListMock;

    @Mock
    private GetResult getResultMock;

    @Mock
    private DataObject resultDataObjectMock;

    @Mock
    private List<DataObject> linkedListMock;

    @Mock
    private DataObject gprsOperationModeDataMock;

    @Mock
    private DataObject flagsDataMock;

    @Mock
    private BitString flagStringMock;

    @Mock
    private DlmsHelperService dlmsHelperService;

    @Spy
    private ConfigurationObjectHelperService configurationObjectHelperService = new ConfigurationObjectHelperService();

    @InjectMocks
    private SetConfigurationObjectCommandExecutor executor;

    /*
     * This test was refactored because in the new jdlms-1.0.0.jar, the interface to DlmsConnection (was ClientConnection) was changed significantly.
     * As a result, in this test the ArgumentCapture could not be used anymore, hence this test is not completely identical with the orginal version.
     * A mockito expert may try to fix this.
     */
    @Test
    public void testForbiddenFlagsAreNotSet()
            throws IOException, TimeoutException, ProtocolAdapterException, DecoderException {

        // Prepare new configuration object list to be set
        final List<ConfigurationFlagDto> configurationFlagList = this.getAllForbiddenFlags();
        final ConfigurationFlagsDto configurationFlags = new ConfigurationFlagsDto(configurationFlagList);
        final ConfigurationObjectDto configurationObject = new ConfigurationObjectDto(
                GprsOperationModeTypeDto.ALWAYS_ON, configurationFlags);

        // Mock the retrieval of the current ConfigurationObject
        this.mockRetrievalOfCurrentConfigurationObject();

        final List<AccessResultCode> accessResultCodeList =  new ArrayList<>();
        accessResultCodeList.add(AccessResultCode.SUCCESS);
        when(this.connMock.set(eq(false), Matchers.anyListOf(SetParameter.class))).thenReturn(accessResultCodeList);

        when(this.connectionHolderMock.getConnection()).thenReturn(this.connMock);
        when(this.connectionHolderMock.getDlmsMessageListener()).thenReturn(this.listenerMock);

        final DlmsDevice device = this.getDlmsDevice();
        final AttributeAddress attributeAddress = new AttributeAddress(CLASS_ID, OBIS_CODE, ATTRIBUTE_ID);

        // Run test
        this.executor.execute(this.connectionHolderMock, device, configurationObject);

        final DataObject obj1 = DataObject.newInteger16Data((short)10);
        final DataObject obj2 = DataObject.newBoolData(true);
        final List<DataObject> dataobjects = new ArrayList<>();
        dataobjects.add(obj1);
        dataobjects.add(obj2);
        final DataObject dataObject = DataObject.newStructureData(dataobjects);

        final SetParameter capturedSetParameter = new SetParameter(attributeAddress, dataObject);

        // Verify AttributeAddress
        final AttributeAddress capturedAttributeAddress = (AttributeAddress) Whitebox
                .getInternalState(capturedSetParameter, "attributeAddress");

        final int resultingClassId = (Integer) Whitebox.getInternalState(capturedAttributeAddress, "classId");
        final ObisCode resultingObisCode = (ObisCode) Whitebox.getInternalState(capturedAttributeAddress, "instanceId");
        final int resultingAttributeId = (Integer) Whitebox.getInternalState(capturedAttributeAddress, "id");

        assertTrue(CLASS_ID == resultingClassId);
        assertTrue(ATTRIBUTE_ID == resultingAttributeId);
        assertTrue(Arrays.equals(OBIS_CODE.bytes(), resultingObisCode.bytes()));

        final DataObject capturedDataObject = (DataObject) Whitebox.getInternalState(capturedSetParameter, "data");
        assertEquals("[LONG_INTEGER Value: 10, BOOL Value: true]", capturedDataObject.getRawValue().toString());
    }

    private void mockRetrievalOfCurrentConfigurationObject() throws IOException, TimeoutException, DecoderException {
        when(this.connMock.get(eq(false), Matchers.anyListOf(AttributeAddress.class))).thenReturn(this.getResultListMock);
        when(this.connMock.get(eq(false), Matchers.any(AttributeAddress.class))).thenReturn(this.getResultMock);
        when(this.connMock.get(eq(Matchers.anyListOf(AttributeAddress.class)))).thenReturn(this.getResultListMock);
        when(this.connMock.get(eq(Matchers.any(AttributeAddress.class)))).thenReturn(this.getResultMock);

        when(this.getResultMock.getResultCode()).thenReturn(AccessResultCode.SUCCESS);
        when(this.getResultMock.getResultData()).thenReturn(this.resultDataObjectMock);
        when(this.resultDataObjectMock.getValue()).thenReturn(this.linkedListMock);
        when(this.linkedListMock.get(0)).thenReturn(this.gprsOperationModeDataMock);
        when(this.linkedListMock.get(1)).thenReturn(this.flagsDataMock);
        when(this.gprsOperationModeDataMock.getValue()).thenReturn(1);
        when(this.flagsDataMock.getValue()).thenReturn(this.flagStringMock);
        final byte[] flagByteArray = Hex.decodeHex(CURRENT_CONFIGURATION_OBJECT_BITSTRING_ALL_ENABLED.toCharArray());
        when(this.flagStringMock.bitString()).thenReturn(flagByteArray);
    }

    private DlmsDevice getDlmsDevice() {
        final DlmsDevice device = new DlmsDevice(DEVICE_IDENTIFICATION);
        return device;
    }

    private List<ConfigurationFlagDto> getAllForbiddenFlags() {
        final List<ConfigurationFlagDto> listOfConfigurationFlags = new ArrayList<>();

        for (final ConfigurationFlagTypeDto confFlagType : FLAGS_TYPES_FORBIDDEN_TO_SET) {
            listOfConfigurationFlags.add(new ConfigurationFlagDto(confFlagType, false));
        }
        return listOfConfigurationFlags;

    }
}
