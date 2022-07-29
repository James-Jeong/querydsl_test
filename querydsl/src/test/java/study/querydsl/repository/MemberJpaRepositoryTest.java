package study.querydsl.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import study.querydsl.entity.Member;
import study.querydsl.entity.Team;
import study.querydsl.entity.condition.MemberSearchCondition;
import study.querydsl.entity.dto.MemberTeamDto;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.transaction.Transactional;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class MemberJpaRepositoryTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private MemberJpaRepository memberJpaRepository;

    @Test
    public void basicTest() throws Exception {
        // 1) Given
        Member member1 = new Member("member1", 10);

        // 2) When
        memberJpaRepository.save(member1);

        // 3) Then
        Member result = memberJpaRepository.findById(member1.getId()).orElse(null);

        assertThat(result).as("NULL 이면 안된다.").isNotNull();
        assertThat(result).as("같은 영속성 컨텍스트의 객체이어야 한다.").isEqualTo(member1);

        List<Member> members = memberJpaRepository.findAll();
        assertThat(members).as("멤버 리스트에 속해있어야 한다.").containsExactly(member1);

        List<Member> byUsername = memberJpaRepository.findByUsername(member1.getUsername());
        assertThat(byUsername).as("멤버 리스트에 속해있어야 한다.").containsExactly(member1);
    }

    @Test
    public void basicQueryDSLTest() throws Exception {
        // 1) Given
        Member member1 = new Member("member1", 10);

        // 2) When
        memberJpaRepository.save(member1);

        // 3) Then
        Member result = memberJpaRepository.findById(member1.getId()).orElse(null);

        assertThat(result).as("NULL 이면 안된다.").isNotNull();
        assertThat(result).as("같은 영속성 컨텍스트의 객체이어야 한다.").isEqualTo(member1);

        List<Member> members = memberJpaRepository.findAll_QueryDSL();
        assertThat(members).as("멤버 리스트에 속해있어야 한다.").containsExactly(member1);

        List<Member> byUsername = memberJpaRepository.findByUsername_QueryDSL(member1.getUsername());
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
        //List<MemberTeamDto> memberTeamDtos = memberJpaRepository.searchByBuilder(memberSearchCondition);
        List<MemberTeamDto> memberTeamDtos = memberJpaRepository.search(memberSearchCondition);

        // 3) Then
        assertThat(memberTeamDtos)
                .extracting("username")
                .containsExactly("member3", "member4");
    }

}
