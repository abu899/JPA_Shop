package jpabook.jpashop.api;

import jpabook.jpashop.domain.Member;
import jpabook.jpashop.service.MemberService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import java.util.List;
import java.util.stream.Collectors;

//@Controller + @ResponseBody
@RestController
@RequiredArgsConstructor
public class MemberApiController {

    private final MemberService memberService;

    /**
     * 수정
     * 문제점(V1)
     * 엔티티의 외부노출
     * 1. 화면 validation을 위한 로직이 들어있음
     * 2. Enitity와 API 스펙이 1:1로 매핑되서, 엔티티가 변하면 api에 문제가 생김
     */
    @PostMapping("/api/v1/members")
    public CreateMemberResponse saveMemberV1(@RequestBody @Valid Member member) {
        Long id = memberService.join(member);
        return new CreateMemberResponse(id);
    }

    /**
     * 장점
     * 1. 엔티티를 변경해도, API 스펙이 바뀌지 않는다
     * 2. DTO만 보면 파라미터로 넘어오는 값을 알 수 있다
     */
    @PostMapping("/api/v2/members")
    public CreateMemberResponse saveMemberV2(@RequestBody @Valid CreateMemberRequest request) {
        Member member = new Member();
        member.setName(request.getName());

        Long id = memberService.join(member);
        return new CreateMemberResponse(id);
    }

    /**
     * 수정
     */
    @PutMapping("/api/v2/members/{id}")
    public UpdateMemberResponse updateMemberV2(@PathVariable("id") Long id,
                                               @RequestBody @Valid UpdateMemberRequest request) {
        memberService.update(id, request.getName()); // 변경 감지

        Member findMember = memberService.findMemberById(id);
        return new UpdateMemberResponse(findMember.getId(), findMember.getName());
    }

    /**
     * 조회
     * 문제점(V1)
     * 1. 엔티티 외부 노출로 인한 원치않는 외부 정보까지 노출
     * 물론 @JsonIgnore로 막아둘 수 있지만, 다수의 API 개발에 있어 모든걸 일일히 설정할 수 없다
     * 2. 바로 array로 반환하면 스펙확장에 닫혀있게된다.
     */
    @GetMapping("/api/v1/members")
    public List<Member> getMemberV1() {
        return memberService.findAllMembers();
    }

    /**
     * 장점
     * 1. 노출하고 싶은 데이터만 노출할 수 있다
     * 2. 엔티티 변경에따른 API 스펙의 변경이 없다
     * 3. API 스펙 확장에 열려있게된다.
     */
    @GetMapping("/api/v2/members")
    public Result getMemberV2() {
        List<Member> findMembers = memberService.findAllMembers();
        List<MemberDto> collect = findMembers.stream()
                .map(m -> new MemberDto(m.getName()))
                .collect(Collectors.toList());

        return new Result(collect.size(), collect);
    }

    @Data
    static class CreateMemberRequest {
        @NotEmpty
        private String name;
    }

    @Data
    static class CreateMemberResponse {
        private Long id;

        public CreateMemberResponse(Long id) {
            this.id = id;
        }
    }

    @Data
    static class UpdateMemberRequest {
        private String name;
    }

    @Data
    @AllArgsConstructor
    static class UpdateMemberResponse {
        private Long id;
        private String name;
    }

    @Data
    @AllArgsConstructor
    static class Result<T> {
        private int count;
        private T data;
    }

    @Data
    @AllArgsConstructor
    static class MemberDto {
        private String name;
    }
}
