package com.mycompany.myshop.core.repositories;

import com.mycompany.myshop.core.entities.Order;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByOrderNumber(String orderNumber);

    // Order history is read one page at a time: each list query below takes the page (LIMIT/OFFSET)
    // and has a COUNT twin over the same predicate. The sort ends on id, so rows that share a
    // timestamp keep one order across pages.

    @Query("SELECT o FROM Order o ORDER BY o.orderTimestamp DESC, o.id DESC")
    List<Order> findPage(Pageable pageable);

    @Query("SELECT o FROM Order o WHERE o.owner = :owner ORDER BY o.orderTimestamp DESC, o.id DESC")
    List<Order> findPageByOwner(@Param("owner") String owner, Pageable pageable);

    long countByOwner(String owner);

    // The order-number search is a substring match on LOWER(order_number), which no b-tree index
    // can serve, so it scans the (owner-filtered) orders. Accepted for a teaching dataset; the fix,
    // if it is ever needed, is a pg_trgm GIN index or a prefix-only search on an indexed LOWER().
    @Query("SELECT o FROM Order o WHERE LOWER(o.orderNumber) LIKE LOWER(CONCAT('%', :orderNumber, '%')) ORDER BY o.orderTimestamp DESC, o.id DESC")
    List<Order> findPageByOrderNumberContaining(@Param("orderNumber") String orderNumber, Pageable pageable);

    @Query("SELECT COUNT(o) FROM Order o WHERE LOWER(o.orderNumber) LIKE LOWER(CONCAT('%', :orderNumber, '%'))")
    long countByOrderNumberContaining(@Param("orderNumber") String orderNumber);

    @Query("SELECT o FROM Order o WHERE o.owner = :owner AND LOWER(o.orderNumber) LIKE LOWER(CONCAT('%', :orderNumber, '%')) ORDER BY o.orderTimestamp DESC, o.id DESC")
    List<Order> findPageByOwnerAndOrderNumberContaining(@Param("owner") String owner, @Param("orderNumber") String orderNumber, Pageable pageable);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.owner = :owner AND LOWER(o.orderNumber) LIKE LOWER(CONCAT('%', :orderNumber, '%'))")
    long countByOwnerAndOrderNumberContaining(@Param("owner") String owner, @Param("orderNumber") String orderNumber);
}
