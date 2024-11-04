package hello.jdbc.connection;

import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import static hello.jdbc.connection.ConnectionConst.*;

@Slf4j
public class ConnectionTest {

    @Test
    void driverManager() throws SQLException {
        Connection conn1 = DriverManager.getConnection(URL, USERNAME, PASSWORD);
        Connection conn2 = DriverManager.getConnection(URL, USERNAME, PASSWORD);
        log.info("conn1: {}, class:{}", conn1, conn1.getClass());
        log.info("conn2: {}, class:{}", conn2, conn2.getClass());
    }

    @Test
    void dataSourceDriverManager() throws SQLException {
        //DriverManagerDataSource - 항상 새로운 커넥션을 획득
        DataSource dataSource = new DriverManagerDataSource(URL, USERNAME, PASSWORD);
        useDataSource(dataSource);
    }

    @Test
    void dataSourceConnectionPool() throws SQLException, InterruptedException {
        // 커넥션 풀링
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setConnectionTimeout(10000);
        dataSource.setJdbcUrl(URL);
        dataSource.setUsername(USERNAME);
        dataSource.setPassword(PASSWORD);
        dataSource.setMaximumPoolSize(10);
        dataSource.setPoolName("CustomPool");

        useDataSource(dataSource);
        Thread.sleep(2000);
    }

    private void useDataSource(DataSource dataSource) throws SQLException {
        Connection conn1 = dataSource.getConnection();
        Connection conn2 = dataSource.getConnection();
//        Connection conn3 = dataSource.getConnection();
//        Connection conn4 = dataSource.getConnection();
//        Connection conn5 = dataSource.getConnection();
//        Connection conn6 = dataSource.getConnection();
//        Connection conn7 = dataSource.getConnection();
//        Connection conn8 = dataSource.getConnection();
//        Connection conn9 = dataSource.getConnection();
//        Connection conn10 = dataSource.getConnection();
//        Connection conn11 = dataSource.getConnection();
        log.info("conn1: {}, class:{}", conn1, conn1.getClass());
        log.info("conn2: {}, class:{}", conn2, conn2.getClass());
    }
}
