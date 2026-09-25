package com.group18.dewecs.repository;

import com.group18.dewecs.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
