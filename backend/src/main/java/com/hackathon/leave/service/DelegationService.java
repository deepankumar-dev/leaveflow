package com.hackathon.leave.service;

import com.hackathon.leave.dto.DelegationDto;
import com.hackathon.leave.dto.DelegationRequest;
import com.hackathon.leave.exception.ApiException;
import com.hackathon.leave.model.Delegation;
import com.hackathon.leave.model.Role;
import com.hackathon.leave.model.User;
import com.hackathon.leave.repository.DelegationRepository;
import com.hackathon.leave.repository.UserRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** A manager hands their approvals to another manager for a date range (RULES section 8). */
@Service
@Transactional
public class DelegationService {

    private final DelegationRepository delegations;
    private final UserRepository users;

    public DelegationService(DelegationRepository delegations, UserRepository users) {
        this.delegations = delegations;
        this.users = users;
    }

    public DelegationDto create(Long delegatorId, DelegationRequest req) {
        User delegator = user(delegatorId);
        User delegate = users.findById(req.delegateId()).orElseThrow(() -> ApiException.notFound("Delegate not found"));
        if (delegate.getId().equals(delegator.getId())) {
            throw ApiException.badRequest("SELF_DELEGATION", "You cannot delegate to yourself");
        }
        if (delegate.getRole() != Role.MANAGER || !delegate.isActive()) {
            throw ApiException.badRequest("INVALID_DELEGATE", "The delegate must be an active manager");
        }
        if (req.fromDate().isAfter(req.toDate())) {
            throw ApiException.badRequest("INVALID_RANGE", "The end date cannot be before the start date");
        }
        Delegation d = new Delegation();
        d.setDelegator(delegator);
        d.setDelegate(delegate);
        d.setFromDate(req.fromDate());
        d.setToDate(req.toDate());
        return toDto(delegations.save(d));
    }

    @Transactional(readOnly = true)
    public List<DelegationDto> list(Long delegatorId) {
        return delegations.findByDelegatorAndActiveTrue(user(delegatorId)).stream().map(DelegationService::toDto)
                .toList();
    }

    public void revoke(Long delegatorId, Long id) {
        Delegation d = delegations.findById(id).orElseThrow(() -> ApiException.notFound("Delegation not found"));
        if (!d.getDelegator().getId().equals(delegatorId)) {
            throw ApiException.forbidden("That delegation is not yours");
        }
        d.setActive(false);
    }

    private User user(Long id) {
        return users.findById(id).orElseThrow(() -> ApiException.notFound("User not found"));
    }

    static DelegationDto toDto(Delegation d) {
        return new DelegationDto(d.getId(), d.getDelegator().getId(), d.getDelegator().getName(),
                d.getDelegate().getId(), d.getDelegate().getName(), d.getFromDate(), d.getToDate(), d.isActive());
    }
}
