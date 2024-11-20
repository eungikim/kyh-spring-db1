package hello.jdbc.exception.translator;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.jdbc.support.SQLErrorCodeSQLExceptionTranslator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static hello.jdbc.connection.ConnectionConst.*;

public class SpringExceptionTranslatorTest {

    private static final Logger log = LoggerFactory.getLogger(SpringExceptionTranslatorTest.class);
    DriverManagerDataSource dataSource;

    @BeforeEach
    void init() {
        dataSource = new DriverManagerDataSource(URL, USERNAME, PASSWORD);
    }

    @Test
    void sqlExceptionErrorCode() {
        String sql = "select bad member";
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = dataSource.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.executeQuery();
        } catch (SQLException e) {
            int errorCode = e.getErrorCode();
            Assertions.assertThat(errorCode).isEqualTo(42122);
            log.info("error code: {}", errorCode);
            log.info("error", e);
        } finally {
            JdbcUtils.closeStatement(pstmt);
            JdbcUtils.closeConnection(conn);
        }
    }

    @Test
    void exceptionTranslator() {
        String sql = "select bad member";
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = dataSource.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.executeQuery();
        } catch (SQLException e) {
            int errorCode = e.getErrorCode();
            Assertions.assertThat(errorCode).isEqualTo(42122);
            //org.springframework.jdbc.support.sql-error-codes.xml
            SQLErrorCodeSQLExceptionTranslator exTranslator = new SQLErrorCodeSQLExceptionTranslator(dataSource);
            DataAccessException resultEx = exTranslator.translate("select", sql, e);
            log.info("resultEx", resultEx);
            Assertions.assertThat(resultEx.getClass()).isEqualTo(BadSqlGrammarException.class);
        } finally {
            JdbcUtils.closeStatement(pstmt);
            JdbcUtils.closeConnection(conn);
        }

    }
}
