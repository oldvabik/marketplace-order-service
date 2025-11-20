package org.oldvabik.orderservice.repository;

import org.oldvabik.orderservice.entity.Order;
import org.oldvabik.orderservice.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.expression.spel.ast.OpAnd;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.items WHERE o.id = :id")
    Optional<Order> findById(Long id);

    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.items WHERE o.id IN :ids")
    List<Order> findByIdIn(List<Long> ids);

    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.items WHERE o.status IN :statuses")
    List<Order> findByStatusIn(List<OrderStatus> statuses);
}
