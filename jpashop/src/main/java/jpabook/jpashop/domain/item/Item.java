package jpabook.jpashop.domain.item;

import jpabook.jpashop.domain.CategoryItem;
import jpabook.jpashop.exception.NotEnoughStockException;
import lombok.Getter;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn
public abstract class Item {

    @Id @GeneratedValue
    @Column(name = "item_id")
    private Long id;

    @OneToMany(mappedBy = "item")
    private List<CategoryItem> categoryItems = new ArrayList<>();

    private String name;
    private int price;
    private int stockQuantity;

    // 비지니스 로직
    // 엔티티 자체가 해결할 수 있는 것들은 엔티티안에 비지니스 로직을 작성한다

    public void addStock(int quantity) {
        stockQuantity += quantity;
    }

    public void removeStock(int quantity) {
        int restStock = stockQuantity - quantity;
        if(restStock < 0) {
            throw new NotEnoughStockException("재고가 부족합니다");
        }

        stockQuantity = restStock;
    }
}
