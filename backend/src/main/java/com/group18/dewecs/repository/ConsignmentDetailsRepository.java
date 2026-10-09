package com.group18.dewecs.repository;

import com.group18.dewecs.domain.ConsignmentDetails;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ConsignmentDetailsRepository extends JpaRepository<ConsignmentDetails, Long> {

    Optional<ConsignmentDetails> findByConsignment_Id(Long consignmentId);

    List<ConsignmentDetails> findByConsignment_IdIn(Collection<Long> consignmentIds);

    /** The other sub-batches of a split allocation (same parent). */
    List<ConsignmentDetails> findByParentConsignment_Id(Long parentId);
}
