package Phase2.OrdersAndNotificationsSystem.controllers;

import Phase2.OrdersAndNotificationsSystem.models.order.CompoundOrder;
import Phase2.OrdersAndNotificationsSystem.models.order.Order;
import Phase2.OrdersAndNotificationsSystem.models.order.SimpleOrder;
import Phase2.OrdersAndNotificationsSystem.models.exceptions.GeneralException;
import Phase2.OrdersAndNotificationsSystem.models.request_bodies.CompoundOrderRequest;
import Phase2.OrdersAndNotificationsSystem.models.request_bodies.OrderRequest;
import Phase2.OrdersAndNotificationsSystem.models.response_bodies.OrderResponse;
import Phase2.OrdersAndNotificationsSystem.repositories.database.Data;
import Phase2.OrdersAndNotificationsSystem.services.account_services.AccountServices;
import Phase2.OrdersAndNotificationsSystem.services.order.CompoundOrderServiceImpl;
import Phase2.OrdersAndNotificationsSystem.services.order.OrderServices;
import Phase2.OrdersAndNotificationsSystem.services.order.SimpleOrderServiceImpl;
import Phase2.OrdersAndNotificationsSystem.services.products.ProductServices;
import Phase2.OrdersAndNotificationsSystem.services.security.JwtTokenUtil;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

// run the server and go to http://localhost:8080/swagger-ui.html

/**
 * Controller class for managing order-related operations.
 * Provides endpoints for placing, confirming, and canceling orders.
 */
@RestController
@RequestMapping("/api/order")
public class OrderController {
    OrderServices simpleOrderServices;

    @Autowired
    JwtTokenUtil jwtTokenUtil;

    @Autowired
    ProductServices productServices;

    @Autowired
    AccountServices userService;
    OrderServices compoundOrderServices;

    /**
     * Constructor for the OrderController class.
     *
     * @param compoundOrderServices The service for managing compound orders.
     * @param orderServices         The service for managing simple orders.
     */
    public OrderController(CompoundOrderServiceImpl compoundOrderServices, SimpleOrderServiceImpl orderServices) {
        this.compoundOrderServices = compoundOrderServices;
        this.simpleOrderServices = orderServices;

    }

    /**
     * Places a simple order for a user.
     *
     * @param order      The order details provided in the request body.
     * @param authHeader The authorization header containing the JWT token.
     * @return An OrderResponse containing the ID of the placed order.
     * @throws GeneralException If there is an issue with placing the order or handling the token.
     */
    @ApiResponse(responseCode = "200", description = "Order is  added successfully and return the total price ")
    @PostMapping("/place-order")
    public OrderResponse makeOrder(@RequestBody OrderRequest order, @RequestHeader("Authorization") String authHeader) throws GeneralException {
        String username = jwtTokenUtil.getUsernameFromToken(authHeader.substring(7));
        SimpleOrder simpleOrder = new SimpleOrder();
        simpleOrder.setAccount(userService.getUserByUsername(username));
        simpleOrder.setProducts(productServices.getProductsByID(order.getProductsIDs()));
        return new OrderResponse(simpleOrderServices.addOrder(simpleOrder).getId());
    }

    /**
     * Confirms a simple order for a user.
     *
     * @param id         The ID of the order to be confirmed.
     * @param authHeader The authorization header containing the JWT token.
     * @return ResponseEntity indicating the status of the confirmation operation.
     * @throws GeneralException If there is an issue with confirming the order or handling the token.
     */
    @PostMapping("/confirm-simple-order/{id}")
    public ResponseEntity<?> confirmOrder(@PathVariable("id") Integer id, @RequestHeader("Authorization") String authHeader) throws GeneralException {
        authHeader = authHeader.substring(7);
        String username = jwtTokenUtil.getUsernameFromToken(authHeader);
        if (username == null) {
            throw new GeneralException(HttpStatus.UNAUTHORIZED, "Token is missed!");
        }
        Optional<Order> order = simpleOrderServices.getOrder(id);
        if (order.get().getStatus().equals("Confirmed")) {
            throw new GeneralException(HttpStatus.BAD_REQUEST, "Order is already confirmed!");
        }
        if (!username.equals(order.get().getAccount().getUsername())) {
            throw new GeneralException(HttpStatus.UNAUTHORIZED, "You are not authorized to confirm this order!");
        }
        if (order.isPresent()) {
            simpleOrderServices.confirmOrder(order.get());
        } else
            throw new GeneralException(HttpStatus.NOT_FOUND, "Invalid order id");
        return new ResponseEntity<>("Order is confirmed successfully", HttpStatus.OK);
    }


