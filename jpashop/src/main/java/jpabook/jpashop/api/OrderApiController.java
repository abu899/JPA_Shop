package jpabook.jpashop.api;

import jpabook.jpashop.domain.Address;
import jpabook.jpashop.domain.Order;
import jpabook.jpashop.domain.OrderItem;
import jpabook.jpashop.domain.OrderStatus;
import jpabook.jpashop.repository.OrderRepository;
import jpabook.jpashop.repository.OrderSearch;
import jpabook.jpashop.repository.order.query.OrderFlatDto;
import jpabook.jpashop.repository.order.query.OrderItemQueryDto;
import jpabook.jpashop.repository.order.query.OrderQueryDto;
import jpabook.jpashop.repository.order.query.OrderQueryRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.*;

/**
 *  X To Many 관계
 *  즉 컬렉션 조회시 문제와 해결방안
 */

@RestController
@RequiredArgsConstructor
public class OrderApiController {

    private final OrderRepository orderRepository;
    private final OrderQueryRepository orderQueryRepository;

    /**
     * 엔티티 직접 노출
     */
    @GetMapping("/api/v1/orders")
    public List<Order> ordersV1() {
        List<Order> all = orderRepository.findAll(new OrderSearch());

        /**
         * 강제 초기화
         */
        for (Order order : all) {
            order.getMember().getName();
            order.getDelivery().getAddress();

            List<OrderItem> orderItems = order.getOrderItems();
            orderItems.forEach(o -> o.getItem().getName());
        }

        return all;
    }

    /**
     * 쿼리가 어마어마하게 많이 나가게 된다..
     */
    @GetMapping("/api/v2/orders")
    public List<OrderDto> ordersV2() {
        List<Order> orders = orderRepository.findAll(new OrderSearch());

        return orders.stream()
                .map( o -> new OrderDto(o))
                .collect(toList());
    }

    /**
     * join을 하면서 DB입장에서 1:N 관계에서는 N개의 갯수만큼 데이터가 늘어난다
     * 그 때 필요한게 distinct!
     * 하지만, 페이징이 불가능하다!
     *
     * 한방 쿼리가 나가지만 페이징의 기준이 orders가 아닌 orderitem이 되어버린다
     * 즉 중복데이터가 많아진다
     */
    @GetMapping("/api/v3/orders")
    public List<OrderDto> orderV3() {
        List<Order> orders = orderRepository.findAllWithItem();

        return orders.stream()
                .map( o -> new OrderDto(o))
                .collect(toList());
    }

    /**
     * XXToOne 관계에 있는 것들만 fetch join으로 가져온다
     * orderItems는 어떻게 해야할까?
     * default_batch_fetch_size 또는 @BatchSize를 통해 In Query를 통해 한번에 가져온다
     * 데이터가 전송량이 줄어들지만, 네트워크을 더 많이 사용하기는 한다!(trade-off 존재)
     * 또한 페이징이 가능해진다
     * 즉, ToOne관계에서는 fetch join으로 쿼리 수를 줄이고 나머지는 fetch size를 조정하자!
     */
    @GetMapping("/api/v3.1/orders")
    public List<OrderDto> orderV3_paging() {
        List<Order> orders = orderRepository.findAllWithMemberDelivery();

        return orders.stream()
                .map( o -> new OrderDto(o))
                .collect(toList());
    }

    @GetMapping("/api/v4/orders")
    public List<OrderQueryDto> orderV4() {
        return orderQueryRepository.findOrderQueryDtos();
    }

    @GetMapping("/api/v5/orders")
    public List<OrderQueryDto> orderV5() {
        return orderQueryRepository.findAllByDtos_opt();
    }


    /**
     * 모든 DTO의 데이터를 하나의 DTO로 합치고 쿼리를 한번에 진행.
     * 중복된 데이터가 존재하게 된다
     */
    @GetMapping("/api/v6/orders")
    public List<OrderQueryDto> orderV6() {
        List<OrderFlatDto> flats = orderQueryRepository.findAllByDtos_flat();

        return flats.stream()
                .collect(groupingBy(o -> new OrderQueryDto(o.getOrderId(), o.getName(), o.getOrderDate(), o.getOrderStatus(), o.getAddress()),
                        mapping(o -> new OrderItemQueryDto(o.getOrderId(), o.getItemName(), o.getOrderPrice(), o.getCount()), toList())
                )).entrySet().stream()
                .map(e -> new OrderQueryDto(e.getKey().getOrderId(), e.getKey().getName(), e.getKey().getOrderDate(),
                        e.getKey().getOrderStatus(), e.getKey().getAddress(), e.getValue()))
                .collect(toList());
    }

    @Getter
    static class OrderDto {

        private Long orderId;
        private String name;
        private LocalDateTime orderDate;
        private OrderStatus orderStatus;
        private Address address;
        private List<OrderItemDto> orderItems;

        public OrderDto(Order order) {
            orderId = order.getId();
            name = order.getMember().getName();
            orderDate = order.getOrderDate();
            orderStatus = order.getStatus();
            address = order.getDelivery().getAddress();
            orderItems = order.getOrderItems()
                    .stream().map( orderItem -> new OrderItemDto(orderItem))
                    .collect(toList());
        }
    }

    @Getter
    static class OrderItemDto {

        private String itemName;
        private int orderPrice;
        private int count;

        public OrderItemDto(OrderItem orderItem) {
            itemName = orderItem.getItem().getName();
            orderPrice = orderItem.getOrderPrice();
            count = orderItem.getCount();
        }
    }
}
