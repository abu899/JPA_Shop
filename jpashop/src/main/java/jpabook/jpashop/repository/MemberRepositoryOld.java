package jpabook.jpashop.repository;

import jpabook.jpashop.domain.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import java.util.List;

//JPA를 사용하는 주체
@Repository
@RequiredArgsConstructor
public class MemberRepositoryOld {

    private final EntityManager em;

    public void save(Member member) {
        em.persist(em);
    }

    public Member findOne(Long id) {
        return em.find(Member.class, id);
    }

    public List<Member> findAll() {
        return em.createQuery("select m from Member m", Member.class)
                .getResultList();
    }

    public List<Member> findByName(String name) {
        return em.createQuery("select m from Member m where m.name = :username", Member.class)
                .setParameter("username", name)
                .getResultList();
    }
}
