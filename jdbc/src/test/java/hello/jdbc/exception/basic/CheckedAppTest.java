package hello.jdbc.exception.basic;

import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.ConnectException;
import java.sql.SQLException;

@Slf4j
public class CheckedAppTest {

    @Test
    void checked() {
        Controller controller = new Controller();
//        controller.request();
        Assertions.assertThatThrownBy(() -> controller.request())
                .isInstanceOf(ConnectException.class);
    }

    static class Controller {
        Service service = new Service();

        public void request() throws Exception {
            service.logic();
        }
    }

    static class Service {
        Repository repository = new Repository();
        NetworkClient networkClient = new NetworkClient();

        public void logic() throws Exception {
            try {
                repository.call();
            } catch (SQLException e) {
                log.info("SQLException caught");
            }
            log.info("ConnectException will fire");
            networkClient.call();
        }
    }

    static class NetworkClient {
        public void call() throws ConnectException {
            throw new ConnectException("연결 실패");
        }
    }

    static class Repository {
        public void call() throws SQLException {
            throw new SQLException("ex");
        }

    }
}
