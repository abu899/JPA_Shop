package jpabook.jpashop.api;

import jpabook.jpashop.domain.Address;
import jpabook.jpashop.domain.Order;
import jpabook.jpashop.domain.OrderStatus;
import jpabook.jpashop.repository.OrderRepository;
import jpabook.jpashop.repository.OrderSearch;
import jpabook.jpashop.repository.SimpleOrderQueryDto;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;


/**
 * Order
 * Order -> Member
 * Member -> Delivery
 * X To One 관계
 */
@RestController
@RequiredArgsConstructor
public class OrderSimpleApiController {

    private final OrderRepository orderRepository;

    /**
     * 엄청난 문제가 발생하는 v1
     * Order -> Member
     * Member -> Order
     * 1. 양방향 관계에서 무한 루프가 발생한다
     *  -> 양방향 중 한 쪽은 @JsonIgnore 해줘야 한다.
     * 2. fetch가 LAZY로 되는 엔티티는 Proxy 객체로 되어 있어 json을 만들 때 오류가 발생한다!
     *  -> Hibernate5Module library bean 등록
     *  -> 또는 강제로 sql을 호출해서 lazy 로딩 진행.
     * 3. 불필요한 정보가 api에 노출되버린다.
     */
    @GetMapping("/api/v1/simple-order")
    public List<Order> ordersV1() {
        List<Order> all = orderRepository.findAll(new OrderSearch());
        for (Order order : all) {
            order.getMember().getName(); // 강제로 lazy 로딩
            order.getDelivery();
        }
        return all;
    }

    /**
     * 아직 해결되지 않은 문제
     * Lazy loading으로 인한 데이터베이스 쿼리가 과도하게 호출된다
     */
    @GetMapping("/api/v2/simple-orders")
    public List<SimpleOrderDto> ordersV2() {
        // Order 2개 발견 (쿼리 1번)
        // N + 1 문제
        List<Order> orders = orderRepository.findAll(new OrderSearch());

        // Simple order dto 내 초기화(쿼리 2번)
        // 총 5번의 쿼리가 나감
        return orders.stream()
                .map(order -> new SimpleOrderDto(order))
                .collect(Collectors.toList());
    }

    /**
     * fetch join을 통해 쿼리 하나로 lazy loading에 필요할 데이터를 모두 가져온다
     */
    @GetMapping("/api/v3/simple-orders")
    public List<SimpleOrderDto> ordersV3() {
        List<Order> orders = orderRepository.findAllWithMemberDelivery();
        return orders.stream()
                .map(o -> new SimpleOrderDto(o))
                .collect(Collectors.toList());
    }

    /**
     * JPA에서 바로 DTO로 변환
     * V3보다 select로 가져오는게 적어진다(성능 최적화)
     * 다만 V3는 외부(Order)를 건드리지 않는 반면 V4는 실제로 외부 데이터를 건드리게된다
     * 따라서 V4는 원하는 걸 이미 선택해서 가져오기에 재사용성이 떨어진다.
     * trade-off가 존재
     * 하지만 select 부분이 성능에 대부분에 영향을 주지 않기에 큰 차이는 없다
     */
    @GetMapping("/api/v4/simple-orders")
    public List<SimpleOrderQueryDto> ordersV4() {
        return orderRepository.findOrderDtos();
    }

    /**
     * 쿼리 방식 선택 순서(권장)
     * 1. 엔티티를 DTO로 변환하는 방법을 선택 (V2)
     * 2. 필요시 fetch join으로 성능을 최적화 (V3)
     * 3. 그래도 안되면, DTO로 직접 조회하하는 방법 선택(V4)
     * 4. 최후로는 네이티브 SQL이나 spring jdbc template으로 직접 sql을 작성
     */

    @Data
    static class SimpleOrderDto {
        private Long orderId;
        private String name;
        private LocalDateTime orderDate;
        private OrderStatus orderStatus;
        private Address address;

        public SimpleOrderDto(Order order) {
            orderId = order.getId();
            name = order.getMember().getName(); // Lazy 로딩 초기화(쿼리 날림)
            orderDate = order.getOrderDate();
            orderStatus = order.getStatus();
            address = order.getDelivery().getAddress(); // lazy 로딩 초기화(쿼리 날림)
        }
    }

}
