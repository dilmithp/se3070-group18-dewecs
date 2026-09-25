package com.group18.dewecs.repository;

import com.group18.dewecs.domain.Organization;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationRepository extends JpaRepository<Organization, Long> {
}
