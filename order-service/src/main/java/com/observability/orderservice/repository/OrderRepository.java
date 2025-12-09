package com.observability.orderservice.repository;

import com.observability.orderservice.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

	List<Order> findByUserId(Long userId);

	List<Order> findByStatus(Order.OrderStatus status);

	@Query("SELECT COUNT(o) FROM Order o WHERE o.status = :status")
	long countByStatus(Order.OrderStatus status);

	@Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.status = :status")
	BigDecimal sumTotalAmountByStatus(Order.OrderStatus status);

	@Query("SELECT SUM(o.totalAmount) FROM Order o")
	BigDecimal sumAllTotalAmount();
}
