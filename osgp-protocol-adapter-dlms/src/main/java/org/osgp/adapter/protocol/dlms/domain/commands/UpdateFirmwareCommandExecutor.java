/**
 * Copyright 2016 Smart Society Services B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.osgp.adapter.protocol.dlms.domain.commands;

import java.util.List;

import javax.annotation.PostConstruct;

import org.osgp.adapter.protocol.dlms.domain.entities.DlmsDevice;
import org.osgp.adapter.protocol.dlms.domain.factories.DlmsConnectionHolder;
import org.osgp.adapter.protocol.dlms.domain.factories.FirwareImageFactory;
import org.osgp.adapter.protocol.dlms.exceptions.FirmwareImageFactoryException;
import org.osgp.adapter.protocol.dlms.exceptions.ImageTransferException;
import org.osgp.adapter.protocol.dlms.exceptions.ProtocolAdapterException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.alliander.osgp.dto.valueobjects.FirmwareVersionDto;
import com.alliander.osgp.dto.valueobjects.smartmetering.ActionResponseDto;
import com.alliander.osgp.dto.valueobjects.smartmetering.UpdateFirmwareResponseDto;

@Component
public class UpdateFirmwareCommandExecutor extends AbstractCommandExecutor<String, List<FirmwareVersionDto>> {

    private static final String EXCEPTION_MSG_UPDATE_FAILED = "Upgrade of firmware did not succeed.";

    private static final String EXCEPTION_MSG_INSTALLATION_FILE_NOT_AVAILABLE = "Installation file is not available.";

    @Autowired
    private FirwareImageFactory firmwareImageFactory;

    @Autowired
    private GetFirmwareVersionsCommandExecutor getFirmwareVersionsCommandExecutor;

    @Value("${command.updatefirmware.activationstatuscheck.interval}")
    private int activationStatusCheckInterval;

    @Value("${command.updatefirmware.activationstatuscheck.timeout}")
    private int activationStatusCheckTimeout;

    @Value("${command.updatefirmware.verificationstatuscheck.interval}")
    private int verificationStatusCheckInterval;

    @Value("${command.updatefirmware.verificationstatuscheck.timeout}")
    private int verificationStatusCheckTimeout;

    private ImageTransfer.ImageTranferProperties imageTransferProperties;

    public UpdateFirmwareCommandExecutor() {
        /*
         * No argument constructor for subclasses that do not act in a bundle
         * context, so they do not need to be looked up by ActionRequestDto
         * class.
         */
    }

    @PostConstruct
    @Override
    public void init() {
        this.imageTransferProperties = new ImageTransfer.ImageTranferProperties();
        this.imageTransferProperties.setActivationStatusCheckInterval(this.activationStatusCheckInterval);
        this.imageTransferProperties.setActivationStatusCheckTimeout(this.activationStatusCheckTimeout);
        this.imageTransferProperties.setVerificationStatusCheckInterval(this.verificationStatusCheckInterval);
        this.imageTransferProperties.setVerificationStatusCheckTimeout(this.verificationStatusCheckTimeout);

        super.init();
    }

    @Override
    public List<FirmwareVersionDto> execute(final DlmsConnectionHolder conn, final DlmsDevice device,
            final String firmwareIdentification) throws ProtocolAdapterException {
        final ImageTransfer transfer = new ImageTransfer(conn, this.imageTransferProperties, firmwareIdentification,
                this.getImageData(firmwareIdentification));

        try {
            this.prepare(transfer);
            this.transfer(transfer);
            this.verify(transfer);
            return this.activate(conn, device, transfer);
        } catch (ImageTransferException | ProtocolAdapterException e) {
            throw new ProtocolAdapterException(EXCEPTION_MSG_UPDATE_FAILED, e);
        } finally {
            transfer.setImageTransferEnabled(false);
        }
    }

    private void prepare(final ImageTransfer transfer) throws ProtocolAdapterException {
        if (!transfer.imageTransferEnabled()) {
            transfer.setImageTransferEnabled(true);
        }

        if (transfer.shouldInitiateTransfer()) {
            transfer.initiateImageTransfer();
        }
    }

    private void transfer(final ImageTransfer transfer) throws ProtocolAdapterException {
        if (transfer.shouldTransferImage()) {
            transfer.transferImageBlocks();
            transfer.transferMissingImageBlocks();
        }
    }

    private void verify(final ImageTransfer transfer) throws ProtocolAdapterException, ImageTransferException {
        if (!transfer.imageIsVerified()) {
            transfer.verifyImage();
        }
    }

    private List<FirmwareVersionDto> activate(final DlmsConnectionHolder conn, final DlmsDevice device,
            final ImageTransfer transfer) throws ProtocolAdapterException, ImageTransferException {
        if (transfer.imageIsVerified() && transfer.imageToActivateOk()) {
            transfer.activateImage();
            return this.getFirmwareVersionsCommandExecutor.execute(conn, device, null);
        } else {
            // Image data is not correct.
            throw new ProtocolAdapterException("An unknown error occurred while updating firmware.");
        }
    }

    private byte[] getImageData(final String firmwareIdentification) throws ProtocolAdapterException {
        try {
            return this.firmwareImageFactory.getFirmwareImage(firmwareIdentification);
        } catch (final FirmwareImageFactoryException e) {
            throw new ProtocolAdapterException(EXCEPTION_MSG_INSTALLATION_FILE_NOT_AVAILABLE, e);
        }
    }

    @Override
    public ActionResponseDto asBundleResponse(final List<FirmwareVersionDto> executionResult)
            throws ProtocolAdapterException {

        return new UpdateFirmwareResponseDto(executionResult);
    }
}