    /**
     * Places a compound order for a user.
     *
     * @param order      The order details provided in the request body.
     * @param authHeader The authorization header containing the JWT token.
     * @return An OrderResponse containing the ID of the confirmed compound order.
     * @throws GeneralException If there is an issue with placing the compound order or handling the token.
     */
    @ApiResponse(responseCode = "200", description = "Order is  added successfully and return the total price of the person who made the order ")
    @PostMapping("/confirm-compound-order")
    public OrderResponse makeCompoundOrder(@RequestBody CompoundOrderRequest order, @RequestHeader("Authorization") String authHeader) throws GeneralException {
        if (authHeader == null) {
            throw new GeneralException(HttpStatus.UNAUTHORIZED, "Token is missed!");
        }
        String username = jwtTokenUtil.getUsernameFromToken(authHeader.substring(7));
        CompoundOrder compoundOrder = new CompoundOrder();
        compoundOrder.setAccount(userService.getUserByUsername(username));
        for (String key : order.getOrders().keySet()) {
            Order simpleOrder = simpleOrderServices.getOrder(order.getOrders().get(key)).get();
            if (simpleOrder.getAccount().getUsername() != key) {
                throw new GeneralException(HttpStatus.UNAUTHORIZED, "You are not authorized to confirm this order!");
            }
            if (simpleOrder.getStatus().equals("Confirmed")) {
                throw new GeneralException(HttpStatus.BAD_REQUEST, "Order is already confirmed!");
            }
            compoundOrder.getOrders().add(simpleOrder);
        }
        return new OrderResponse(compoundOrderServices.confirmOrder(compoundOrder).getId());
    }

    /**
     * Retrieves details of a specific order by ID.
     *
     * @param id The ID of the order to be retrieved.
     * @return An Optional containing the order if found.
     * @throws GeneralException If there is an issue with retrieving the order.
     */

    @GetMapping("/get-order/{id}")
    public Optional<Order> getOrder(@PathVariable("id") Integer id) throws GeneralException {
        return simpleOrderServices.getOrder(id);
    }

    /**
     * Cancels a specific order by ID.
     *
     * @param id         The ID of the order to be canceled.
     * @param authHeader The authorization header containing the JWT token.
     * @return ResponseEntity indicating the status of the cancellation operation.
     * @throws GeneralException If there is an issue with canceling the order or handling the token.
     */
    @DeleteMapping("/cancel-order/{id}")
    public ResponseEntity<?> cancelOrder(@PathVariable("id") Integer id, @RequestHeader("Authorization") String authHeader) throws GeneralException {
        authHeader = authHeader.substring(7);
        String username = jwtTokenUtil.getUsernameFromToken(authHeader);
        if (username == null) {
            throw new GeneralException(HttpStatus.UNAUTHORIZED, "Token is missed!");
        }
        Optional<Order> order = simpleOrderServices.getOrder(id);
        if (order.get().getStatus().equals("Cancelled")) {
            throw new GeneralException(HttpStatus.BAD_REQUEST, "Order is already cancelled!");
        }
        if (!username.equals(order.get().getAccount().getUsername())) {
            throw new GeneralException(HttpStatus.UNAUTHORIZED, "You are not authorized to cancel this order!");
        }
        if (order.isPresent()) {
            if (order.get() instanceof CompoundOrder) {
                compoundOrderServices.cancelOrder(order.get());
            } else {
                simpleOrderServices.cancelOrder(order.get());
            }
        } else
            throw new GeneralException(HttpStatus.NOT_FOUND, "Invalid order id");
        return new ResponseEntity<>("Order is cancelled successfully", HttpStatus.OK);
    }
}