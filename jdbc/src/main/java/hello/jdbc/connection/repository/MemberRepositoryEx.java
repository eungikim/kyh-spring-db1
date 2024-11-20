package hello.jdbc.connection.repository;

import hello.jdbc.connection.domain.Member;

import java.sql.SQLException;

public interface MemberRepositoryEx { // test for throw method signature
    Member save(Member member) throws SQLException;
    Member findById(String memberId) throws SQLException;
    void update(String memberId, int money) throws SQLException;
    void delete(String memberId) throws SQLException;
}
