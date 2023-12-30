package Phase2.OrdersAndNotificationsSystem.services.order;

import Phase2.OrdersAndNotificationsSystem.models.order.Order;
import Phase2.OrdersAndNotificationsSystem.models.exceptions.GeneralException;
import Phase2.OrdersAndNotificationsSystem.repositories.database.Data;

import java.util.Optional;

public interface OrderServices {
    //TODO : where do i add the cancellation date? OrderServices or OrderController?

    default Order addOrder(Order order) throws GeneralException{
        throw new UnsupportedOperationException("Not implemented yet");
    }

    boolean cancelOrder(Order order) throws GeneralException;
    Optional<Order> getOrder(int orderID) throws GeneralException;

    Order confirmOrder(Order order) throws GeneralException;



}
