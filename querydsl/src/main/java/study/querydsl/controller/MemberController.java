package study.querydsl.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import study.querydsl.entity.condition.MemberSearchCondition;
import study.querydsl.entity.dto.MemberTeamDto;
import study.querydsl.repository.MemberJpaRepository;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
public class MemberController {

    private final MemberJpaRepository memberJpaRepository;

    /**
     * http://localhost:8080/v1/members?teamName=teamB&ageGoe=31&ageLoe=35&username=member31
     */
    @GetMapping("/v1/members")
    public List<MemberTeamDto> searchMemberV1(MemberSearchCondition memberSearchCondition) {
        //System.out.println("memberSearchCondition = " + memberSearchCondition);
        return memberJpaRepository.search(memberSearchCondition);
    }

}
