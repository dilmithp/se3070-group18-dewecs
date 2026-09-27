package com.group18.dewecs.repository;

import com.group18.dewecs.domain.Resource;
import com.group18.dewecs.domain.ResourceType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResourceRepository extends JpaRepository<Resource, Long> {

    List<Resource> findByType(ResourceType type);

    List<Resource> findByDistrict_Id(Long districtId);

    List<Resource> findByTypeAndDistrict_Id(ResourceType type, Long districtId);

    List<Resource> findByQuantityLessThan(Integer threshold);

    long countByQuantityLessThan(Integer threshold);
}
