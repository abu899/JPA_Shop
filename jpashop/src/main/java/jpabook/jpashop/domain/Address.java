package jpabook.jpashop.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import javax.persistence.Embeddable;

@Embeddable
@Getter
@RequiredArgsConstructor
public class Address {

    private String city;
    private String street;
    private String zipcode;
}