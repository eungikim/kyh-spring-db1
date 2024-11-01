package hello.jdbc.connection.repository;

import hello.jdbc.connection.domain.Member;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

class MemberRepositoryV0Test {

    MemberRepositoryV0 repository = new MemberRepositoryV0();

    @Test
    void crud() throws SQLException {
        Member memberV0 = new Member("memberV0", 10000);
        repository.save(memberV0);
    }
}