package com.group18.dewecs.repository;

import com.group18.dewecs.domain.RiverBasin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RiverBasinRepository extends JpaRepository<RiverBasin, Long> {

    List<RiverBasin> findAllByOrderByNameAsc();
}
