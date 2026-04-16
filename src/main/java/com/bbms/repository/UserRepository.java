package com.bbms.repository;

import com.bbms.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByNid(String nid);
    boolean existsByUsername(String username);
    boolean existsByNid(String nid);

    List<User> findByRole(User.Role role);
    List<User> findByAccountStatus(User.AccountStatus status);
    List<User> findByRoleAndAccountStatus(User.Role role, User.AccountStatus status);

    @Query("SELECT COUNT(u) FROM User u WHERE u.accountStatus = 'PENDING'")
    long countPendingUsers();
}
