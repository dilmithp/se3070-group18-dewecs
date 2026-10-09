package com.group18.dewecs.repository;

import com.group18.dewecs.domain.PostEventReport;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PostEventReportRepository extends JpaRepository<PostEventReport, Long> {

    List<PostEventReport> findAllByOrderByGeneratedAtDescIdDesc();

    /** Loads the lazy warning and shelter sets in the same query, because open-in-view is off. */
    @EntityGraph(attributePaths = {"relatedWarnings", "relatedShelters"}, type = EntityGraph.EntityGraphType.LOAD)
    Optional<PostEventReport> findWithRelationsById(Long id);
}
