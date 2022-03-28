package jpabook.jpashop.domain;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Getter
public class Delivery {

    @Id @GeneratedValue
    @Column(name = "delivery_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, mappedBy = "delivery")
    @Setter
    private Order order;

    @Embedded
    @Setter
    private Address address;

    @Enumerated(EnumType.STRING)
    private DeliveryStatus status;

}
