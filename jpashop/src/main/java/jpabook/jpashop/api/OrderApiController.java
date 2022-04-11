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
     * V1 : 엔티티 직접 노출
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
     * V2 : 엔티티 조회후 DTO로 변환
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
     * V3 : fetch 조인으로 쿼리수 최적화
     *
     * join을 하면서 DB입장에서 1:N 관계(collection)에서는 N개의 갯수만큼 데이터가 늘어난다
     * 그 때 필요한게 distinct!
     * 하지만, 페이징이 불가능하다!
     *
     * 한방 쿼리가 나가지만 기준이 orders가 아닌 orderitem이 되어버린다
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
     * V3.1 : 컬렉션 페이징과 한계 극복
     * XXToOne 관계에 있는 것들만 fetch join으로 가져오고 collection은 지연로딩으로 둔다
     * orderItems(collection)는 어떻게 해야할까?
     *
     * default_batch_fetch_size 또는 @BatchSize를 통해 In Query를 통해 한번에 가져온다
     * 데이터가 전송량이 줄어들지만, 네트워크을 더 많이 사용하기는 한다!(trade-off 존재)
     * 또한 페이징이 가능해진다
     *
     * 즉, ToOne관계에서는 fetch join으로 쿼리 수를 줄이고 나머지는 fetch size를 조정하자!
     */
    @GetMapping("/api/v3.1/orders")
    public List<OrderDto> orderV3_paging() {
        List<Order> orders = orderRepository.findAllWithMemberDelivery();

        return orders.stream()
                .map( o -> new OrderDto(o))
                .collect(toList());
    }

    /**
     * DTO 방식의 선택지
     * 쿼리가 1번 실행된다고 V6가 항상 좋은 방법은 아니다.
     * V4는 코드가 단순하며 유지보수가 쉽고, 특정 주문 한건 조회라면 이 방식으로도 성능이 충분하다.
     * V5는 여러 주문을 조회 할 시 사용해야하며, V4 사용 시 1 + N의 문제가 발생한다.(= default_batch_fetch_size)
     * V6는 완전히 다른 방식이며, 쿼리 한번으로 최적화되어 좋아보이지만, Order를 기준으로 페이징이 불가능하다.
     * 실무에서는 페이징 처리가 필요한 경우가 발생하고, 중복된 데이터들로 인해 실제 성능차이가 미비할 수 있다.
     */

    /**
     * V4 : JPA에서 DTO를 직접 조회
     */
    @GetMapping("/api/v4/orders")
    public List<OrderQueryDto> orderV4() {
        return orderQueryRepository.findOrderQueryDtos();
    }

    /**
     * V5 : 컬렉션 조회 최적화
     * IN 구문을 활용해서 메모리에 미리 조회해서 최적화
     */
    @GetMapping("/api/v5/orders")
    public List<OrderQueryDto> orderV5() {
        return orderQueryRepository.findAllByDtos_opt();
    }


    /**
     * V6 : 플랫 데이터 최적화
     * JOIN 결과를 그대로 조회 후 어플리케이션에서 원하는 모양으로 직접 변환
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

    /**
     * 권장 순서
     * 1. 엔티티 조회 방식으로 우선 접근
     *  1. fetch join으로 쿼리 수를 최적화
     *  2. 컬렉션 최적화
     *      1. 페이징이 필요하다
     *          - hibernate.default_batch_fetch_size, @BatchSize로 최적화
     *      2. 페이징이 필요없다
     *          - fetch join 사용
     * 2. 엔티티 조회 방식으로 안되면 DTO 조회 방식 사용
     * 3. DTO 조회 방식으로 해결이 안되면 NativeSQL or 스프링 JdbcTemplate
     */

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
