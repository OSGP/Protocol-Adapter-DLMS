/**
 * Copyright 2015 Smart Society Services B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.osgp.adapter.protocol.dlms.domain.commands;

import org.openmuc.jdlms.ClientConnection;
import org.osgp.adapter.protocol.dlms.domain.entities.DlmsDevice;
import org.osgp.adapter.protocol.dlms.exceptions.ProtocolAdapterException;

/**
 * Abstract class for executors that don't take input, calls to
 * {@link #execute(ClientConnection, DlmsDevice, Void)} are forwarded to
 * {@link #execute(ClientConnection, DlmsDevice)}
 *
 * @param <R>
 *            the type of object returned as a result from executing a command.
 */
public abstract class CommandExecutorNoInput<R> implements CommandExecutor<Void, R> {

    @Override
    public final R execute(final ClientConnection conn, final DlmsDevice device, final Void object)
            throws ProtocolAdapterException {
        return this.execute(conn, device);
    }

    /**
     * subclasses should implement this method
     * 
     * @param conn
     * @param device
     * @return
     * @throws ProtocolAdapterException
     */
    protected abstract R execute(ClientConnection conn, DlmsDevice device) throws ProtocolAdapterException;

}
