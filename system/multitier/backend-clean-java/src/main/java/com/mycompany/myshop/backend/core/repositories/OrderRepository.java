package com.mycompany.myshop.backend.core.repositories;

import com.mycompany.myshop.backend.core.entities.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByOrderNumber(String orderNumber);

    List<Order> findAllByOrderByOrderTimestampDesc();

    @Query("SELECT o FROM Order o WHERE LOWER(o.orderNumber) LIKE LOWER(CONCAT('%', :orderNumber, '%')) ORDER BY o.orderTimestamp DESC")
    List<Order> findByOrderNumberContainingIgnoreCaseOrderByOrderTimestampDesc(@Param("orderNumber") String orderNumber);
}
