package study.querydsl.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import study.querydsl.entity.Member;
import study.querydsl.entity.condition.MemberSearchCondition;
import study.querydsl.repository.support.Querydsl4RepositorySupport;

import java.util.List;

import static org.springframework.util.StringUtils.hasText;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

public class MemberTestRepository extends Querydsl4RepositorySupport {


    public MemberTestRepository() {
        super(Member.class);
    }

    public List<Member> basicSelect() {
        return select(member)
                .from(member)
                .fetch();
    }

    public List<Member> basicSelectFrom() {
        return selectFrom(member)
                .fetch();
    }

    public Page<Member> searchPageByApplyPage(MemberSearchCondition memberSearchCondition, Pageable pageable) {
        JPAQuery<Member> query = selectFrom(member)
                .leftJoin(member.team, team)
                .where(
                        isUsernameEqual(memberSearchCondition.getUsername()),
                        isTeamnameEqual(memberSearchCondition.getTeamName()),
                        isAgeGoeExist(memberSearchCondition.getAgeGoe()),
                        isAgeLoeExist(memberSearchCondition.getAgeLoe())
                );
        List<Member> content = getQuerydsl().applyPagination(pageable, query).fetch();

        return PageableExecutionUtils.getPage(content, pageable, query::fetchCount);
    }

    /**
     * Page 쿼리와 Count 쿼리 통합
     */
    public Page<Member> applyPagination(MemberSearchCondition memberSearchCondition, Pageable pageable) {
        return applyPagination(pageable, query -> query
                .selectFrom(member)
                .leftJoin(member.team, team)
                .where(
                        isUsernameEqual(memberSearchCondition.getUsername()),
                        isTeamnameEqual(memberSearchCondition.getTeamName()),
                        isAgeGoeExist(memberSearchCondition.getAgeGoe()),
                        isAgeLoeExist(memberSearchCondition.getAgeLoe())
                )
        );
    }

    /**
     * Page 쿼리와 Count 쿼리 분리
     */
    public Page<Member> applyPagination2(MemberSearchCondition memberSearchCondition, Pageable pageable) {
        return applyPagination(pageable, contentQuery -> contentQuery
                .selectFrom(member)
                .leftJoin(member.team, team)
                .where(
                        isUsernameEqual(memberSearchCondition.getUsername()),
                        isTeamnameEqual(memberSearchCondition.getTeamName()),
                        isAgeGoeExist(memberSearchCondition.getAgeGoe()),
                        isAgeLoeExist(memberSearchCondition.getAgeLoe())
                ),
                countQuery -> countQuery
                        .select(member.id)
                        .from(member)
                        .leftJoin(member.team, team)
                        .where(
                                isUsernameEqual(memberSearchCondition.getUsername()),
                                isTeamnameEqual(memberSearchCondition.getTeamName()),
                                isAgeGoeExist(memberSearchCondition.getAgeGoe()),
                                isAgeLoeExist(memberSearchCondition.getAgeLoe())
                        )
        );
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

}
