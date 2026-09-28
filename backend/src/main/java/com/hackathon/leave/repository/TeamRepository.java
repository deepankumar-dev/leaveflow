package com.hackathon.leave.repository;

import com.hackathon.leave.model.Team;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamRepository extends JpaRepository<Team, Long> {}
