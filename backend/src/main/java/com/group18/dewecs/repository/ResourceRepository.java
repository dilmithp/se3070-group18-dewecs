package com.group18.dewecs.repository;

import com.group18.dewecs.domain.Resource;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResourceRepository extends JpaRepository<Resource, Long> {
}
