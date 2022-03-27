package jpabook.jpashop.domain.item;

import lombok.Getter;

import javax.persistence.Entity;

@Entity
@Getter
public class Movie extends Item{

    private String actor;
    private String director;
}
