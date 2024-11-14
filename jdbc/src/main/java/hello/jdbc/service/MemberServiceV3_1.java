package hello.jdbc.service;

import hello.jdbc.connection.domain.Member;
import hello.jdbc.connection.repository.MemberRepositoryV3;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.sql.SQLException;


/**
 * 트랜잭션 - 트랜잭션 매니저
 */
@Slf4j
@RequiredArgsConstructor
public class MemberServiceV3_1 {
    private final PlatformTransactionManager transactionManager;
    private final MemberRepositoryV3 memberRepository;

    public void accountTransfer(String fromId, String toId, int money) {
        // 트랜잭션 시작
        TransactionStatus status = transactionManager.getTransaction(new DefaultTransactionDefinition());
        try {
            bizLogic(toId, fromId, money);
            transactionManager.commit(status); // 성공 시 커밋
        } catch (Exception e) {
            transactionManager.rollback(status); // 실패 시 롤백
            throw new IllegalStateException(e);
        }
        // release 는 자동으로 이루어짐
    }

    private void bizLogic(String toId, String fromId, int money) throws SQLException {
        // 비즈니스 로직
        Member fromMember = memberRepository.findById(fromId);
        Member toMember = memberRepository.findById(toId);

        memberRepository.update(fromId, fromMember.getMoney() - money);
        testValidation(toMember);
        memberRepository.update(toId, toMember.getMoney() + money);
    }

    private static void testValidation(Member toMember) {
        if (toMember.getMemberId().equals("ex")) {
            throw new IllegalStateException("이체중 예외 발생");
        }
    }
}
