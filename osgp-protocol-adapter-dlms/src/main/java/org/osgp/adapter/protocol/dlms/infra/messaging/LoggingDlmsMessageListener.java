/**
 * Copyright 2016 Smart Society Services B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.osgp.adapter.protocol.dlms.infra.messaging;

import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.StringUtils;
import org.openmuc.jdlms.RawMessageData;
import org.openmuc.jdlms.RawMessageData.MessageSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingDlmsMessageListener implements DlmsMessageListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingDlmsMessageListener.class);

    private final String deviceIdentification;
    private final DlmsLogItemRequestMessageSender dlmsLogItemRequestMessageSender;

    private DlmsDeviceMessageMetadata messageMetadata;
    private String description;

    private AtomicInteger numberOfCapturedMessages = new AtomicInteger(0);

    public LoggingDlmsMessageListener(final String deviceIdentification,
            final DlmsLogItemRequestMessageSender dlmsLogItemRequestMessageSender) {
        this.deviceIdentification = deviceIdentification;
        this.dlmsLogItemRequestMessageSender = dlmsLogItemRequestMessageSender;
    }

    @Override
    public void messageCaptured(final RawMessageData rawMessageData) {

        final int sequenceNumber = this.numberOfCapturedMessages.incrementAndGet();

        final boolean incoming = MessageSource.SERVER == rawMessageData.getMessageSource();
        final byte[] encodedMessage = rawMessageData.getMessage();

        String decodedMessage = "";
        if (rawMessageData.getApdu() != null) {
            decodedMessage = rawMessageData.getApdu().toString().trim();
        }

        this.logMessage(incoming, encodedMessage, decodedMessage, sequenceNumber);
    }

    public void logMessage(final boolean incoming, final byte[] encodedMessage, final String decodedMessage,
            final int sequenceNumber) {

        final String communicationDirection;
        if (incoming) {
            communicationDirection = "incoming";
        } else {
            communicationDirection = "outgoing";
        }
        final String organisationIdentification;
        if (this.hasMessageMetadata()) {
            organisationIdentification = this.messageMetadata.getOrganisationIdentification();
            LOGGER.info("Logging {} device communication for {} in the context of {}.", communicationDirection,
                    this.deviceIdentification, this.messageMetadata);
        } else {
            organisationIdentification = null;
            LOGGER.info("Logging {} device communication for {} without platform message context available.",
                    communicationDirection, this.deviceIdentification);
        }

        final String decodedMessageWithDescription;
        if (this.hasDescription()) {
            decodedMessageWithDescription = String.format("%s%n%n%s", this.getDescription(sequenceNumber),
                    decodedMessage);
        } else {
            decodedMessageWithDescription = String.format("[%d]%n%s", sequenceNumber, decodedMessage);
        }

        final DlmsLogItemRequestMessage dlmsLogItemRequestMessage = new DlmsLogItemRequestMessage(
                this.deviceIdentification, organisationIdentification, incoming, encodedMessage,
                decodedMessageWithDescription);

        this.dlmsLogItemRequestMessageSender.send(dlmsLogItemRequestMessage);
    }

    public boolean hasMessageMetadata() {
        return this.messageMetadata != null;
    }

    @Override
    public void setMessageMetadata(final DlmsDeviceMessageMetadata messageMetadata) {
        this.messageMetadata = messageMetadata;
    }

    public boolean hasDescription() {
        return this.hasMessageMetadata() || this.description != null;
    }

    public String getDescription(final int sequenceNumber) {
        final StringBuilder sb = new StringBuilder();
        if (this.hasMessageMetadata()) {
            sb.append(this.messageMetadata.getMessageType());
            sb.append(" (").append(this.messageMetadata.getCorrelationUid()).append(')');
            if (this.description != null) {
                sb.append(" - ");
            }
        }
        sb.append('[').append(sequenceNumber).append(']');
        if (this.description != null) {
            sb.append(' ').append(this.description);
        }
        return sb.toString();
    }

    @Override
    public void setDescription(final String description) {
        if (StringUtils.isBlank(description)) {
            this.description = null;
        } else {
            this.description = description.trim();
        }
    }

    public int getNumberOfCapturedMessages() {
        return this.numberOfCapturedMessages.get();
    }
}
