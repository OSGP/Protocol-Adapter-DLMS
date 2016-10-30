/**
 * Copyright 2015 Smart Society Services B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.osgp.adapter.protocol.dlms.domain.entities;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.Transient;

import org.hibernate.annotations.Cascade;

import com.alliander.osgp.shared.domain.entities.AbstractEntity;

@Entity
public class DlmsDevice extends AbstractEntity {

    /**
     * Serial Version UID.
     */
    private static final long serialVersionUID = 3899692163578950343L;

    @Column(unique = true, nullable = false, length = 40)
    private String deviceIdentification;

    @Column
    private String communicationMethod;

    @Column
    private String communicationProvider;

    @Column
    private String iccId;

    @Column
    private boolean hls3Active;

    @Column
    private boolean hls4Active;

    @Column
    private boolean hls5Active;

    @OneToMany(mappedBy = "dlmsDevice", fetch = FetchType.EAGER, cascade = { CascadeType.ALL })
    @Cascade(org.hibernate.annotations.CascadeType.DELETE_ORPHAN)
    private final List<SecurityKey> securityKeys = new ArrayList<>();

    @Column
    private Integer challengeLength;

    @Column
    private boolean withListSupported;

    @Column
    private boolean selectiveAccessSupported;

    @Column
    private boolean ipAddressIsStatic;

    // The following three are optional columns that are used in the device
    // simulator (DeviceServer)
    @Column
    private Long port;

    @Column
    private Long clientId;

    @Column
    private Long logicalId;

    @Column
    private boolean inDebugMode;

    // -- This comes from: Core Device.

    @Transient
    private String ipAddress;

    public DlmsDevice() {
        // Default constructor
    }

    public DlmsDevice(final String deviceIdentification) {
        this.deviceIdentification = deviceIdentification;
    }

    public String getDeviceIdentification() {
        return this.deviceIdentification;
    }

    @Override
    public String toString() {
        return String.format(
                "DlmsDevice[deviceId=%s, hls3=%b, hls4=%b, hls5=%b, ipAddress=%s, port=%s, logicalId=%s, clientId=%s]",
                this.deviceIdentification, this.hls3Active, this.hls4Active, this.hls5Active, this.ipAddress, this.port,
                this.logicalId, this.clientId);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }

        final DlmsDevice device = (DlmsDevice) o;

        if (this.deviceIdentification != null ? !this.deviceIdentification.equals(device.deviceIdentification)
                : device.deviceIdentification != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return 31 * (this.deviceIdentification != null ? this.deviceIdentification.hashCode() : 0);
    }

    public boolean isIpAddressIsStatic() {
        return this.ipAddressIsStatic;
    }

    public void setIpAddressIsStatic(final boolean ipAddressIsStatic) {
        this.ipAddressIsStatic = ipAddressIsStatic;
    }

    public String getCommunicationMethod() {
        return this.communicationMethod;
    }

    public void setCommunicationMethod(final String communicationMethod) {
        this.communicationMethod = communicationMethod;
    }

    public String getCommunicationProvider() {
        return this.communicationProvider;
    }

    public void setCommunicationProvider(final String communicationProvider) {
        this.communicationProvider = communicationProvider;
    }

    public void setIccId(final String value) {
        this.iccId = value;
    }

    public String getIccId() {
        return this.iccId;
    }

    public boolean isHls3Active() {
        return this.hls3Active;
    }

    public void setHls3Active(final boolean hls3Active) {
        this.hls3Active = hls3Active;
    }

    public boolean isHls4Active() {
        return this.hls4Active;
    }

    public void setHls4Active(final boolean hls4Active) {
        this.hls4Active = hls4Active;
    }

    public boolean isHls5Active() {
        return this.hls5Active;
    }

    public void setHls5Active(final boolean hls5Active) {
        this.hls5Active = hls5Active;
    }

    public Integer getChallengeLength() {
        return this.challengeLength;
    }

    public void setChallengeLength(final Integer challengeLength) {
        this.challengeLength = challengeLength;
    }

    public boolean isWithListSupported() {
        return this.withListSupported;
    }

    public void setWithListSupported(final boolean withListSupported) {
        this.withListSupported = withListSupported;
    }

    public boolean isSelectiveAccessSupported() {
        return this.selectiveAccessSupported;
    }

    public void setSelectiveAccessSupported(final boolean selectiveAccessSupported) {
        this.selectiveAccessSupported = selectiveAccessSupported;
    }

    public void setDeviceIdentification(final String deviceIdentification) {
        this.deviceIdentification = deviceIdentification;
    }

    public List<SecurityKey> getSecurityKeys() {
        return this.securityKeys;
    }

    public void addSecurityKey(final SecurityKey securityKey) {
        this.securityKeys.add(securityKey);
    }

    public Long getPort() {
        return this.port;
    }

    public void setPort(final Long port) {
        this.port = port;
    }

    public Long getClientId() {
        return this.clientId;
    }

    public void setClientId(final Long clientId) {
        this.clientId = clientId;
    }

    public Long getLogicalId() {
        return this.logicalId;
    }

    public void setLogicalId(final Long logicalId) {
        this.logicalId = logicalId;
    }

    public boolean isInDebugMode() {
        return this.inDebugMode;
    }

    public void setInDebugMode(final boolean inDebugMode) {
        this.inDebugMode = inDebugMode;
    }

    /**
     * The IP address is not part of the data in the protocol adapter database.
     * The value needs to have been set based on information from the core
     * database before it can be used.
     *
     * @return the device's network address, if it has been explicitly set;
     *         otherwise {@code null}.
     */
    public String getIpAddress() {
        return this.ipAddress;
    }

    public void setIpAddress(final String ipAddress) {
        this.ipAddress = ipAddress;
    }

    /**
     * Get the valid security key of the given type. This can be only one or
     * none. If none is found, null is returned.
     *
     * @param securityKeyType
     * @return Security key, or null if no valid key is found.
     */
    public SecurityKey getValidSecurityKey(final SecurityKeyType securityKeyType) {
        for (final SecurityKey securityKey : this.securityKeys) {
            if (securityKey.getSecurityKeyType().equals(securityKeyType) && this.securityKeyActivated(securityKey)
                    && !this.securityKeyExpired(securityKey)) {
                return securityKey;
            }
        }

        return null;
    }

    private List<SecurityKey> getNewSecurityKeys() {
        final List<SecurityKey> keys = new ArrayList<>();
        for (final SecurityKey securityKey : this.securityKeys) {
            if (securityKey.getValidFrom() == null) {
                keys.add(securityKey);
            }
        }
        return keys;
    }

    public boolean hasNewSecurityKey() {
        return !this.getNewSecurityKeys().isEmpty();
    }

    public SecurityKey getNewSecurityKey(final SecurityKeyType securityKeyType) {
        final List<SecurityKey> keys = this.getNewSecurityKeys();
        for (final SecurityKey securityKey : keys) {
            if (securityKey.getSecurityKeyType().equals(securityKeyType)) {
                return securityKey;
            }
        }

        return null;
    }

    /**
     * Check if the security key has become active before now.
     *
     * @param securityKey
     * @return activated
     */
    private boolean securityKeyActivated(final SecurityKey securityKey) {
        if (securityKey.getValidFrom() == null) {
            return false;
        }

        final Date now = new Date();
        return securityKey.getValidFrom().before(now) || securityKey.getValidFrom().equals(now);
    }

    /**
     * Check if security key is expired, the valid to date is before now.
     *
     * @param securityKey
     * @return expired.
     */
    private boolean securityKeyExpired(final SecurityKey securityKey) {
        final Date now = new Date();
        final Date validTo = securityKey.getValidTo();
        return validTo != null && validTo.before(now);
    }

    /**
     * Removes keys that have never been valid. Caution: only execute this
     * method when valid keys have been proven to work with the meter.
     * Otherwise, these invalid keys could hold keys that are present on the
     * meter
     */
    public void discardInvalidKeys() {
        final List<SecurityKey> keys = this.getNewSecurityKeys();
        if (!keys.isEmpty()) {
            this.getSecurityKeys().removeAll(keys);
        }
    }

    /**
     * Promotes the existing invalid (or never valid) key to be a valid key, and
     * makes the currently valid key a key of the past.
     */
    public void promoteInvalidKey() {
        if (this.getNewSecurityKeys().size() > 1) {
            throw new IllegalStateException("There may not be more than one new, never valid, security key.");
        }

        final SecurityKeyType[] keyTypes = new SecurityKeyType[] { SecurityKeyType.E_METER_AUTHENTICATION,
                SecurityKeyType.E_METER_ENCRYPTION };

        for (final SecurityKeyType keyType : keyTypes) {
            final SecurityKey key = this.getNewSecurityKey(keyType);
            if (key != null && key.getValidFrom() == null) {
                this.promoteInvalidKey(key);
            }
        }
    }

    private void promoteInvalidKey(final SecurityKey promoteKey) {
        final Date now = new Date();
        this.getValidSecurityKey(promoteKey.getSecurityKeyType()).setValidTo(now);
        promoteKey.setValidFrom(now);
    }
}
