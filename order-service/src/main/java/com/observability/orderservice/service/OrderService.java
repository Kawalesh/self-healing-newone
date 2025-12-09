package com.observability.orderservice.service;

import com.observability.orderservice.client.UserServiceClient;
import com.observability.orderservice.dto.OrderDTO;
import com.observability.orderservice.model.Order;
import com.observability.orderservice.repository.OrderRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class OrderService {

	private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

	private final OrderRepository orderRepository;
	private final UserServiceClient userServiceClient;
	private final Counter orderCreatedCounter;
	private final Counter orderUpdatedCounter;
	private final Counter orderCancelledCounter;
	private final Counter orderCompletedCounter;
	private final Timer orderOperationTimer;

	@Autowired
	public OrderService(OrderRepository orderRepository, UserServiceClient userServiceClient,
			MeterRegistry meterRegistry) {
		this.orderRepository = orderRepository;
		this.userServiceClient = userServiceClient;
		this.orderCreatedCounter = Counter.builder("order.created").description("Total number of orders created")
				.register(meterRegistry);
		this.orderUpdatedCounter = Counter.builder("order.updated").description("Total number of orders updated")
				.register(meterRegistry);
		this.orderCancelledCounter = Counter.builder("order.cancelled").description("Total number of orders cancelled")
				.register(meterRegistry);
		this.orderCompletedCounter = Counter.builder("order.completed").description("Total number of orders completed")
				.register(meterRegistry);
		this.orderOperationTimer = Timer.builder("order.operation.duration")
				.description("Time taken for order operations").register(meterRegistry);
	}

	@Transactional
	public OrderDTO createOrder(OrderDTO orderDTO) throws Exception {
		return orderOperationTimer.recordCallable(() -> {
			logger.info("Creating order for user ID: {}", orderDTO.getUserId());

			// Validate user exists
			if (!userServiceClient.userExists(orderDTO.getUserId())) {
				throw new IllegalArgumentException("User not found with ID: " + orderDTO.getUserId());
			}

			// Calculate total amount
			BigDecimal totalAmount = orderDTO.getPrice().multiply(BigDecimal.valueOf(orderDTO.getQuantity()));

			Order order = new Order();
			order.setUserId(orderDTO.getUserId());
			order.setProductName(orderDTO.getProductName());
			order.setQuantity(orderDTO.getQuantity());
			order.setPrice(orderDTO.getPrice());
			order.setTotalAmount(totalAmount);
			order.setStatus(Order.OrderStatus.PENDING);
			order.setShippingAddress(orderDTO.getShippingAddress());

			Order savedOrder = orderRepository.save(order);
			orderCreatedCounter.increment();

			logger.info("Order created successfully with ID: {}", savedOrder.getId());
			return convertToDTO(savedOrder);
		});
	}

	public List<OrderDTO> getAllOrders() throws Exception {
		return orderOperationTimer.recordCallable(() -> {
			logger.debug("Fetching all orders");
			return orderRepository.findAll().stream().map(this::convertToDTO).collect(Collectors.toList());
		});
	}

	public Optional<OrderDTO> getOrderById(Long id) throws Exception {
		return orderOperationTimer.recordCallable(() -> {
			logger.debug("Fetching order by ID: {}", id);
			return orderRepository.findById(id).map(this::convertToDTO);
		});
	}

	public List<OrderDTO> getOrdersByUserId(Long userId) throws Exception {
		return orderOperationTimer.recordCallable(() -> {
			logger.debug("Fetching orders for user ID: {}", userId);
			return orderRepository.findByUserId(userId).stream().map(this::convertToDTO).collect(Collectors.toList());
		});
	}

	public List<OrderDTO> getOrdersByStatus(Order.OrderStatus status) throws Exception {
		return orderOperationTimer.recordCallable(() -> {
			logger.debug("Fetching orders with status: {}", status);
			return orderRepository.findByStatus(status).stream().map(this::convertToDTO).collect(Collectors.toList());
		});
	}

	@Transactional
	public OrderDTO updateOrderStatus(Long id, Order.OrderStatus newStatus) throws Exception {
		return orderOperationTimer.recordCallable(() -> {
			logger.info("Updating order {} status to {}", id, newStatus);

			Order order = orderRepository.findById(id)
					.orElseThrow(() -> new IllegalArgumentException("Order not found with ID: " + id));

			Order.OrderStatus oldStatus = order.getStatus();
			order.setStatus(newStatus);

			Order updatedOrder = orderRepository.save(order);
			orderUpdatedCounter.increment();

			// Increment specific status counters
			if (newStatus == Order.OrderStatus.CANCELLED) {
				orderCancelledCounter.increment();
			} else if (newStatus == Order.OrderStatus.DELIVERED) {
				orderCompletedCounter.increment();
			}

			logger.info("Order {} status updated from {} to {}", id, oldStatus, newStatus);
			return convertToDTO(updatedOrder);
		});
	}

	@Transactional
	public OrderDTO updateOrder(Long id, OrderDTO orderDTO) throws Exception {
		return orderOperationTimer.recordCallable(() -> {
			logger.info("Updating order with ID: {}", id);

			Order order = orderRepository.findById(id)
					.orElseThrow(() -> new IllegalArgumentException("Order not found with ID: " + id));

			// Validate user exists if userId is being changed
			if (!order.getUserId().equals(orderDTO.getUserId())
					&& !userServiceClient.userExists(orderDTO.getUserId())) {
				throw new IllegalArgumentException("User not found with ID: " + orderDTO.getUserId());
			}

			order.setUserId(orderDTO.getUserId());
			order.setProductName(orderDTO.getProductName());
			order.setQuantity(orderDTO.getQuantity());
			order.setPrice(orderDTO.getPrice());

			// Recalculate total amount
			BigDecimal totalAmount = orderDTO.getPrice().multiply(BigDecimal.valueOf(orderDTO.getQuantity()));
			order.setTotalAmount(totalAmount);

			order.setShippingAddress(orderDTO.getShippingAddress());

			Order updatedOrder = orderRepository.save(order);
			orderUpdatedCounter.increment();

			logger.info("Order updated successfully with ID: {}", updatedOrder.getId());
			return convertToDTO(updatedOrder);
		});
	}

	@Transactional
	public void cancelOrder(Long id) {
		orderOperationTimer.record(() -> {
			logger.info("Cancelling order with ID: {}", id);
			try {
				updateOrderStatus(id, Order.OrderStatus.CANCELLED);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});
	}

	public long getOrderCountByStatus(Order.OrderStatus status) {
		return orderRepository.countByStatus(status);
	}

	public BigDecimal getTotalRevenue() {
		BigDecimal total = orderRepository.sumAllTotalAmount();
		return total != null ? total : BigDecimal.ZERO;
	}

	public BigDecimal getRevenueByStatus(Order.OrderStatus status) {
		BigDecimal total = orderRepository.sumTotalAmountByStatus(status);
		return total != null ? total : BigDecimal.ZERO;
	}

	private OrderDTO convertToDTO(Order order) {
		return new OrderDTO(order.getId(), order.getUserId(), order.getProductName(), order.getQuantity(),
				order.getPrice(), order.getTotalAmount(), order.getStatus(), order.getCreatedAt(), order.getUpdatedAt(),
				order.getShippingAddress());
	}
}
