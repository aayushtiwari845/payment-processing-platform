package com.paymentplatform.account.repository;

import com.paymentplatform.account.domain.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, UUID> {
    Optional<Account> findByEmail(String email);

    boolean existsByEmailAndIdNot(String email, UUID id);
}
