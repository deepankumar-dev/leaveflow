package com.hackathon.leave.repository;

import com.hackathon.leave.model.Role;
import com.hackathon.leave.model.Team;
import com.hackathon.leave.model.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    List<User> findByTeam(Team team);
    List<User> findByRole(Role role);
}
