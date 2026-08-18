package com.mycompany.myshop.backend.infrastructure.persistence.repositories;

import com.mycompany.myshop.backend.domain.values.OrderStatus;
import com.mycompany.myshop.backend.infrastructure.persistence.entities.OrderJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderJpaRepository extends JpaRepository<OrderJpaEntity, Long> {
    Optional<OrderJpaEntity> findByOrderNumber(String orderNumber);

    List<OrderJpaEntity> findAllByOrderByOrderTimestampDesc();

    @Query("SELECT o FROM OrderJpaEntity o WHERE LOWER(o.orderNumber) LIKE LOWER(CONCAT('%', :orderNumber, '%')) ORDER BY o.orderTimestamp DESC")
    List<OrderJpaEntity> findByOrderNumberContainingIgnoreCaseOrderByOrderTimestampDesc(@Param("orderNumber") String orderNumber);

    // One statement for the whole recall. The status <> :cancelled guard is what makes it idempotent
    // and what makes the returned count mean "orders this recall cancelled", not "orders matching the
    // SKU". flushAutomatically so a pending insert in the same transaction is visible to the update;
    // clearAutomatically so no entity already in the persistence context keeps reporting the status it
    // had before the update ran behind its back.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE OrderJpaEntity o SET o.status = :cancelled "
            + "WHERE o.sku = :sku AND o.status <> :cancelled")
    int cancelOutstandingForSku(@Param("sku") String sku,
                                @Param("cancelled") OrderStatus cancelled);

    // Same shape as cancelOutstandingForSku, over a time window instead of a SKU.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE OrderJpaEntity o SET o.status = :delivered "
            + "WHERE o.status = :placed AND o.orderTimestamp <= :cutoff")
    int deliverPlacedOlderThan(@Param("cutoff") Instant cutoff,
                               @Param("placed") OrderStatus placed,
                               @Param("delivered") OrderStatus delivered);
}
