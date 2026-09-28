package com.hackathon.leave.repository;

import com.hackathon.leave.model.Delegation;
import com.hackathon.leave.model.User;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DelegationRepository extends JpaRepository<Delegation, Long> {

    List<Delegation> findByDelegatorAndActiveTrue(User delegator);

    @Query("select d from Delegation d where d.delegator = :delegator and d.active = true "
            + "and d.fromDate <= :day and d.toDate >= :day")
    List<Delegation> findEffective(@Param("delegator") User delegator, @Param("day") LocalDate day);
}
