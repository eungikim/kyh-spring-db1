package hello.jdbc.connection.repository;

import hello.jdbc.connection.domain.Member;

import java.sql.SQLException;

public interface MemberRepository {
    Member save(Member member);
    Member findById(String memberId);
    void update(String memberId, int money);
    void delete(String memberId);
}
