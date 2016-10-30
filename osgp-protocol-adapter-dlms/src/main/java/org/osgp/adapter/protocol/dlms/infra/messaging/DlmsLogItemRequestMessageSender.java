/**
 * Copyright 2015 Smart Society Services B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.osgp.adapter.protocol.dlms.infra.messaging;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.ObjectMessage;
import javax.jms.Session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;

import com.alliander.osgp.shared.infra.jms.Constants;

public class DlmsLogItemRequestMessageSender {

    private static final Logger LOGGER = LoggerFactory.getLogger(DlmsLogItemRequestMessageSender.class);

    @Autowired
    private JmsTemplate dlmsLogItemRequestsJmsTemplate;

    public void send(final DlmsLogItemRequestMessage dlmsLogItemRequestMessage) {

        LOGGER.debug("Sending DlmsLogItemRequestMessage");

        this.dlmsLogItemRequestsJmsTemplate.send(new MessageCreator() {
            @Override
            public Message createMessage(final Session session) throws JMSException {
                final ObjectMessage objectMessage = session.createObjectMessage();
                objectMessage.setJMSType(Constants.DLMS_LOG_ITEM_REQUEST);
                objectMessage.setStringProperty(Constants.IS_INCOMING, dlmsLogItemRequestMessage.isIncoming()
                        .toString());
                objectMessage.setStringProperty(Constants.ENCODED_MESSAGE,
                        dlmsLogItemRequestMessage.getEncodedMessage());
                objectMessage.setStringProperty(Constants.DECODED_MESSAGE,
                        dlmsLogItemRequestMessage.getDecodedMessage());
                objectMessage.setStringProperty(Constants.DEVICE_IDENTIFICATION,
                        dlmsLogItemRequestMessage.getDeviceIdentification());
                if (dlmsLogItemRequestMessage.hasOrganisationIdentification()) {
                    objectMessage.setStringProperty(Constants.ORGANISATION_IDENTIFICATION,
                            dlmsLogItemRequestMessage.getOrganisationIdentification());
                }
                objectMessage.setStringProperty(Constants.IS_VALID, dlmsLogItemRequestMessage.isValid().toString());
                objectMessage.setIntProperty(Constants.PAYLOAD_MESSAGE_SERIALIZED_SIZE,
                        dlmsLogItemRequestMessage.getPayloadMessageSerializedSize());
                return objectMessage;
            }
        });
    }
}
