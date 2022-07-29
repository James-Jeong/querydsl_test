package study.querydsl.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.stereotype.Repository;
import study.querydsl.entity.Member;
import study.querydsl.entity.condition.MemberSearchCondition;
import study.querydsl.entity.dto.MemberTeamDto;
import study.querydsl.entity.dto.QMemberTeamDto;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.Optional;

import static org.springframework.util.StringUtils.hasText;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

@Repository
public class MemberJpaRepository {

    private final EntityManager entityManager;
    private final JPAQueryFactory jpaQueryFactory;

    /*public MemberJpaRepository(EntityManager entityManager, JPAQueryFactory jpaQueryFactory) {
        this.entityManager = entityManager;
        this.jpaQueryFactory = jpaQueryFactory;
    }*/

    public MemberJpaRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
        this.jpaQueryFactory = new JPAQueryFactory(entityManager);
    }

    public void save(Member member) {
        entityManager.persist(member);
    }

    public Optional<Member> findById(Long id) {
        Member member = entityManager.find(Member.class, id);
        return Optional.ofNullable(member);
    }

    public List<Member> findAll() {
        return entityManager.createQuery(
                "select m from Member m",
                Member.class
        ).getResultList();
    }

    public List<Member> findAll_QueryDSL() {
        return jpaQueryFactory
                .selectFrom(member)
                .fetch();
    }

    public List<Member> findByUsername(String username) {
        return entityManager.createQuery(
                        "select m from Member m where m.username = :username",
                        Member.class
                ).setParameter("username", username)
                .getResultList();
    }

    public List<Member> findByUsername_QueryDSL(String username) {
        return jpaQueryFactory
                .selectFrom(member)
                .where(member.username.eq(username))
                .fetch();
    }

    public List<MemberTeamDto> searchByBuilder(MemberSearchCondition memberSearchCondition) {
        BooleanBuilder booleanBuilder = new BooleanBuilder();
        if (hasText(memberSearchCondition.getUsername())) {
            booleanBuilder.and(member.username.eq(memberSearchCondition.getUsername()));
        }
        if (hasText(memberSearchCondition.getTeamName())) {
            booleanBuilder.and(team.name.eq(memberSearchCondition.getTeamName()));
        }
        if (memberSearchCondition.getAgeGoe() != null) {
            booleanBuilder.and(member.age.goe(memberSearchCondition.getAgeGoe()));
        }
        if (memberSearchCondition.getAgeLoe() != null) {
            booleanBuilder.and(member.age.loe(memberSearchCondition.getAgeLoe()));
        }

        return jpaQueryFactory
                .select(
                        new QMemberTeamDto(
                                member.id.as("memberId"),
                                member.username,
                                member.age,
                                team.id.as("teamId"),
                                team.name.as("teamName")
                        )
                )
                .from(member)
                .leftJoin(member.team, team)
                .where(booleanBuilder)
                .fetch();
    }

    public List<MemberTeamDto> search(MemberSearchCondition memberSearchCondition) {
        return jpaQueryFactory
                .select(
                        new QMemberTeamDto(
                                member.id.as("memberId"),
                                member.username,
                                member.age,
                                team.id.as("teamId"),
                                team.name.as("teamName")
                        )
                )
                .from(member)
                .leftJoin(member.team, team)
                .where(
                        isUsernameEqual(memberSearchCondition.getUsername()),
                        isTeamnameEqual(memberSearchCondition.getTeamName()),
                        isAgeGoeExist(memberSearchCondition.getAgeGoe()),
                        isAgeLoeExist(memberSearchCondition.getAgeLoe())
                )
                .fetch();
    }

    private BooleanExpression isUsernameEqual(String username) {
        return hasText(username) ? member.username.eq(username) : null;
    }

    private BooleanExpression isTeamnameEqual(String teamName) {
        return hasText(teamName) ? team.name.eq(teamName) : null;
    }

    private BooleanExpression isAgeGoeExist(Integer agGoe) {
        return agGoe != null ? member.age.goe(agGoe) : null;
    }

    private BooleanExpression isAgeLoeExist(Integer agLoe) {
        return agLoe != null ? member.age.loe(agLoe) : null;
    }

    private BooleanExpression ageBetween(Integer ageLoe, Integer ageGoe) {
        BooleanExpression isAgeLoe = isAgeLoeExist(ageLoe);
        BooleanExpression isAgeGoe = null;
        if (isAgeLoe != null) {
            isAgeGoe = isAgeLoe.and(isAgeGoeExist(ageGoe));
        }
        return isAgeGoe;
    }

}
