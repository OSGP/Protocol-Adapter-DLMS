/**
 * Copyright 2016 Smart Society Services B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.opensmartgridplatform.adapter.protocol.dlms.application.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import org.opensmartgridplatform.adapter.protocol.dlms.domain.commands.stub.AbstractCommandExecutorStub;
import org.opensmartgridplatform.adapter.protocol.dlms.domain.commands.stub.CommandExecutorMapStub;
import org.opensmartgridplatform.adapter.protocol.dlms.domain.entities.DlmsDevice;
import org.opensmartgridplatform.adapter.protocol.dlms.exceptions.ConnectionException;
import org.opensmartgridplatform.adapter.protocol.dlms.exceptions.ProtocolAdapterException;

import org.opensmartgridplatform.dto.valueobjects.smartmetering.ActionDto;
import org.opensmartgridplatform.dto.valueobjects.smartmetering.ActionDtoBuilder;
import org.opensmartgridplatform.dto.valueobjects.smartmetering.ActionRequestDto;
import org.opensmartgridplatform.dto.valueobjects.smartmetering.BundleMessagesRequestDto;
import org.opensmartgridplatform.dto.valueobjects.smartmetering.FaultResponseDto;
import org.opensmartgridplatform.dto.valueobjects.smartmetering.FaultResponseParameterDto;
import org.opensmartgridplatform.dto.valueobjects.smartmetering.FindEventsRequestDto;
import org.opensmartgridplatform.dto.valueobjects.smartmetering.OsgpResultTypeDto;
import org.opensmartgridplatform.shared.exceptionhandling.ComponentType;

@RunWith(MockitoJUnitRunner.class)
public class BundleServiceTest {

    @InjectMocks
    private BundleService bundleService;

    private ActionDtoBuilder builder = new ActionDtoBuilder();

    @Spy
    private CommandExecutorMapStub bundleCommandExecutorMap = new CommandExecutorMapStub();

    final String defaultMessage = "Unable to handle request";
    final List<FaultResponseParameterDto> parameters = new ArrayList<>();
    final ComponentType defaultComponent = ComponentType.PROTOCOL_DLMS;

    // ------------------

    @Before
    public void setup() {
    }

    @Test
    public void testHappyFlow() throws ProtocolAdapterException {
        final List<ActionDto> actionDtoList = this.makeActions();
        final BundleMessagesRequestDto dto = new BundleMessagesRequestDto(actionDtoList);
        final BundleMessagesRequestDto result = this.callExecutors(dto);
        Assert.assertTrue(result != null);
        this.assertResult(result);
    }

    @Test
    public void testException() {
        final List<ActionDto> actionDtoList = this.makeActions();
        final BundleMessagesRequestDto dto = new BundleMessagesRequestDto(actionDtoList);
        this.getStub(FindEventsRequestDto.class).failWith(new ProtocolAdapterException("simulate error"));
        final BundleMessagesRequestDto result = this.callExecutors(dto);
        this.assertResult(result);
    }

    /**
     * Tests the retry mechanism works in the adapter-protocol. In the first run
     * a ConnectionException is thrown while executing the
     * {@link FindEventsRequestDto}. In the second attempt (when the connection is
     * restored again) the rest of the actions are executed.
     *
     * @throws ProtocolAdapterException
     *             is not thrown in this test
     */
    @Test
    public void testConnectionException() throws ProtocolAdapterException {
        final List<ActionDto> actionDtoList = this.makeActions();
        final BundleMessagesRequestDto dto = new BundleMessagesRequestDto(actionDtoList);

        // Set the point where to throw the ConnectionException
        this.getStub(FindEventsRequestDto.class)
                .failWithRuntimeException(new ConnectionException("Connection Exception thrown!"));

        try {
            // Execute all the actions
            this.callExecutors(dto);
            Assert.fail("A ConnectionException should be thrown");
        } catch (final ConnectionException connectionException) {
            // The execution is stopped. The number of responses is equal to the
            // actions performed before the point the exception is thrown. See
            // also the order of the ArrayList in method 'makeActions'.
            Assert.assertEquals(dto.getAllResponses().size(), 8);
        }

        // Reset the point where the exception was thrown.
        this.getStub(FindEventsRequestDto.class).failWithRuntimeException(null);

        try {
            // Execute the remaining actions
            this.callExecutors(dto);
            Assert.assertEquals(dto.getAllResponses().size(), actionDtoList.size());
        } catch (final ConnectionException connectionException) {
            Assert.fail("A ConnectionException should not have been thrown.");
        }

    }

    @Test
    public void exceptionDetailsWithDefaultComponentInFaultResponse() throws Exception {

        final String message = "Unexpected null/unspecified value for M-Bus Capture Time";
        final Exception exception = new ProtocolAdapterException(message);

        this.parameters.add(new FaultResponseParameterDto("deviceIdentification", "ESIM1400000000123"));
        this.parameters.add(new FaultResponseParameterDto("gasDeviceIdentification", "ESIMG140000000841"));
        this.parameters.add(new FaultResponseParameterDto("channel", "3"));

        final FaultResponseDto faultResponse = this.bundleService.faultResponseForException(exception, this.parameters,
                this.defaultMessage);

        this.assertResponse(faultResponse, null, this.defaultMessage, this.defaultComponent.name(),
                exception.getClass().getName(), message, this.parameters);
    }

    public void assertResponse(final FaultResponseDto actualResponse, final Integer expectedCode,
            final String expectedMessage, final String expectedComponent, final String expectedInnerException,
            final String expectedInnerMessage, final List<FaultResponseParameterDto> expectedParameterList) {

        assertNotNull("faultResponse", actualResponse);

        /*
         * Fault Response should not contain the result fields for a generic
         * Action Response, and the result should always be NOT OK.
         */
        assertNull("exception", actualResponse.getException());
        assertNull("resultString", actualResponse.getResultString());
        assertSame("result", OsgpResultTypeDto.NOT_OK, actualResponse.getResult());

        assertEquals("code", expectedCode, actualResponse.getCode());
        assertEquals("message", expectedMessage, actualResponse.getMessage());
        assertEquals("component", expectedComponent, actualResponse.getComponent());
        assertEquals("innerException", expectedInnerException, actualResponse.getInnerException());
        assertEquals("innerMessage", expectedInnerMessage, actualResponse.getInnerMessage());

        if (expectedParameterList == null || expectedParameterList.isEmpty()) {
            assertNull("parameters", actualResponse.getFaultResponseParameters());
        } else {
            assertNotNull("parameters", actualResponse.getFaultResponseParameters());
            final List<FaultResponseParameterDto> actualParameterList = actualResponse.getFaultResponseParameters()
                    .getParameterList();
            assertNotNull("parameter list", actualParameterList);
            final int numberOfParameters = expectedParameterList.size();
            assertEquals("number of parameters", numberOfParameters, actualParameterList.size());
            for (int i = 0; i < numberOfParameters; i++) {
                final FaultResponseParameterDto expectedParameter = expectedParameterList.get(i);
                final FaultResponseParameterDto actualParameter = actualParameterList.get(i);
                final int parameterNumber = i + 1;
                assertEquals("parameter key " + parameterNumber, expectedParameter.getKey(), actualParameter.getKey());
                assertEquals("parameter value " + parameterNumber, expectedParameter.getValue(),
                        actualParameter.getValue());
            }
        }
    }

    private void assertResult(final BundleMessagesRequestDto result) {
        Assert.assertTrue(result != null);
        Assert.assertNotNull(result.getActionList());
        for (final ActionDto actionDto : result.getActionList()) {
            Assert.assertNotNull(actionDto.getRequest());
            Assert.assertNotNull(actionDto.getResponse());
        }
    }

    private BundleMessagesRequestDto callExecutors(final BundleMessagesRequestDto dto) {
        final DlmsDevice device = new DlmsDevice();
        return this.bundleService.callExecutors(null, device, dto);
    }

    // ---- private helper methods

    private AbstractCommandExecutorStub getStub(final Class<? extends ActionRequestDto> actionRequestDto) {
        return (AbstractCommandExecutorStub) this.bundleCommandExecutorMap.getCommandExecutor(actionRequestDto);
    }

    private List<ActionDto> makeActions() {
        final List<ActionDto> actions = new ArrayList<>();
        actions.add(new ActionDto(this.builder.makeActualMeterReadsDataDtoAction()));
        actions.add(new ActionDto(this.builder.makePeriodicMeterReadsGasRequestDataDto()));
        actions.add(new ActionDto(this.builder.makePeriodicMeterReadsRequestDataDto()));
        actions.add(new ActionDto(this.builder.makeSpecialDaysRequestDataDto()));
        actions.add(new ActionDto(this.builder.makeReadAlarmRegisterDataDto()));
        actions.add(new ActionDto(this.builder.makeGetAdministrativeStatusDataDto()));
        actions.add(new ActionDto(this.builder.makeAdministrativeStatusTypeDataDto()));
        actions.add(new ActionDto(this.builder.makeActivityCalendarDataDto()));
        actions.add(new ActionDto(this.builder.makeFindEventsQueryDto()));
        actions.add(new ActionDto(this.builder.makeGMeterInfoDto()));
        actions.add(new ActionDto(this.builder.makeSetAlarmNotificationsRequestDataDto()));
        actions.add(new ActionDto(this.builder.makeSetConfigurationObjectRequestDataDto()));
        actions.add(new ActionDto(this.builder.makeSetPushSetupAlarmRequestDataDto()));
        actions.add(new ActionDto(this.builder.makeSetPushSetupSmsRequestDataDto()));
        actions.add(new ActionDto(this.builder.makeSynchronizeTimeRequestDataDto()));
        actions.add(new ActionDto(this.builder.makeGetAllAttributeValuesRequestDto()));
        actions.add(new ActionDto(this.builder.makeGetFirmwareVersionRequestDataDto()));
        return actions;
    }
}