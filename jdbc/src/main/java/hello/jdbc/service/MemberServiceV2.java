package hello.jdbc.service;

import hello.jdbc.connection.domain.Member;
import hello.jdbc.connection.repository.MemberRepositoryV2;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;


/**
 * 트랜잭션 - 파라미터 연동, 풀을 고려한 종료
 */
@Slf4j
@RequiredArgsConstructor
public class MemberServiceV2 {
    private final DataSource dataSource;
    private final MemberRepositoryV2 memberRepository;

    public void accountTransfer(String fromId, String toId, int money) throws SQLException {
        Connection conn = dataSource.getConnection();
        try {
            conn.setAutoCommit(false); // 트랜잭션 시작
            bizLogic(conn, toId, fromId, money);
            conn.commit(); // 성공 시 커밋
        } catch (Exception e) {
            conn.rollback(); // 실패 시 롤백
            throw new IllegalStateException(e);
        } finally {
            release(conn);
        }
    }

    private void bizLogic(Connection conn, String toId, String fromId, int money) throws SQLException {
        // 비즈니스 로직
        Member fromMember = memberRepository.findById(conn, fromId);
        Member toMember = memberRepository.findById(conn, toId);

        memberRepository.update(conn, fromId, fromMember.getMoney() - money);
        testValidation(toMember);
        memberRepository.update(conn, toId, toMember.getMoney() + money);
    }

    private static void testValidation(Member toMember) {
        if (toMember.getMemberId().equals("ex")) {
            throw new IllegalStateException("이체중 예외 발생");
        }
    }

    private static void release(Connection conn) {
        if (conn != null) {
            try {
                conn.setAutoCommit(true); // 커넥션 풀 고려
                conn.close();
            } catch (Exception e) {
                log.info("error", e);
            }
        }
    }

}
