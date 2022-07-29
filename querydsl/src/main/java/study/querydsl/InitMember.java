package study.querydsl;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import study.querydsl.entity.Member;
import study.querydsl.entity.Team;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;

@Profile("local")
@Component
@RequiredArgsConstructor
public class InitMember {

    private final InitMemberService initMemberService;

    /**
     * PostConstruct 로직과 Transactional 로직은 분리되어야 한다.
     */
    @PostConstruct
    public void init() {
        initMemberService.init();
    }

    @Component
    static class InitMemberService {

        @PersistenceContext
        private EntityManager entityManager;

        @Transactional
        public void init() {
            Team teamA = new Team("teamA");
            Team teamB = new Team("teamB");
            entityManager.persist(teamA);
            entityManager.persist(teamB);

            for (int i = 0; i < 100; i++) {
                Team selectedTeam = (i % 2 == 0) ? teamA : teamB;
                Member member = new Member("member" + i, i, selectedTeam);
                entityManager.persist(member);
            }

            entityManager.flush();
            entityManager.clear();
        }

    }

}
