package com.group18.dewecs.service;

import com.group18.dewecs.domain.ConsignmentStatus;
import com.group18.dewecs.domain.ReliefConsignment;
import com.group18.dewecs.domain.Resource;
import com.group18.dewecs.domain.Shelter;

import java.util.List;

public interface ReliefDistributionService {

    /** Source organization is derived from the supply's owning organization. */
    ReliefConsignment create(Long resourceId, Long shelterId, Integer quantity);

    ReliefConsignment deliver(Long distributionId);

    ReliefConsignment cancel(Long distributionId);

    ReliefConsignment getById(Long distributionId);

    /** Any filter may be null to mean "no filter on that dimension". */
    List<ReliefConsignment> list(ConsignmentStatus statusFilter, Long shelterId, Long resourceId);

    List<Resource> listSuppliesForSelection();

    List<Shelter> listSheltersForSelection();
}
