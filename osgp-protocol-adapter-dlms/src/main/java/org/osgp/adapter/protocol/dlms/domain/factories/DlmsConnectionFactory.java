/**
 * Copyright 2015 Smart Society Services B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.osgp.adapter.protocol.dlms.domain.factories;

import java.io.IOException;

import javax.inject.Provider;
import javax.naming.OperationNotSupportedException;

import org.openmuc.jdlms.DlmsConnection;
import org.osgp.adapter.protocol.dlms.domain.entities.DlmsDevice;
import org.osgp.adapter.protocol.dlms.infra.messaging.DlmsMessageListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alliander.osgp.shared.exceptionhandling.TechnicalException;

@Component
public class DlmsConnectionFactory {

    @Autowired
    private Provider<Hls5Connector> hls5ConnectorProvider;

    /**
     * Returns an open connection using the appropriate security settings for
     * the device.
     *
     * @param device
     *            The device to connect to. This reference can be updated when
     *            the invalid but correctable connection credentials are
     *            detected.
     * @param dlmsMessageListener
     *            A message listener that will be provided to the
     *            {@link DlmsConnection} that is initialized if the given
     *            {@code device} is in {@link DlmsDevice#isInDebugMode() debug
     *            mode}. If this is {@code null} no DLMS device communication
     *            debug logging will be done.
     * @return a holder providing access to an open DLMS connection as well as
     *         an optional message listener active in the connection.
     * @throws IOException
     * @throws OperationNotSupportedException
     */
    public DlmsConnectionHolder getConnection(final DlmsDevice device, final DlmsMessageListener dlmsMessageListener)
            throws TechnicalException {
        if (device.isHls5Active()) {
            final DlmsConnectionHolder holder = new DlmsConnectionHolder(this.hls5ConnectorProvider.get(), device,
                    dlmsMessageListener);
            holder.connect();
            return holder;
        } else {
            throw new UnsupportedOperationException("Only HLS 5 connections are currently supported");
        }
    }
}
