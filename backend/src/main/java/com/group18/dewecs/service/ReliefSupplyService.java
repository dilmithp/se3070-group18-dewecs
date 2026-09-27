package com.group18.dewecs.service;

import com.group18.dewecs.domain.District;
import com.group18.dewecs.domain.Organization;
import com.group18.dewecs.domain.Resource;
import com.group18.dewecs.domain.ResourceType;

import java.util.List;

public interface ReliefSupplyService {

    Resource create(Long districtId, Long organizationId, String name, String type, String unit,
                     Integer initialQuantity);

    Resource update(Long resourceId, String name, String type, String unit);

    Resource restock(Long resourceId, Integer additionalQuantity);

    Resource getById(Long resourceId);

    /** Either filter may be null to mean "no filter on that dimension". */
    List<Resource> list(ResourceType typeFilter, Long districtId);

    List<Resource> listLowStock();

    List<District> listDistricts();

    List<Organization> listOrganizations();
}
