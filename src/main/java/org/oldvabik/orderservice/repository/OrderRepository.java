package org.oldvabik.orderservice.repository;

import org.oldvabik.orderservice.entity.Order;
import org.oldvabik.orderservice.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    @Query("""
        SELECT o
        FROM Order o
        LEFT JOIN FETCH o.items oi
        LEFT JOIN FETCH oi.item
        WHERE o.id = :id
        """)
    Optional<Order> findByIdWithDetails(Long id);

    @Query("""
        SELECT DISTINCT o
        FROM Order o
        LEFT JOIN FETCH o.items oi
        LEFT JOIN FETCH oi.item
        """)
    Page<Order> findAllWithDetails(Pageable pageable);

    @Query("""
        SELECT DISTINCT o
        FROM Order o
        LEFT JOIN FETCH o.items oi
        LEFT JOIN FETCH oi.item
        WHERE o.id IN :ids
        """)
    Page<Order> findByIdIn(List<Long> ids, Pageable pageable);

    @Query("""
        SELECT DISTINCT o
        FROM Order o
        LEFT JOIN FETCH o.items oi
        LEFT JOIN FETCH oi.item
        WHERE o.status IN :statuses
        """)
    Page<Order> findByStatusIn(List<OrderStatus> statuses, Pageable pageable);

    @Query("""
        SELECT DISTINCT o
        FROM Order o
        LEFT JOIN FETCH o.items oi
        LEFT JOIN FETCH oi.item
        WHERE o.id IN :ids AND o.status IN :statuses
        """)
    Page<Order> findByIdInAndStatusIn(List<Long> ids, List<OrderStatus> statuses, Pageable pageable);
}
