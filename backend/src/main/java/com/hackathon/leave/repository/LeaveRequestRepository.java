package com.hackathon.leave.repository;

import com.hackathon.leave.model.LeaveRequest;
import com.hackathon.leave.model.LeaveStatus;
import com.hackathon.leave.model.User;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {

    List<LeaveRequest> findByEmployeeOrderByCreatedAtDesc(User employee);

    List<LeaveRequest> findByStatus(LeaveStatus status);

    /** Requests of the given employees, in any of the given statuses, overlapping [from, to]. */
    @Query("select r from LeaveRequest r where r.employee in :employees and r.status in :statuses "
            + "and r.fromDate <= :to and r.toDate >= :from")
    List<LeaveRequest> findOverlapping(@Param("employees") Collection<User> employees,
                                       @Param("statuses") Collection<LeaveStatus> statuses,
                                       @Param("from") LocalDate from, @Param("to") LocalDate to);
}
