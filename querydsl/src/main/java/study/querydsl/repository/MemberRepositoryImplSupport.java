package study.querydsl.repository;


import com.querydsl.core.QueryResults;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPQLQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;
import org.springframework.data.support.PageableExecutionUtils;
import study.querydsl.entity.Member;
import study.querydsl.entity.condition.MemberSearchCondition;
import study.querydsl.entity.dto.MemberTeamDto;
import study.querydsl.entity.dto.QMemberTeamDto;

import java.util.List;
import java.util.Objects;

import static org.springframework.util.StringUtils.hasText;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

/**
 * MemberRepositoryImpl > JPARepository 를 상속받은 인터페이스 이름 : MemberRepository
 *
 * 클랠스 네이밍 규칙 : MemberRepository + Impl
 */
public class MemberRepositoryImplSupport extends QuerydslRepositorySupport implements MemberRepositoryCustom{

    /*private final JPAQueryFactory jpaQueryFactory;

    public MemberRepositoryImpl(EntityManager entityManager) {
        this.jpaQueryFactory = new JPAQueryFactory(entityManager);
    }*/

    public MemberRepositoryImplSupport() {
        super(Member.class);
    }

    @Override
    public List<MemberTeamDto> search(MemberSearchCondition memberSearchCondition) {
        return getMemberTeamDtos(memberSearchCondition);
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

    /**
     * 멤버 리스트와 전체 개수를 같이 조회
     */
    @Override
    public Page<MemberTeamDto> searchSimple(MemberSearchCondition memberSearchCondition, Pageable pageable) {
        //QueryResults<MemberTeamDto> memberTeamDtoQueryResults = getMemberTeamDtosByPaging(memberSearchCondition, pageable)
        QueryResults<MemberTeamDto> memberTeamDtoQueryResults = getMemberTeamDtosByPaging2(memberSearchCondition, pageable)
                .fetchResults();

        List<MemberTeamDto> results = memberTeamDtoQueryResults.getResults();
        long total = memberTeamDtoQueryResults.getTotal();

        return new PageImpl<>(results, pageable, total);
    }

    /**
     * 멤버 리스트와 전체 개수를 분리해서 조회
     */
    @Override
    public Page<MemberTeamDto> searchComplex(MemberSearchCondition memberSearchCondition, Pageable pageable) {
        //List<MemberTeamDto> memberTeamDtos = getMemberTeamDtosByPaging(memberSearchCondition, pageable)
        List<MemberTeamDto> memberTeamDtos = getMemberTeamDtosByPaging2(memberSearchCondition, pageable)
                .fetch();

        //long total = getTotal(memberSearchCondition);

        /**
         * @ Count 쿼리 최적화
         *
         * Count 쿼리가 생략 가능한 경우 생략해서 처리 > QueryDSL 에서 함수로 제공함
         *  1. 페이지 시작이면서 컨텐츠 사이즈가 페이지 사이즈보다 작을 때
         *  2. 마지막 페이지 일 때 (offset + 컨텐츠 사이즈를 더해서 전체 사이즈 구함)
         */
        return PageableExecutionUtils
                .getPage(
                        memberTeamDtos,
                        pageable,
                        () -> getMemberDtoQuery(memberSearchCondition).fetchCount()
                );
        //return new PageImpl<>(memberTeamDtos, pageable, total);
    }

    private List<MemberTeamDto> getMemberTeamDtos(MemberSearchCondition memberSearchCondition) {
        return getMemberDtoQuery(memberSearchCondition)
                .fetch();
    }

    private long getTotal(MemberSearchCondition memberSearchCondition) {
        return from(member)
                .leftJoin(member.team, team)
                .where(
                        isUsernameEqual(memberSearchCondition.getUsername()),
                        isTeamnameEqual(memberSearchCondition.getTeamName()),
                        isAgeGoeExist(memberSearchCondition.getAgeGoe()),
                        isAgeLoeExist(memberSearchCondition.getAgeLoe())
                )
                .fetchCount();
        /*return jpaQueryFactory
                .select(member)
                .from(member)
                .leftJoin(member.team, team)
                .where(
                        isUsernameEqual(memberSearchCondition.getUsername()),
                        isTeamnameEqual(memberSearchCondition.getTeamName()),
                        isAgeGoeExist(memberSearchCondition.getAgeGoe()),
                        isAgeLoeExist(memberSearchCondition.getAgeLoe())
                )
                .fetchCount();*/
    }

    private JPQLQuery<MemberTeamDto> getMemberTeamDtosByPaging(MemberSearchCondition memberSearchCondition, Pageable pageable) {
    //private JPAQuery<MemberTeamDto> getMemberTeamDtosByPaging(MemberSearchCondition memberSearchCondition, Pageable pageable) {
        return getMemberDtoQuery(memberSearchCondition)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize());
    }

    private JPQLQuery<MemberTeamDto> getMemberTeamDtosByPaging2(MemberSearchCondition memberSearchCondition, Pageable pageable) {
        JPQLQuery<MemberTeamDto> memberTeamDtoJPQLQuery = getMemberDtoQuery(memberSearchCondition);

        /**
         * Offset, Limit 을 자동으로 넣어준다
         *
         * 	query.offset(pageable.getOffset());
         *  query.limit(pageable.getPageSize());
         *
         * ! 단점
         * 1. 스프링 데이터 Sort 오류 발생
         */
        return Objects.requireNonNull(getQuerydsl()).applyPagination(pageable, memberTeamDtoJPQLQuery);
    }

    private JPQLQuery<MemberTeamDto> getMemberDtoQuery(MemberSearchCondition memberSearchCondition) {
    //private JPAQuery<MemberTeamDto> getMemberDtoQuery(MemberSearchCondition memberSearchCondition) {
        return from(member)
                .leftJoin(member.team, team)
                .where(
                        isUsernameEqual(memberSearchCondition.getUsername()),
                        isTeamnameEqual(memberSearchCondition.getTeamName()),
                        isAgeGoeExist(memberSearchCondition.getAgeGoe()),
                        isAgeLoeExist(memberSearchCondition.getAgeLoe())
                )
                .select(
                        new QMemberTeamDto(
                                member.id.as("memberId"),
                                member.username,
                                member.age,
                                team.id.as("teamId"),
                                team.name.as("teamName")
                        )
                );

        /*return jpaQueryFactory
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
                );*/
    }
}
