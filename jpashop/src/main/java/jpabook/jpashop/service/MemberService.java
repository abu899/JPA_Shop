package jpabook.jpashop.service;

import jpabook.jpashop.domain.Member;
import jpabook.jpashop.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// 비지니스 로직이 들어가는 Service
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;

    //회원 가입
    @Transactional
    public Long join(Member member) {
        validateDuplicateMember(member);
        memberRepository.save(member);
        return member.getId();
    }

    public Member findMemberById(Long id) {
        return memberRepository.findById(id).get();
    }

    //회원 전체 조회
    public List<Member> findAllMembers() {
        return memberRepository.findAll();
    }

    // 비지니스 로직
    private void validateDuplicateMember(Member member) {
        List<Member> members = memberRepository.findByName(member.getName());
        if(!members.isEmpty()){
            throw new IllegalStateException("이미 존재하는 회원입니다");
        }
    }

    @Transactional
    public void update(Long id, String name) {
        Member member = memberRepository.findById(id).get();
        member.setName(name);
    }
}
