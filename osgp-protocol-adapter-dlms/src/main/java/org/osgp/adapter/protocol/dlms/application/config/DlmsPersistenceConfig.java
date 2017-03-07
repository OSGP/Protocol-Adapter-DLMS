/**
 * Copyright 2015 Smart Society Services B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.osgp.adapter.protocol.dlms.application.config;

import java.util.Properties;

import javax.annotation.PreDestroy;
import javax.sql.DataSource;

import org.hibernate.ejb.HibernatePersistence;
import org.jboss.netty.logging.InternalLoggerFactory;
import org.jboss.netty.logging.Slf4JLoggerFactory;
import org.osgp.adapter.protocol.dlms.domain.repositories.DlmsDeviceRepository;
import org.osgp.adapter.protocol.dlms.exceptions.ProtocolAdapterException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.alliander.osgp.shared.application.config.AbstractConfig;
import com.alliander.osgp.shared.infra.db.DefaultConnectionPoolFactory;
import com.googlecode.flyway.core.Flyway;
import com.zaxxer.hikari.HikariDataSource;

/**
 * An application context Java configuration class.
 */
@EnableJpaRepositories(entityManagerFactoryRef = "dlmsEntityManagerFactory", basePackageClasses = { DlmsDeviceRepository.class })
@Configuration
@EnableTransactionManagement()
@PropertySources({ @PropertySource("classpath:osgp-adapter-protocol-dlms.properties"),
        @PropertySource(value = "file:${osgp/Global/config}", ignoreResourceNotFound = true),
        @PropertySource(value = "file:${osgp/AdapterProtocolDlms/config}", ignoreResourceNotFound = true), })
public class DlmsPersistenceConfig extends AbstractConfig {

    private static final String PROPERTY_NAME_DATABASE_USERNAME = "db.username.dlms";
    private static final String PROPERTY_NAME_DATABASE_PW = "db.password.dlms";

    private static final String PROPERTY_NAME_DATABASE_DRIVER = "db.driver";
    private static final String PROPERTY_NAME_DATABASE_PROTOCOL = "db.protocol";

    private static final String PROPERTY_NAME_DATABASE_HOST = "db.host.dlms";
    private static final String PROPERTY_NAME_DATABASE_PORT = "db.port.dlms";
    private static final String PROPERTY_NAME_DATABASE_NAME = "db.name.dlms";

    private static final String PROPERTY_NAME_DATABASE_MIN_POOL_SIZE = "db.min_pool_size";
    private static final String PROPERTY_NAME_DATABASE_MAX_POOL_SIZE = "db.max_pool_size";
    private static final String PROPERTY_NAME_DATABASE_AUTO_COMMIT = "db.auto_commit";
    private static final String PROPERTY_NAME_DATABASE_IDLE_TIMEOUT = "db.idle_timeout";

    private static final String PROPERTY_NAME_HIBERNATE_DIALECT = "hibernate.dialect";
    private static final String PROPERTY_NAME_HIBERNATE_FORMAT_SQL = "hibernate.format_sql";
    private static final String PROPERTY_NAME_HIBERNATE_NAMING_STRATEGY = "hibernate.ejb.naming_strategy";
    private static final String PROPERTY_NAME_HIBERNATE_SHOW_SQL = "hibernate.show_sql";

    private static final String PROPERTY_NAME_FLYWAY_INITIAL_VERSION = "flyway.initial.version";
    private static final String PROPERTY_NAME_FLYWAY_INITIAL_DESCRIPTION = "flyway.initial.description";
    private static final String PROPERTY_NAME_FLYWAY_INIT_ON_MIGRATE = "flyway.init.on.migrate";

    private static final String PROPERTY_NAME_ENTITYMANAGER_PACKAGES_TO_SCAN = "entitymanager.packages.to.scan";

    private static final Logger LOGGER = LoggerFactory.getLogger(DlmsPersistenceConfig.class);

    private HikariDataSource dataSource;

    public DlmsPersistenceConfig() {
        InternalLoggerFactory.setDefaultFactory(new Slf4JLoggerFactory());
    }

