package study.querydsl.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import study.querydsl.entity.condition.MemberSearchCondition;
import study.querydsl.entity.dto.MemberTeamDto;

import java.util.List;

public interface MemberRepositoryCustom {

    List<MemberTeamDto> search(MemberSearchCondition memberSearchCondition);
    Page<MemberTeamDto> searchSimple(MemberSearchCondition memberSearchCondition, Pageable pageable);
    Page<MemberTeamDto> searchComplex(MemberSearchCondition memberSearchCondition, Pageable pageable);

}
