package com.observability.orderservice.controller;

import com.observability.orderservice.dto.OrderDTO;
import com.observability.orderservice.model.Order;
import com.observability.orderservice.service.OrderService;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*", maxAge = 3600)
public class OrderController {
    
    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);
    
    private final OrderService orderService;
    private final Tracer tracer;
    

    public OrderController(OrderService orderService, Tracer tracer) {
        this.orderService = orderService;
        this.tracer = tracer;
    }
    
    @PostMapping
    public ResponseEntity<?> createOrder(@Valid @RequestBody OrderDTO orderDTO) {
        Span span = tracer.nextSpan().name("createOrder").start();
        try {
            logger.info("POST /api/orders - Creating new order for user: {}", orderDTO.getUserId());
            span.tag("order.userId", orderDTO.getUserId().toString());
            span.tag("order.productName", orderDTO.getProductName());
            span.tag("order.quantity", orderDTO.getQuantity().toString());
            
            OrderDTO createdOrder = orderService.createOrder(orderDTO);
            span.tag("order.id", createdOrder.getId().toString());
            span.tag("order.totalAmount", createdOrder.getTotalAmount().toString());
            
            return ResponseEntity.status(HttpStatus.CREATED).body(createdOrder);
        } catch (IllegalArgumentException e) {
            logger.error("Error creating order: {}", e.getMessage());
            span.error(e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error creating order", e);
            span.error(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create order"));
        } finally {
            span.end();
        }
    }
    
    @GetMapping
    public ResponseEntity<List<OrderDTO>> getAllOrders() {
        Span span = tracer.nextSpan().name("getAllOrders").start();
        try {
            logger.info("GET /api/orders - Fetching all orders");
            List<OrderDTO> orders = orderService.getAllOrders();
            span.tag("orders.count", String.valueOf(orders.size()));
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            logger.error("Unexpected error fetching orders", e);
            span.error(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(List.of());
        } finally {
            span.end();
        }
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<?> getOrderById(@PathVariable Long id) {
        Span span = tracer.nextSpan().name("getOrderById").start();
        try {
            logger.info("GET /api/orders/{} - Fetching order", id);
            span.tag("order.id", id.toString());
            
            Optional<OrderDTO> order = orderService.getOrderById(id);
            if (order.isPresent()) {
                return ResponseEntity.ok(order.get());
            } else {
                span.tag("error", "Order not found");
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Unexpected error fetching order", e);
            span.error(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch order"));
        } finally {
            span.end();
        }
    }
    
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<OrderDTO>> getOrdersByUserId(@PathVariable Long userId) {
        Span span = tracer.nextSpan().name("getOrdersByUserId").start();
        try {
            logger.info("GET /api/orders/user/{} - Fetching orders for user", userId);
            span.tag("order.userId", userId.toString());
            
            List<OrderDTO> orders = orderService.getOrdersByUserId(userId);
            span.tag("orders.count", String.valueOf(orders.size()));
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            logger.error("Unexpected error fetching orders", e);
            span.error(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(List.of());
        } finally {
            span.end();
        }
    }
    
    @GetMapping("/status/{status}")
    public ResponseEntity<List<OrderDTO>> getOrdersByStatus(@PathVariable String status) {
        Span span = tracer.nextSpan().name("getOrdersByStatus").start();
        try {
            logger.info("GET /api/orders/status/{} - Fetching orders by status", status);
            span.tag("order.status", status);
            
            try {
                Order.OrderStatus orderStatus = Order.OrderStatus.valueOf(status.toUpperCase());
                List<OrderDTO> orders = orderService.getOrdersByStatus(orderStatus);
                span.tag("orders.count", String.valueOf(orders.size()));
                return ResponseEntity.ok(orders);
            } catch (IllegalArgumentException e) {
                span.tag("error", "Invalid status");
                return ResponseEntity.badRequest().body(List.of());
            }
        } catch (Exception e) {
            logger.error("Unexpected error fetching orders by status", e);
            span.error(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(List.of());
        } finally {
            span.end();
        }
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<?> updateOrder(@PathVariable Long id, @Valid @RequestBody OrderDTO orderDTO) {
        Span span = tracer.nextSpan().name("updateOrder").start();
        try {
            logger.info("PUT /api/orders/{} - Updating order", id);
            span.tag("order.id", id.toString());
            
            OrderDTO updatedOrder = orderService.updateOrder(id, orderDTO);
            return ResponseEntity.ok(updatedOrder);
        } catch (IllegalArgumentException e) {
            logger.error("Error updating order: {}", e.getMessage());
            span.error(e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error updating order", e);
            span.error(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update order"));
        } finally {
            span.end();
        }
    }
    
    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateOrderStatus(@PathVariable Long id, @RequestBody Map<String, String> statusMap) {
        Span span = tracer.nextSpan().name("updateOrderStatus").start();
        try {
            logger.info("PATCH /api/orders/{}/status - Updating order status", id);
            span.tag("order.id", id.toString());
            
            String statusStr = statusMap.get("status");
            if (statusStr == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Status is required"));
            }
            
            try {
                Order.OrderStatus newStatus = Order.OrderStatus.valueOf(statusStr.toUpperCase());
                span.tag("order.newStatus", newStatus.toString());
                
                OrderDTO updatedOrder = orderService.updateOrderStatus(id, newStatus);
                return ResponseEntity.ok(updatedOrder);
            } catch (IllegalArgumentException e) {
                span.tag("error", "Invalid status");
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid status: " + statusStr));
            }
        } catch (IllegalArgumentException e) {
            logger.error("Error updating order status: {}", e.getMessage());
            span.error(e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error updating order status", e);
            span.error(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update order status"));
        } finally {
            span.end();
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<?> cancelOrder(@PathVariable Long id) {
        Span span = tracer.nextSpan().name("cancelOrder").start();
        try {
            logger.info("DELETE /api/orders/{} - Cancelling order", id);
            span.tag("order.id", id.toString());
            
            orderService.cancelOrder(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            logger.error("Error cancelling order: {}", e.getMessage());
            span.error(e);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Unexpected error cancelling order", e);
            span.error(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to cancel order"));
        } finally {
            span.end();
        }
    }
    
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getOrderStats() {
        Span span = tracer.nextSpan().name("getOrderStats").start();
        try {
            logger.info("GET /api/orders/stats - Fetching order statistics");
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("pending", orderService.getOrderCountByStatus(Order.OrderStatus.PENDING));
            stats.put("processing", orderService.getOrderCountByStatus(Order.OrderStatus.PROCESSING));
            stats.put("shipped", orderService.getOrderCountByStatus(Order.OrderStatus.SHIPPED));
            stats.put("delivered", orderService.getOrderCountByStatus(Order.OrderStatus.DELIVERED));
            stats.put("cancelled", orderService.getOrderCountByStatus(Order.OrderStatus.CANCELLED));
            stats.put("totalRevenue", orderService.getTotalRevenue());
            stats.put("deliveredRevenue", orderService.getRevenueByStatus(Order.OrderStatus.DELIVERED));
            
            span.tag("stats.totalRevenue", stats.get("totalRevenue").toString());
            return ResponseEntity.ok(stats);
        } finally {
            span.end();
        }
    }
}
