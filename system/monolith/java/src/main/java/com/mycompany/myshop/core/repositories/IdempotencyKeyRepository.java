package com.mycompany.myshop.core.repositories;

import com.mycompany.myshop.core.entities.IdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, Long> {
    Optional<IdempotencyKey> findByOwnerAndKey(String owner, String key);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    // Only an expired row is ever deleted. A live one must survive, so that a concurrent first use
    // fails on the unique constraint instead of replacing the winner's row.
    @Query("DELETE FROM IdempotencyKey k WHERE k.owner = :owner AND k.key = :key AND k.createdAt <= :cutoff")
    int deleteExpired(@Param("owner") String owner, @Param("key") String key, @Param("cutoff") Instant cutoff);
}
