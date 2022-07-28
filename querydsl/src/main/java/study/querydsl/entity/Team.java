package study.querydsl.entity;

import lombok.*;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

@Getter
@Setter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Team {

    @Id
    @GeneratedValue
    @Column(name = "team_id")
    private Long id;

    private String name;

    @OneToMany(mappedBy = "team")
    @ToString.Exclude
    private List<Member> members = new ArrayList<>();

    @Transient
    private final ReentrantLock memberLock = new ReentrantLock();

    public Team(String name) {
        this.name = name;
    }

    public void addMember(Member member) {
        if (member == null) { return; }

        memberLock.lock();
        try {
            if (!members.contains(member)) {
                members.add(member);
            }
        } catch (Exception e) {
            //ignore
        } finally {
            memberLock.unlock();
        }
    }

    @Override
    public String toString() {
        return "Team{" +
                "id=" + id +
                ", name='" + name + '\'' +
                '}';
    }
}
