/**
 * Copyright 2016 Smart Society Services B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.osgp.adapter.protocol.dlms.domain.commands;

import javax.annotation.PostConstruct;

import org.openmuc.jdlms.AccessResultCode;
import org.openmuc.jdlms.MethodResultCode;
import org.osgp.adapter.protocol.dlms.domain.entities.DlmsDevice;
import org.osgp.adapter.protocol.dlms.domain.factories.DlmsConnectionHolder;
import org.osgp.adapter.protocol.dlms.exceptions.ProtocolAdapterException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.alliander.osgp.dto.valueobjects.smartmetering.ActionRequestDto;
import com.alliander.osgp.dto.valueobjects.smartmetering.ActionResponseDto;

public abstract class AbstractCommandExecutor<T, R> implements CommandExecutor<T, R> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractCommandExecutor.class);

    @Autowired
    private CommandExecutorMap bundleCommandExecutorMap;

    private final Class<? extends ActionRequestDto> bundleExecutorMapKey;

    /**
     * Constructor for CommandExecutors that do not need to be executed in the
     * context of bundle actions.
     */
    public AbstractCommandExecutor() {
        this(null);
    }

    /**
     * Constructor for CommandExecutors that need to be executed in the context
     * of bundle actions.
     *
     * @param clazz
     *            the class of the ActionRequestDto subtype for which this
     *            CommandExecutor needs to be called.
     */
    public AbstractCommandExecutor(final Class<? extends ActionRequestDto> clazz) {
        this.bundleExecutorMapKey = clazz;
    }

    @PostConstruct
    public void init() {
        if (this.bundleExecutorMapKey != null) {
            this.bundleCommandExecutorMap.addCommandExecutor(this.bundleExecutorMapKey, this);
        }
    }

    @Override
    public ActionResponseDto executeBundleAction(final DlmsConnectionHolder conn, final DlmsDevice device,
            final ActionRequestDto actionRequestDto) throws ProtocolAdapterException {

        if (this.bundleExecutorMapKey == null) {
            throw new ProtocolAdapterException("Execution of " + this.getClass().getName()
                    + " is not supported in a bundle context.");
        }

        final T commandInput = this.fromBundleRequestInput(actionRequestDto);
        LOGGER.debug("Translated {} from bundle to {} for call to CommandExecutor.", this.className(actionRequestDto),
                this.className(commandInput));
        final R executionResult = this.execute(conn, device, commandInput);
        final ActionResponseDto bundleResponse = this.asBundleResponse(executionResult);
        LOGGER.debug("Translated {} to {} for bundle response after call to CommandExecutor.",
                this.className(executionResult), this.className(bundleResponse));
        return bundleResponse;
    }

    protected void checkActionRequestType(final ActionRequestDto bundleInput) throws ProtocolAdapterException {
        if (this.bundleExecutorMapKey != null && !this.bundleExecutorMapKey.isInstance(bundleInput)) {
            throw new ProtocolAdapterException("Expected bundleInput to be of type "
                    + this.bundleExecutorMapKey.getName() + ", got: " + this.className(bundleInput));
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public T fromBundleRequestInput(final ActionRequestDto bundleInput) throws ProtocolAdapterException {
        if (bundleInput == null) {
            return null;
        }
        try {
            return (T) bundleInput;
        } catch (final ClassCastException e) {
            throw new ProtocolAdapterException("Translation from bundle ActionRequestDto to CommandExecutor input for "
                    + this.getClass().getName() + " is not implemented.", e);
        }
    }

    @Override
    public ActionResponseDto asBundleResponse(final R executionResult) throws ProtocolAdapterException {
        try {
            return ActionResponseDto.class.cast(executionResult);
        } catch (final ClassCastException e) {
            throw new ProtocolAdapterException(
                    "Translation from CommandExecutor result to bundle ActionResponseDto for "
                            + this.getClass().getName() + " is not implemented.", e);
        }
    }

    protected final String className(final Object object) {
        if (object == null) {
            return "null";
        }
        return object.getClass().getName();
    }

    protected Class<? extends ActionRequestDto> bundleExecutorMapKey() {
        return this.bundleExecutorMapKey;
    }

    protected void checkAccessResultCode(final AccessResultCode accessResultCode) throws ProtocolAdapterException {
        if (AccessResultCode.SUCCESS != accessResultCode) {
            throw new ProtocolAdapterException("AccessResultCode: " + accessResultCode);
        }
    }

    protected void checkMethodResultCode(final MethodResultCode methodResultCode) throws ProtocolAdapterException {
        if (MethodResultCode.SUCCESS != methodResultCode) {
            throw new ProtocolAdapterException("MethodResultCode: " + methodResultCode);
        }
    }
}
