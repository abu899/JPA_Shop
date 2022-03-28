package jpabook.jpashop.domain;

import jpabook.jpashop.domain.item.Item;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem {

    @Id @GeneratedValue
    @Column(name = "order_item_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id")
    private Item item;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    @Setter
    private Order order;

    private int orderPrice;
    private int count;

    public OrderItem(Item item, int orderPrice, int count) {
        this.item = item;
        this.orderPrice = orderPrice;
        this.count = count;

        item.removeStock(count);
    }

    // 생성 메서드
    public static OrderItem createOrderItem(Item item, int orderPrice, int count) {
        return new OrderItem(item, orderPrice, count);
    }

    // 비지니스 로직
    public void cancel() {
        getItem().addStock(getCount());
    }

    public int getTotalPrice() {
        return getOrderPrice() * getCount();
    }
}
