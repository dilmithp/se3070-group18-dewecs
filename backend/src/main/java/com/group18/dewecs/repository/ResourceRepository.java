package com.group18.dewecs.repository;

import com.group18.dewecs.domain.Resource;
import com.group18.dewecs.domain.ResourceType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ResourceRepository extends JpaRepository<Resource, Long> {

    List<Resource> findByType(ResourceType type);

    List<Resource> findByDistrict_Id(Long districtId);

    List<Resource> findByTypeAndDistrict_Id(ResourceType type, Long districtId);

    List<Resource> findByQuantityLessThan(Integer threshold);

    long countByQuantityLessThan(Integer threshold);

    /** Locks the row until the transaction ends, so two dispatches cannot both spend the same stock. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Resource r WHERE r.id = :id")
    Optional<Resource> findByIdForUpdate(@Param("id") Long id);
}