    /**
     * Method for creating the Data Source.
     *
     * @return DataSource
     */
    public DataSource dlmsDataSource() {
        if (this.dataSource == null) {
            final String username = this.environment.getRequiredProperty(PROPERTY_NAME_DATABASE_USERNAME);
            final String password = this.environment.getRequiredProperty(PROPERTY_NAME_DATABASE_PW);

            final String driverClassName = this.environment.getRequiredProperty(PROPERTY_NAME_DATABASE_DRIVER);
            final String databaseProtocol = this.environment.getRequiredProperty(PROPERTY_NAME_DATABASE_PROTOCOL);

            final String databaseHost = this.environment.getRequiredProperty(PROPERTY_NAME_DATABASE_HOST);
            final int databasePort = Integer
                    .parseInt(this.environment.getRequiredProperty(PROPERTY_NAME_DATABASE_PORT));
            final String databaseName = this.environment.getRequiredProperty(PROPERTY_NAME_DATABASE_NAME);

            final int minPoolSize = Integer.parseInt(this.environment
                    .getRequiredProperty(PROPERTY_NAME_DATABASE_MIN_POOL_SIZE));
            final int maxPoolSize = Integer.parseInt(this.environment
                    .getRequiredProperty(PROPERTY_NAME_DATABASE_MAX_POOL_SIZE));
            final boolean isAutoCommit = Boolean.parseBoolean(this.environment
                    .getRequiredProperty(PROPERTY_NAME_DATABASE_AUTO_COMMIT));
            final int idleTimeout = Integer.parseInt(this.environment
                    .getRequiredProperty(PROPERTY_NAME_DATABASE_IDLE_TIMEOUT));

            final DefaultConnectionPoolFactory.Builder builder = new DefaultConnectionPoolFactory.Builder()
            .withUsername(username).withPassword(password).withDriverClassName(driverClassName)
            .withProtocol(databaseProtocol).withDatabaseHost(databaseHost).withDatabasePort(databasePort)
            .withDatabaseName(databaseName).withMinPoolSize(minPoolSize).withMaxPoolSize(maxPoolSize)
            .withAutoCommit(isAutoCommit).withIdleTimeout(idleTimeout);
            final DefaultConnectionPoolFactory factory = builder.build();
            this.dataSource = factory.getDefaultConnectionPool();
        }
        return this.dataSource;
    }

    /**
     * Method for creating the Transaction Manager.
     *
     * @return JpaTransactionManager
     * @throws ClassNotFoundException
     *             when class not found
     */
    @Bean
    public JpaTransactionManager transactionManager() throws ProtocolAdapterException {
        final JpaTransactionManager transactionManager = new JpaTransactionManager();

        try {
            transactionManager.setEntityManagerFactory(this.dlmsEntityManagerFactory().getObject());
            transactionManager.setTransactionSynchronization(JpaTransactionManager.SYNCHRONIZATION_ALWAYS);
        } catch (final ClassNotFoundException e) {
            final String msg = "Error in creating transaction manager bean";
            LOGGER.error(msg, e);
            throw new ProtocolAdapterException(msg, e);
        }

        return transactionManager;
    }

    /**
     * @return
     */
    @Bean(initMethod = "migrate")
    public Flyway dlmsFlyway() {
        final Flyway flyway = new Flyway();

        // Initialization for non-empty schema with no metadata table
        flyway.setInitVersion(this.environment.getRequiredProperty(PROPERTY_NAME_FLYWAY_INITIAL_VERSION));
        flyway.setInitDescription(this.environment.getRequiredProperty(PROPERTY_NAME_FLYWAY_INITIAL_DESCRIPTION));
        flyway.setInitOnMigrate(Boolean.parseBoolean(this.environment
                .getRequiredProperty(PROPERTY_NAME_FLYWAY_INIT_ON_MIGRATE)));

        flyway.setDataSource(this.dlmsDataSource());

        return flyway;
    }

    /**
     * Method for creating the Entity Manager Factory Bean.
     *
     * @return LocalContainerEntityManagerFactoryBean
     * @throws ClassNotFoundException
     *             when class not found
     */
    @Bean
    @DependsOn("dlmsFlyway")
    public LocalContainerEntityManagerFactoryBean dlmsEntityManagerFactory() throws ClassNotFoundException {
        final LocalContainerEntityManagerFactoryBean entityManagerFactoryBean = new LocalContainerEntityManagerFactoryBean();

        entityManagerFactoryBean.setPersistenceUnitName("OSGP_PROTOCOL_ADAPTER_DLMS_SETTINGS");
        entityManagerFactoryBean.setDataSource(this.dlmsDataSource());
        entityManagerFactoryBean.setPackagesToScan(this.environment
                .getRequiredProperty(PROPERTY_NAME_ENTITYMANAGER_PACKAGES_TO_SCAN));
        entityManagerFactoryBean.setPersistenceProviderClass(HibernatePersistence.class);

        final Properties jpaProperties = new Properties();
        jpaProperties.put(PROPERTY_NAME_HIBERNATE_DIALECT,
                this.environment.getRequiredProperty(PROPERTY_NAME_HIBERNATE_DIALECT));
        jpaProperties.put(PROPERTY_NAME_HIBERNATE_FORMAT_SQL,
                this.environment.getRequiredProperty(PROPERTY_NAME_HIBERNATE_FORMAT_SQL));
        jpaProperties.put(PROPERTY_NAME_HIBERNATE_NAMING_STRATEGY,
                this.environment.getRequiredProperty(PROPERTY_NAME_HIBERNATE_NAMING_STRATEGY));
        jpaProperties.put(PROPERTY_NAME_HIBERNATE_SHOW_SQL,
                this.environment.getRequiredProperty(PROPERTY_NAME_HIBERNATE_SHOW_SQL));

        entityManagerFactoryBean.setJpaProperties(jpaProperties);

        return entityManagerFactoryBean;
    }

    @PreDestroy
    public void destroyDataSource() {
        if (this.dataSource != null) {
            this.dataSource.close();
        }
    }
}
