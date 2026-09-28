package com.hackathon.leave.service;

import com.hackathon.leave.model.Delegation;
import com.hackathon.leave.model.User;
import com.hackathon.leave.repository.DelegationRepository;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** Finds who should actually hold a manager step: the delegate if an effective delegation exists. */
@Component
public class DelegationResolver {

    public record Assignment(User assignee, User delegatedFrom) {}

    private final DelegationRepository delegations;

    public DelegationResolver(DelegationRepository delegations) {
        this.delegations = delegations;
    }

    /**
     * @param requester never chosen as the delegate of their own request; if the delegate is the requester the
     *                  step stays with the original manager
     */
    public Assignment resolve(User manager, User requester, LocalDate day) {
        Optional<Delegation> d = delegations.findEffective(manager, day).stream()
                .filter(x -> !x.getDelegate().getId().equals(requester.getId())).findFirst();
        return d.map(x -> new Assignment(x.getDelegate(), manager)).orElseGet(() -> new Assignment(manager, null));
    }
}
