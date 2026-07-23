package com.unihive.repository;

import com.unihive.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data MongoDB repository for {@link User} documents.
 *
 * <p>All query methods are derived from method names — no custom
 * {@code @Query} annotations are required for v1. This interface is
 * intentionally kept minimal; additional finder methods for v2 features
 * (community membership, trust score range queries, etc.) should be
 * added here when needed.
 */
@Repository
public interface UserRepository extends MongoRepository<User, String> {

    /**
     * Finds a user by their unique username.
     *
     * @param username the username to search for
     * @return an {@link Optional} containing the user if found
     */
    Optional<User> findByUsername(String username);

    /**
     * Finds a user by their unique email address.
     *
     * @param email the email to search for
     * @return an {@link Optional} containing the user if found
     */
    Optional<User> findByEmail(String email);

    /**
     * Checks whether a username is already taken.
     *
     * @param username the username to check
     * @return {@code true} if the username exists in the database
     */
    boolean existsByUsername(String username);

    /**
     * Checks whether an email address is already registered.
     *
     * @param email the email to check
     * @return {@code true} if the email exists in the database
     */
    boolean existsByEmail(String email);

    // ─────────────────────────────────────────────────────────────
    //  V2+ Extension Points (add here when implementing Version 2)
    // ─────────────────────────────────────────────────────────────

    /*
     * List<User> findByCommunityIdsContaining(String communityId);
     * List<User> findByTrustScoreGreaterThanEqual(int minScore);
     * List<User> findByRole(String role);
     * long countByOnlineTrue();
     */
}
