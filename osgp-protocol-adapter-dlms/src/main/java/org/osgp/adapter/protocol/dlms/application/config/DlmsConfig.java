/**
 * Copyright 2015 Smart Society Services B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.osgp.adapter.protocol.dlms.application.config;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import javax.annotation.Resource;
import javax.inject.Provider;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.logging.LoggingHandler;
import org.jboss.netty.logging.InternalLogLevel;
import org.jboss.netty.logging.InternalLoggerFactory;
import org.jboss.netty.logging.Slf4JLoggerFactory;
import org.osgp.adapter.protocol.dlms.application.services.DomainHelperService;
import org.osgp.adapter.protocol.dlms.application.threads.RecoverKeyProcess;
import org.osgp.adapter.protocol.dlms.application.threads.RecoverKeyProcessInitiator;
import org.osgp.adapter.protocol.dlms.domain.factories.Hls5Connector;
import org.osgp.adapter.protocol.dlms.domain.repositories.DlmsDeviceRepository;
import org.osgp.adapter.protocol.dlms.exceptions.ProtocolAdapterException;
import org.osgp.adapter.protocol.dlms.infra.networking.DlmsChannelHandlerServer;
import org.osgp.adapter.protocol.dlms.infra.networking.DlmsPushNotificationDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.context.annotation.Scope;
import org.springframework.core.env.Environment;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * An application context Java configuration class. The usage of Java
 * configuration requires Spring Framework 3.0
 */
@Configuration
@EnableTransactionManagement()
@PropertySources({
	@PropertySource("classpath:osgp-adapter-protocol-dlms.properties"),
	@PropertySource(value = "${osgp/AdapterProtocolDlms/config}", ignoreResourceNotFound = true),
	@PropertySource(value = "${osgp/Global/config}", ignoreResourceNotFound = true),
})
public class DlmsConfig {
    private static final String PROPERTY_NAME_DLMS_PORT_SERVER = "dlms.port.server";

    private static final Logger LOGGER = LoggerFactory.getLogger(DlmsConfig.class);

    @Resource
    private Environment environment;

    public DlmsConfig() {
        InternalLoggerFactory.setDefaultFactory(new Slf4JLoggerFactory());
    }

    /**
     * Returns a ServerBootstrap setting up a server pipeline listening for
     * incoming DLMS alarm notifications.
     *
     * @return a DLMS alarm server bootstrap.
     */
    @Bean(destroyMethod = "releaseExternalResources")
    public ServerBootstrap serverBootstrap() {
        final ChannelFactory factory = new NioServerSocketChannelFactory(Executors.newCachedThreadPool(),
                Executors.newCachedThreadPool());

        final ServerBootstrap bootstrap = new ServerBootstrap(factory);

        bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
            @Override
            public ChannelPipeline getPipeline() throws ProtocolAdapterException {
                final ChannelPipeline pipeline = DlmsConfig.this.createChannelPipeline(DlmsConfig.this
                        .dlmsChannelHandlerServer());

                LOGGER.info("Created new DLMS handler pipeline for server");

                return pipeline;
            }
        });

        bootstrap.setOption("child.tcpNoDelay", true);
        bootstrap.setOption("child.keepAlive", false);

        bootstrap.bind(new InetSocketAddress(this.dlmsPortServer()));

        return bootstrap;
    }

    private ChannelPipeline createChannelPipeline(final ChannelHandler handler) throws ProtocolAdapterException {
        final ChannelPipeline pipeline = Channels.pipeline();

        pipeline.addLast("loggingHandler", new LoggingHandler(InternalLogLevel.INFO, true));

        pipeline.addLast("dlmsPushNotificationDecoder", new DlmsPushNotificationDecoder());

        pipeline.addLast("dlmsChannelHandler", handler);

        return pipeline;
    }

    /**
     * Returns the port the DLMS server is listening on.
     *
     * @return the port number of the DLMS server endpoint.
     */
    @Bean
    public int dlmsPortServer() {
        return Integer.parseInt(this.environment.getProperty(PROPERTY_NAME_DLMS_PORT_SERVER));
    }

    /**
     * @return a new {@link DlmsChannelHandlerServer}.
     */
    @Bean
    public DlmsChannelHandlerServer dlmsChannelHandlerServer() {
        return new DlmsChannelHandlerServer();
    }

    @Bean
    @Scope("prototype")
    @Autowired
    public Hls5Connector hls5Connector(final RecoverKeyProcessInitiator recoverKeyProcessInitiator,
            final DlmsDeviceRepository dlmsDeviceRepository,
            @Value("${jdlms.response_timeout}") final int responseTimeout,
            @Value("${jdlms.logical_device_address}") final int logicalDeviceAddress,
            @Value("${jdlms.client_access_point}") final int clientAccessPoint) {
        return new Hls5Connector(recoverKeyProcessInitiator, dlmsDeviceRepository, responseTimeout,
                logicalDeviceAddress, clientAccessPoint);
    }

    @Bean
    @Scope("prototype")
    @Autowired
    public RecoverKeyProcess recoverKeyProcess(final DomainHelperService domainHelperService,
            final DlmsDeviceRepository dlmsDeviceRepository,
            @Value("${jdlms.response_timeout}") final int responseTimeout,
            @Value("${jdlms.logical_device_address}") final int logicalDeviceAddress,
            @Value("${jdlms.client_access_point}") final int clientAccessPoint) {
        return new RecoverKeyProcess(domainHelperService, dlmsDeviceRepository, responseTimeout, logicalDeviceAddress,
                clientAccessPoint);
    }

    @Bean
    @Autowired
    public RecoverKeyProcessInitiator recoverKeyProcesInitiator(final ScheduledExecutorService executorService,
            final Provider<RecoverKeyProcess> recoverKeyProcessProvider,
            @Value("${key.recovery.delay}") final int recoverKeyDelay) {
        return new RecoverKeyProcessInitiator(executorService, recoverKeyProcessProvider, recoverKeyDelay);
    }

    @Bean
    public ScheduledExecutorService scheduledExecutorService(@Value("${executor.scheduled.poolsize}") final int poolsize) {
        return Executors.newScheduledThreadPool(poolsize);
    }
}
