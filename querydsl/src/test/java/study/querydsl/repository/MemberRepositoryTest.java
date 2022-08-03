package study.querydsl.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.Team;
import study.querydsl.entity.condition.MemberSearchCondition;
import study.querydsl.entity.dto.MemberTeamDto;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static study.querydsl.entity.QMember.*;

@SpringBootTest
@Transactional
class MemberRepositoryTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private MemberRepository memberRepository;

    @Test
    public void basicTest() throws Exception {
        // 1) Given
        Member member1 = new Member("member1", 10);

        // 2) When
        memberRepository.save(member1);

        // 3) Then
        Member result = memberRepository.findById(member1.getId()).orElse(null);

        assertThat(result).as("NULL 이면 안된다.").isNotNull();
        assertThat(result).as("같은 영속성 컨텍스트의 객체이어야 한다.").isEqualTo(member1);

        List<Member> members = memberRepository.findAll();
        assertThat(members).as("멤버 리스트에 속해있어야 한다.").containsExactly(member1);

        List<Member> byUsername = memberRepository.findByUsername(member1.getUsername());
        assertThat(byUsername).as("멤버 리스트에 속해있어야 한다.").containsExactly(member1);
    }

    @Test
    public void searchTest() throws Exception {
        // 1) Given
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        entityManager.persist(teamA);
        entityManager.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);
        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);
        entityManager.persist(member1);
        entityManager.persist(member2);
        entityManager.persist(member3);
        entityManager.persist(member4);

        entityManager.flush();
        entityManager.clear();

        MemberSearchCondition memberSearchCondition = new MemberSearchCondition();
        /*memberSearchCondition.setAgGoe(35);
        memberSearchCondition.setAgLoe(40);*/
        memberSearchCondition.setTeamName("teamB");
        /**
         * 만약에 조건이 아무것도 없다면 아래 쿼리에서 모든 데이터를 다 가져온다. > 성능에 문제가 생길 수 있다.
         */

        // 2) When
        List<MemberTeamDto> memberTeamDtos = memberRepository.search(memberSearchCondition);

        // 3) Then
        assertThat(memberTeamDtos)
                .extracting("username")
                .containsExactly("member3", "member4");
    }

    @Test
    public void searchSimpleTest() throws Exception {
        // 1) Given
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        entityManager.persist(teamA);
        entityManager.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);
        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);
        entityManager.persist(member1);
        entityManager.persist(member2);
        entityManager.persist(member3);
        entityManager.persist(member4);

        entityManager.flush();
        entityManager.clear();

        MemberSearchCondition memberSearchCondition = new MemberSearchCondition();
        PageRequest pageRequest = PageRequest.of(0, 3);

        // 2) When
        Page<MemberTeamDto> memberTeamDtos = memberRepository.searchSimple(memberSearchCondition, pageRequest);

        // 3) Then
        assertThat(memberTeamDtos).hasSize(3);
        assertThat(memberTeamDtos)
                .extracting("username")
                .containsExactly("member1", "member2", "member3");
    }

    @Test
    public void querydslPredicateExecutorTest() throws Exception {
        // 1) Given
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        entityManager.persist(teamA);
        entityManager.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);
        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);
        entityManager.persist(member1);
        entityManager.persist(member2);
        entityManager.persist(member3);
        entityManager.persist(member4);

        entityManager.flush();
        entityManager.clear();

        // 2) When
        /**
         * 1. 조인이 안된다.
         * 2. 클라이언트(또는 컨트롤러)가 QueryDSL 에 의존해야 한다. 서비스 클래스가 QueryDSL 이라는 구현 기술에 의존해야 한다.
         * 3. 복잡한 실무환경에서 사용하기에는 한계가 명확하다.
         * 4. 단순한 비교 조건 연산만 가능
         *
         * - Pageable, Sort 모두 지원함
         */
        Iterable<Member> memberIterable = memberRepository.findAll(
                member.age.between(10, 40)
                        .and(member.username.eq("member1"))
        );

        // 3) Then
        for (Member result : memberIterable) {
            System.out.println("result = " + result);
        }
    }


}