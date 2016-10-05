/**
 * Copyright 2016 Smart Society Services B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package org.osgp.adapter.protocol.dlms.application.config;

import org.osgp.adapter.protocol.dlms.simulator.trigger.SimulatorTriggerClient;
import org.osgp.adapter.protocol.dlms.simulator.trigger.SimulatorTriggerClientException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement()
@PropertySource("file:${osp/osgpAdapterProtocolDlms/config}")
public class SimulatorTriggerConfig {

    @Value("${web.service.truststore.location}")
    private String truststoreLocation;
    @Value("${web.service.truststore.password}")
    private String truststorePassword;
    @Value("${web.service.truststore.type}")
    private String truststoreType;
    @Value("${triggered.simulator.url}")
    private String baseAddress;

    @Bean
    public SimulatorTriggerClient simulatorTriggerClient() throws SimulatorTriggerClientException {
        return new SimulatorTriggerClient(this.truststoreLocation, this.truststorePassword, this.truststoreType,
                this.baseAddress);
    }
}