package com.mycompany.myshop.backend.infrastructure.persistence.repositories;

import com.mycompany.myshop.backend.domain.values.OrderStatus;
import com.mycompany.myshop.backend.infrastructure.persistence.entities.OrderJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface OrderJpaRepository extends JpaRepository<OrderJpaEntity, Long> {
    Optional<OrderJpaEntity> findByOrderNumber(String orderNumber);

    // One statement for the whole recall. The status = :placed guard mirrors Order.cancel(): only a
    // placed order can be cancelled, so the returned count means "placed orders this recall
    // cancelled", and re-running the recall is a no-op. flushAutomatically so a pending insert in the
    // same transaction is visible to the update; clearAutomatically so no entity already in the
    // persistence context keeps reporting the status it had before the update ran behind its back.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE OrderJpaEntity o SET o.status = :cancelled "
            + "WHERE o.sku = :sku AND o.status = :placed")
    int cancelOutstandingForSku(@Param("sku") String sku,
                                @Param("placed") OrderStatus placed,
                                @Param("cancelled") OrderStatus cancelled);

    // Same shape as cancelOutstandingForSku, over a time window instead of a SKU.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE OrderJpaEntity o SET o.status = :delivered "
            + "WHERE o.status = :placed AND o.orderTimestamp <= :cutoff")
    int deliverPlacedOlderThan(@Param("cutoff") Instant cutoff,
                               @Param("placed") OrderStatus placed,
                               @Param("delivered") OrderStatus delivered);
}
