package study.querydsl.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import study.querydsl.entity.Member;

import java.util.List;

public interface MemberRepository extends
        JpaRepository<Member, Long>, // 스프링 데이터 JPA 기능 사용
        MemberRepositoryCustom, // 커스텀한 JPA 기능 사용
        QuerydslPredicateExecutor<Member> // 조건이 있는 쿼리를 조회 함수 안에 작성 가능
{

    // select m from Member m where m.username = :username
    List<Member> findByUsername(String username);

}
