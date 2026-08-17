package com.mycompany.myshop.backend.infrastructure.persistence.repositories;

import com.mycompany.myshop.backend.infrastructure.persistence.entities.OrderJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderJpaRepository extends JpaRepository<OrderJpaEntity, Long> {
    Optional<OrderJpaEntity> findByOrderNumber(String orderNumber);

    List<OrderJpaEntity> findAllByOrderByOrderTimestampDesc();

    @Query("SELECT o FROM OrderJpaEntity o WHERE LOWER(o.orderNumber) LIKE LOWER(CONCAT('%', :orderNumber, '%')) ORDER BY o.orderTimestamp DESC")
    List<OrderJpaEntity> findByOrderNumberContainingIgnoreCaseOrderByOrderTimestampDesc(@Param("orderNumber") String orderNumber);
}
