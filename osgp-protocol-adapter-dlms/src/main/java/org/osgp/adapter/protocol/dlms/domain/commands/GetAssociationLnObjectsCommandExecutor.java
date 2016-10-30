/**
 * Copyright 2016 Smart Society Services B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.osgp.adapter.protocol.dlms.domain.commands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.openmuc.jdlms.AttributeAddress;
import org.openmuc.jdlms.GetResult;
import org.openmuc.jdlms.ObisCode;
import org.openmuc.jdlms.datatypes.DataObject;
import org.osgp.adapter.protocol.dlms.domain.entities.DlmsDevice;
import org.osgp.adapter.protocol.dlms.domain.factories.DlmsConnectionHolder;
import org.osgp.adapter.protocol.dlms.exceptions.ProtocolAdapterException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alliander.osgp.dto.valueobjects.smartmetering.AccessRightDto;
import com.alliander.osgp.dto.valueobjects.smartmetering.AccessSelectorListDto;
import com.alliander.osgp.dto.valueobjects.smartmetering.ActionRequestDto;
import com.alliander.osgp.dto.valueobjects.smartmetering.ActionResponseDto;
import com.alliander.osgp.dto.valueobjects.smartmetering.AssociationLnListElementDto;
import com.alliander.osgp.dto.valueobjects.smartmetering.AssociationLnListTypeDto;
import com.alliander.osgp.dto.valueobjects.smartmetering.AssociationLnObjectsResponseDto;
import com.alliander.osgp.dto.valueobjects.smartmetering.AttributeAccessDescriptorDto;
import com.alliander.osgp.dto.valueobjects.smartmetering.AttributeAccessItemDto;
import com.alliander.osgp.dto.valueobjects.smartmetering.AttributeAccessModeTypeDto;
import com.alliander.osgp.dto.valueobjects.smartmetering.GetAssociationLnObjectsRequestDto;
import com.alliander.osgp.dto.valueobjects.smartmetering.MethodAccessDescriptorDto;
import com.alliander.osgp.dto.valueobjects.smartmetering.MethodAccessItemDto;
import com.alliander.osgp.dto.valueobjects.smartmetering.MethodAccessModeTypeDto;

@Component
public class GetAssociationLnObjectsCommandExecutor extends AbstractCommandExecutor<Void, AssociationLnListTypeDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(GetAssociationLnObjectsCommandExecutor.class);

    private static final int CLASS_ID = 15;
    private static final ObisCode OBIS_CODE = new ObisCode("0.0.40.0.0.255");
    private static final int ATTRIBUTE_ID = 2;

    private static final int CLASS_ID_INDEX = 0;
    private static final int VERSION_INDEX = 1;
    private static final int OBIS_CODE_INDEX = 2;
    private static final int ACCESS_RIGHTS_INDEX = 3;

    private static final int ACCESS_RIGHTS_ATTRIBUTE_ACCESS_INDEX = 0;
    private static final int ACCESS_RIGHTS_ATTRIBUTE_ACCESS_ATTRIBUTE_ID_INDEX = 0;
    private static final int ACCESS_RIGHTS_ATTRIBUTE_ACCESS_ACCESS_MODE_INDEX = 1;
    private static final int ACCESS_RIGHTS_ATTRIBUTE_ACCESS_ACCESS_SELECTORS_INDEX = 2;

    private static final int ACCESS_RIGHTS_METHOD_ACCESS_INDEX = 1;
    private static final int ACCESS_RIGHTS_METHOD_ACCESS_METHOD_ID_INDEX = 0;
    private static final int ACCESS_RIGHTS_METHOD_ACCESS_ACCESS_MODE_INDEX = 1;

    @Autowired
    private DlmsHelperService dlmsHelperService;

    public GetAssociationLnObjectsCommandExecutor() {
        super(GetAssociationLnObjectsRequestDto.class);
    }

    @Override
    public Void fromBundleRequestInput(final ActionRequestDto bundleInput) throws ProtocolAdapterException {

        this.checkActionRequestType(bundleInput);

        return null;
    }

    @Override
    public ActionResponseDto asBundleResponse(final AssociationLnListTypeDto executionResult)
            throws ProtocolAdapterException {

        return new AssociationLnObjectsResponseDto(executionResult);
    }

    @Override
    public AssociationLnListTypeDto execute(final DlmsConnectionHolder conn, final DlmsDevice device, final Void object)
            throws ProtocolAdapterException {

        final AttributeAddress attributeAddress = new AttributeAddress(CLASS_ID, OBIS_CODE, ATTRIBUTE_ID);

        conn.getDlmsMessageListener().setDescription("GetAssociationLnObjects, retrieve attribute: "
                + JdlmsObjectToStringUtil.describeAttributes(attributeAddress));

        LOGGER.debug("Retrieving Association LN objects for class id: {}, obis code: {}, attribute id: {}", CLASS_ID,
                OBIS_CODE, ATTRIBUTE_ID);

        final List<GetResult> getResultList = this.dlmsHelperService.getAndCheck(conn, device,
                "Association LN Objects", attributeAddress);

        final DataObject resultData = getResultList.get(0).getResultData();
        if (!resultData.isComplex()) {
            throw new ProtocolAdapterException("Unexpected type of element");
        }

        @SuppressWarnings("unchecked")
        final List<AssociationLnListElementDto> elements = this
        .convertAssociationLnList((List<DataObject>) getResultList.get(0).getResultData().getValue());

        return new AssociationLnListTypeDto(elements);
    }

    private List<AssociationLnListElementDto> convertAssociationLnList(final List<DataObject> resultDataValue)
            throws ProtocolAdapterException {
        final List<AssociationLnListElementDto> elements = new ArrayList<>();

        for (final DataObject obisCodeMetaData : resultDataValue) {
            @SuppressWarnings("unchecked")
            final List<DataObject> obisCodeMetaDataList = (List<DataObject>) obisCodeMetaData.getValue();
            final AssociationLnListElementDto element = new AssociationLnListElementDto(
                    this.dlmsHelperService.readLong(obisCodeMetaDataList.get(CLASS_ID_INDEX), "classId"), new Integer(
                            (short) obisCodeMetaDataList.get(VERSION_INDEX).getValue()),
                            this.dlmsHelperService.readLogicalName(obisCodeMetaDataList.get(OBIS_CODE_INDEX),
                            "AssociationLN Element"), this.convertAccessRights(obisCodeMetaDataList
                                            .get(ACCESS_RIGHTS_INDEX)));

            elements.add(element);
        }

        return elements;
    }

    private AccessRightDto convertAccessRights(final DataObject dataObject) throws ProtocolAdapterException {
        if (!dataObject.isComplex()) {
            return null;
        }

        @SuppressWarnings("unchecked")
        final List<DataObject> accessRights = (List<DataObject>) dataObject.getValue();

        @SuppressWarnings("unchecked")
        final AttributeAccessDescriptorDto attributeAccessDescriptor = this
                .convertAttributeAccessDescriptor((List<DataObject>) accessRights.get(
                        ACCESS_RIGHTS_ATTRIBUTE_ACCESS_INDEX).getValue());

        @SuppressWarnings("unchecked")
        final MethodAccessDescriptorDto methodAccessDescriptor = this
                .convertMethodAttributeAccessDescriptor((List<DataObject>) accessRights.get(
                        ACCESS_RIGHTS_METHOD_ACCESS_INDEX).getValue());

        return new AccessRightDto(attributeAccessDescriptor, methodAccessDescriptor);
    }

    @SuppressWarnings("unchecked")
    private AttributeAccessDescriptorDto convertAttributeAccessDescriptor(
            final List<DataObject> attributeAccessDescriptor) throws ProtocolAdapterException {
        final List<AttributeAccessItemDto> attributeAccessItems = new ArrayList<>();

        for (final DataObject attributeAccessItemRaw : attributeAccessDescriptor) {
            final List<DataObject> attributeAccessItem = (List<DataObject>) attributeAccessItemRaw.getValue();

            AccessSelectorListDto asl = null;
            if (attributeAccessItem.get(ACCESS_RIGHTS_ATTRIBUTE_ACCESS_ACCESS_SELECTORS_INDEX).isNull()) {
                asl = new AccessSelectorListDto(Collections.<Integer> emptyList());
            } else {
                asl = new AccessSelectorListDto(this.convertAccessSelectors((List<DataObject>) attributeAccessItem.get(
                        ACCESS_RIGHTS_ATTRIBUTE_ACCESS_ACCESS_SELECTORS_INDEX).getValue()));
            }

            attributeAccessItems.add(new AttributeAccessItemDto(this.dlmsHelperService.readLong(
                    attributeAccessItem.get(ACCESS_RIGHTS_ATTRIBUTE_ACCESS_ATTRIBUTE_ID_INDEX), "").intValue(),
                    AttributeAccessModeTypeDto.values()[this.dlmsHelperService.readLong(
                            attributeAccessItem.get(ACCESS_RIGHTS_ATTRIBUTE_ACCESS_ACCESS_MODE_INDEX), "").intValue()],
                    asl));
        }

        return new AttributeAccessDescriptorDto(attributeAccessItems);
    }

    private List<Integer> convertAccessSelectors(final List<DataObject> accessSelectors)
            throws ProtocolAdapterException {
        final List<Integer> convertedAccessSelectors = new ArrayList<>();
        for (final DataObject accessSelectorRaw : accessSelectors) {
            convertedAccessSelectors.add(this.dlmsHelperService.readLong(accessSelectorRaw, "").intValue());
        }
        return convertedAccessSelectors;
    }

    private MethodAccessDescriptorDto convertMethodAttributeAccessDescriptor(
            final List<DataObject> methodAccessDescriptor) throws ProtocolAdapterException {
        final List<MethodAccessItemDto> methodAccessItems = new ArrayList<>();

        for (final DataObject methodAccessItemRaw : methodAccessDescriptor) {
            @SuppressWarnings("unchecked")
            final List<DataObject> methodAccessItem = (List<DataObject>) methodAccessItemRaw.getValue();
            methodAccessItems.add(new MethodAccessItemDto(this.dlmsHelperService.readLong(
                    methodAccessItem.get(ACCESS_RIGHTS_METHOD_ACCESS_METHOD_ID_INDEX), "").intValue(),
                    MethodAccessModeTypeDto.values()[this.dlmsHelperService.readLong(
                            methodAccessItem.get(ACCESS_RIGHTS_METHOD_ACCESS_ACCESS_MODE_INDEX), "").intValue()]));
        }

        return new MethodAccessDescriptorDto(methodAccessItems);
    }
}
