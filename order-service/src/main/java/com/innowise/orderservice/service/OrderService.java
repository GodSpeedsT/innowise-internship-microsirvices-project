package com.innowise.orderservice.service;

import com.innowise.orderservice.dto.request.OrderCreateRequest;
import com.innowise.orderservice.dto.request.UpdateOrderRequest;
import com.innowise.orderservice.dto.response.OrderResponse;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderService {

  /**
   * Creates a new order for the specified user.
   *
   * @param request order creation request containing userId and order items
   * @return created order response
   * @throws com.innowise.orderservice.exception.EntityNotFoundException if any item is not found
   */
  OrderResponse createOrder(OrderCreateRequest request);

  /**
   * Retrieves an order by its ID.
   *
   * @param orderId order UUID
   * @return order response
   * @throws com.innowise.orderservice.exception.EntityNotFoundException if order is not found
   */
  OrderResponse getOrderById(UUID orderId);

  /**
   * Retrieves a paginated list of all orders with optional filters.
   *
   * @param from     filter orders created after this date (inclusive), nullable
   * @param to       filter orders created before this date (inclusive), nullable
   * @param status   filter by order status string (e.g. "NEW", "SHIPPED"), nullable
   * @param pageable pagination and sorting parameters
   * @return page of order responses
   */
  Page<OrderResponse> getAllOrders(LocalDateTime from, LocalDateTime to, String status,
      Pageable pageable);

  /**
   * Retrieves a paginated list of orders belonging to a specific user.
   *
   * @param userId   user UUID
   * @param pageable pagination and sorting parameters
   * @return page of order responses
   */
  Page<OrderResponse> getOrdersByUserId(UUID userId, Pageable pageable);

  /**
   * Updates an existing order (e.g. changes its status).
   *
   * @param orderId order UUID
   * @param update  update request containing new status
   * @return updated order response
   * @throws com.innowise.orderservice.exception.EntityNotFoundException if order is not found
   */
  OrderResponse updateOrder(UUID orderId, UpdateOrderRequest update);

  /**
   * Soft-deletes an order by its ID.
   *
   * @param orderId order UUID
   * @throws com.innowise.orderservice.exception.EntityNotFoundException if order is not found
   */
  void deleteOrderById(UUID orderId);

}
