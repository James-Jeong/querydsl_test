package study.querydsl;

import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.EntityPath;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;
import java.util.List;

import static com.querydsl.jpa.JPAExpressions.*;
import static org.assertj.core.api.Assertions.assertThat;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

    @Autowired
    private EntityManager entityManager;
    JPAQueryFactory queryFactory;

    @BeforeEach
    public void beforeTest() {
         queryFactory = new JPAQueryFactory(entityManager);

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
    }

    @Test
    public void startJPQL() throws Exception {
        // 1) Given
        String queryStr = "select m from Member m where m.username = :username";
        Member member = entityManager.createQuery(
                        queryStr,
                        Member.class
                )
                .setParameter("username", "member1")
                .getSingleResult();

        // 2) When
        // 3) Then
        assertThat(member.getUsername()).isEqualTo("member1");
    }

    @Test
    public void startQueryDSL() throws Exception {
        // 1) Given
        //QMember m = new QMember("m");
        QMember m = member;

        // 2) When
        Member member = queryFactory
                .select(m)
                .from(m)
                .where(m.username.eq("member1")) // 파라미터 자동 바인딩됨
                .fetchOne();

        // 3) Then
        assertThat(member.getUsername()).isEqualTo("member1");
    }

    @Test
    public void search() throws Exception {
        // 1) Given
        Member member1 = queryFactory
                .selectFrom(member)
                .where(
                        member.username.eq("member1"),
                        (member.age.eq(10))
                        /*member.username.eq("member1")
                        .and(member.age.eq(10))*/
                )
                .fetchOne();

        // 2) When

        // 3) Then
        assertThat(member1.getUsername()).isEqualTo("member1");
    }

    @Test
    public void resultFetch() throws Exception {
        // 1) Given
        List<Member> members = queryFactory.selectFrom(member).fetch();

        Member member1 = queryFactory.selectFrom(member).fetchOne();

        Member member2 = queryFactory.selectFrom(member).fetchFirst();

        // Mind that for any scenario where the count is not strictly needed separately,
        // we recommend to use fetch() instead.
        QueryResults<Member> memberQueryResults = queryFactory.selectFrom(member).fetchResults();
        memberQueryResults.getTotal();
        List<Member> results = memberQueryResults.getResults();

        // 2) When

        // 3) Then

    }

    /**
     * 회원 정렬 순서
     * 1. 회원 나이 내림차순
     * 2. 회원 이름 올림차순
     * 단 2번에서 회원 이름이 없으면 마지막에 출력(이름이 없으면 맨 마지막으로)
     */
    @Test
    public void sort() throws Exception {
        // 1) Given
        entityManager.persist(new Member(null, 100));
        entityManager.persist(new Member("member5", 100));
        entityManager.persist(new Member("member6", 100));

        // 2) When
        List<Member> members = queryFactory
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                .fetch();

        // 3) Then
        Member member5 = members.get(0);
        Member member6 = members.get(1);
        Member memberNull = members.get(2);

        assertThat(member5.getUsername()).isEqualTo("member5");
        assertThat(member6.getUsername()).isEqualTo("member6");
        assertThat(memberNull.getUsername()).isNull();
    }

    @Test
    public void paging1() throws Exception {
        // 1) Given
        List<Member> members = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(2)
                .limit(2)
                .fetch();

        // 2) When


        // 3) Then
        for (Member member1 : members) {
            System.out.println("member1 = " + member1);
        }

        assertThat(members.size()).isEqualTo(2);
    }

    @Test
    public void aggregation() throws Exception {
        // 1) Given
        // > 실제로는 tuple 로 안쓰고 DTO 로 뽑아온다.
        List<Tuple> tuples = queryFactory
                .select(
                        member.count(),
                        member.age.sum(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min()
                )
                .from(member)
                .fetch();

        // 2) When
        Tuple tuple = tuples.get(0);

        // 3) Then
        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.sum())).isEqualTo(100);
        assertThat(tuple.get(member.age.avg())).isEqualTo(25);
        assertThat(tuple.get(member.age.max())).isEqualTo(40);
        assertThat(tuple.get(member.age.min())).isEqualTo(10);
    }

    /**
     * 팀의 이름과 각 팀의 평균 연령을 구해라
     */
    @Test
    public void group() throws Exception {
        // 1) Given
        List<Tuple> tuples = queryFactory
                .select(
                        team.name,
                        member.age.avg()
                )
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .fetch();

        // 2) When
        Tuple teamATuple = tuples.get(0);
        Tuple teamBTuple = tuples.get(1);

        // 3) Then
        assertThat(teamATuple.get(team.name)).isEqualTo("teamA");
        assertThat(teamATuple.get(member.age.avg())).isEqualTo(15);

        assertThat(teamBTuple.get(team.name)).isEqualTo("teamB");
        assertThat(teamBTuple.get(member.age.avg())).isEqualTo(35);
    }

    /**
     * 팀 A 에 소속된 모든 회원
     */
    @Test
    public void join() throws Exception {
        // 1) Given
        List<Member> members = queryFactory
                .selectFrom(member)
                .leftJoin(member.team, team)
                //.join(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();

        // 2) When

        // 3) Then
        assertThat(members)
                .extracting("username")
                .containsExactly("member1", "member2");
    }

    /**
     * 세타 조인 (막 조인 == 연관관계가 없는 조인, NoSQL 느낌)
     *
     * 회원의 이름이 팀 이름과 같은 회원을 조회
     */
    @Test
    public void theta_join() throws Exception {
        // 1) Given
        entityManager.persist(new Member("teamA"));
        entityManager.persist(new Member("teamB"));
        entityManager.persist(new Member("teamC"));

        // 2) When
        List<Member> members = queryFactory
                .select(member)
                .from(member, team)
                .where(member.username.eq(team.name))
                .fetch();

        // 3) Then
        assertThat(members)
                .extracting("username")
                .containsExactly("teamA", "teamB");
    }

    /**
     * 회원과 팀을 조인하면서, 팀 이름이 teamA 인 팀만 조인, 회원은 모두 조회
     * JPQL : select m from Member m left join m.team t on t.name = "teamA"
     */
    @Test
    public void join_on_filtering() throws Exception {
        // 1) Given
        List<Tuple> teamA = queryFactory
                .select(member, team)
                .from(member)

                // 내부 조인이면 where 절로 필터링
                /*.join(member.team, team)
                .where(team.name.eq("teamA"))*/

                // 외부 조인이면 on 절로 필터링
                .leftJoin(member.team, team)
                .on(team.name.eq("teamA"))

                .fetch();

        // 2) When

        // 3) Then
        // Left outer join
        for (Tuple tuple : teamA) {
            System.out.println("tuple = " + tuple);
        }
        /**
         * tuple = [Member{id=3, username='member1', age=10}, Team{id=1, name='teamA'}]
         * tuple = [Member{id=4, username='member2', age=20}, Team{id=1, name='teamA'}]
         * tuple = [Member{id=5, username='member3', age=30}, null]
         * tuple = [Member{id=6, username='member4', age=40}, null]
         */
    }

    /**
     * 연관관계가 없는 엔티티 외부 조인
     *
     * 회원의 이름이 팀 이름과 같은 대상 외부 조인
     */
    @Test
    public void join_on_no_relation() throws Exception {
        // 1) Given
        entityManager.persist(new Member("teamA"));
        entityManager.persist(new Member("teamB"));
        entityManager.persist(new Member("teamC"));

        // 2) When
        List<Tuple> members = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(team)
                .on(member.username.eq(team.name))
                .fetch();

        // 3) Then
        for (Tuple tuple : members) {
            System.out.println("tuple = " + tuple);
        }
    }

    @PersistenceUnit
    EntityManagerFactory entityManagerFactory;

    @Test
    public void fetchJoinNo() throws Exception {
        // 1) Given
        entityManager.flush();
        entityManager.clear();

        // 2) When
        // Member 만 조회됨 > Team 은 Lazy 상태임
        Member member1 = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        // 3) Then
        boolean loaded = entityManagerFactory.getPersistenceUnitUtil().isLoaded(member1.getTeam());
        assertThat(loaded).as("패치 조인이 미적용이면 Team 은 영속성 컨텍스트에 로딩되면 안된다.").isFalse();
    }

    @Test
    public void fetchJoinUse() throws Exception {
        // 1) Given
        entityManager.flush();
        entityManager.clear();

        // 2) When
        // Member 만 조회됨 > Team 은 Lazy 상태임
        Member member1 = queryFactory
                .selectFrom(member)
                .join(member.team, team).fetchJoin()
                .where(member.username.eq("member1"))
                .fetchOne();

        // 3) Then
        boolean loaded = entityManagerFactory.getPersistenceUnitUtil().isLoaded(member1.getTeam());
        assertThat(loaded).as("패치 조인이 적용되면 Team 은 영속성 컨텍스트에 로딩되어야 한다.").isTrue();
    }

    /**
     * 나이가 가장 많은 회원 조회
     */
    @Test
    public void subQueryEq() throws Exception {
        // 1) Given
        QMember memberSub = new QMember("memberSub");

        // 2) When
        List<Member> members = queryFactory
                .selectFrom(member)
                .where(member.age.eq(
                        // SUB QUERY
                        select(memberSub.age.max())
                                .from(memberSub)
                        // SUB QUERY
                ))
                .fetch();

        // 3) Then
        assertThat(members)
                .extracting("age")
                .containsExactly(40);
    }

    /**
     * 나이가 평균 이상인 회원 조회
     */
    @Test
    public void subQueryGoe() throws Exception {
        // 1) Given
        QMember memberSub = new QMember("memberSub");

        // 2) When
        List<Member> members = queryFactory
                .selectFrom(member)
                .where(member.age.goe(
                        // SUB QUERY
                        select(memberSub.age.avg())
                                .from(memberSub)
                        // SUB QUERY
                ))
                .fetch();

        // 3) Then
        assertThat(members)
                .extracting("age")
                .containsExactly(30, 40);
    }

    /**
     * 나이가 10살 이상인 회원 조회
     */
    @Test
    public void subQueryIn() throws Exception {
        // 1) Given
        QMember memberSub = new QMember("memberSub");

        // 2) When
        List<Member> members = queryFactory
                .selectFrom(member)
                .where(member.age.in(
                        // SUB QUERY
                        select(memberSub.age)
                                .from(memberSub)
                        // SUB QUERY
                                .where(memberSub.age.gt(10))
                ))
                .fetch();

        // 3) Then
        assertThat(members)
                .extracting("age")
                .containsExactly(20, 30, 40);
    }

    /**
     * 회원 이름과 평균 나이 같이 출력
     */
    @Test
    public void selectSubQuery() throws Exception {
        // 1) Given
        QMember memberSub = new QMember("memberSub");
        //QMember memberSub2 = new QMember("memberSub2");

        // 2) When
        List<Tuple> members = queryFactory
                .select(member.username,
                        select(memberSub.age.avg())
                                .from(memberSub)
                        /**
                         * ! From 절의 Sub query 는 JPA 에서 지원하지 않기 때문에 동작하지 않는다.
                         * - 하이버네이트에서는 지원한다.
                         *
                         * from(
                         * select(memberSub2)
                         *      .from(memberSub2)
                         *      .where(memberSub2.username.eq("member2"))
                         *      )
                         */
                )
                .from(member)
                .fetch();

        // 3) Then
        for (Tuple tuple : members) {
            System.out.println("tuple = " + tuple);
        }
        /**
         * tuple = [member1, 25.0]
         * tuple = [member2, 25.0]
         * tuple = [member3, 25.0]
         * tuple = [member4, 25.0]
         */
    }

    /**
     * Case 로직은 DB 에서 하는 것 보다는 어플리케이션 레벨에서 처리하는게 맞다
     * 진짜 필요한 로직이 아니면 쓰지말자!
     */
    @Test
    public void basicCase() throws Exception {
        // 1) Given

        // 2) When
        List<String> result = queryFactory
                .select(member.age
                        .when(10).then("열살")
                        .when(20).then("스무살")
                        .otherwise("기타")
                )
                .from(member)
                .fetch();

        // 3) Then
        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    public void complexCase() throws Exception {
        // 1) Given

        // 2) When
        List<String> result = queryFactory
                .select(
                        new CaseBuilder()
                                .when(member.age.between(0, 20)).then("0~20살")
                                .when(member.age.between(21, 30)).then("21~30살")
                                .otherwise("기타")
                )
                .from(member)
                .fetch();

        // 3) Then
        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    public void constant() throws Exception {
        // 1) Given

        // 2) When
        List<Tuple> result = queryFactory
                .select(member.username,
                        Expressions.constant("A")
                )
                .from(member)
                .fetch();

        // 3) Then
        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    @Test
    public void concat() throws Exception {
        // 1) Given

        // 2) When
        List<String> result = queryFactory
                .select(member.username.concat("_")
                        .concat(member.age.stringValue())
                        /**
                         * member.age.stringValue()
                         *
                         * 문자가 아닌 다른 타입들은 stringValue() 로 문자로 변환할 수 있다.
                         * 이 방법은 ENUM 을 처리할 때도 자주 사용한다.
                         */
                )
                .from(member)
                .where(member.username.eq("member1"))
                .fetch();

        // 3) Then
        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

}
