# 스프링 DB 1편 - 데이터 접근 핵심 원리

## JDBC 이해

H2 데이터베이스 설정<br>
External libraies 에서 com.h2database:h2:x.x.xxx 버전 확인하여 설치 추천

JDBC 등장 이유<br>
각 데이터베이스 연결 방법을 구현한 클래스(드라이버) 마다 연결 방법이 달라 JDBC 표준 인터페이스 정의

JDBC 랩핑<br>
오래된 기술인 만큼 사용하는 방법도 복잡하다. SQL Mapper 와 ORM 기술로 한번 랩핑하게되었다.

**커넥션 연결 코드**
```java
public class DBConnectionUtil {
  public static Connection getConnection() {
    try {
      Connection connection = DriverManager.getConnection(URL, USERNAME,PASSWORD);
      log.info("get connection={}, class={}", connection, connection.getClass());
      return connection;
    } catch (SQLException e) {
      throw new IllegalStateException(e);
    }
  }
}

  // DBConnectionUtilTest.java
  @Test
  void connection() {
    Connection connection = DBConnectionUtil.getConnection();
    assertThat(connection).isNotNull();
  }
```

**JDBC 사용하여 Repository 작성**
```java

public class MemberRepositoryV0 {
  public Member save(Member member) throws SQLException {
    String sql = "insert into member(member_id, money) values(?, ?)";
    Connection con = null;
    PreparedStatement pstmt = null;
    try {
      con = getConnection();
      pstmt = con.prepareStatement(sql);
      pstmt.setString(1, member.getMemberId());
      pstmt.setInt(2, member.getMoney());
      pstmt.executeUpdate();
      return member;
    } catch (SQLException e) {
      log.error("db error", e);
      throw e;
    } finally {
      close(con, pstmt, null);
    }
  }

  private void close(Connection con, Statement stmt, ResultSet rs) {
    if (rs != null) {
      try {
        rs.close();
      } catch (SQLException e) {
        log.info("error", e);
      }
    }
    if (stmt != null) {
      // stmt close
    }
    if (con != null) {
      // con close
    }
  }
}
```

findById, update, delete 함수도 작성한다.

**테스트 코드**
```java
class MemberRepositoryV0Test {
  MemberRepositoryV0 repository = new MemberRepositoryV0();
  
  @Test
  void crud() throws SQLException {
    // save
    Member memberV0 = new Member("memberV2", 10000);
    repository.save(memberV0);
    
    // findById
    Member foundMember = repository.findById(memberV0.getMemberId());
    log.info("foundMember : {}", foundMember);
    assertThat(foundMember).isEqualTo(memberV0);
    
    // update: money: 10000 -> 20000
    repository.update(memberV0.getMemberId(), 20000);
    Member updatedMember = repository.findById(memberV0.getMemberId());
    assertThat(updatedMember.getMoney()).isEqualTo(20000);
    
    // delete
    repository.delete(memberV0.getMemberId());
    Assertions.assertThatThrownBy(() -> repository.findById(memberV0.getMemberId()))
          .isInstanceOf(NoSuchElementException.class);
  }
}
```

JDBC가 제공하는 `DriverManager` 는 라이브러리에 등록된 DB 드라이버들을 관리하고, 커넥션을 획득하는 기능을 제공한다.

## 커넥션풀과 데이터소스 이해

데이터베이스 커넥션 획득<br>
- DB드라이버가 커넥션 조회 
- DB드라이버가 DB와 TCP연결 후 인증정보 전달
- DB는 인증완료 후 내부에 세션 생성, 커넥션 연결 응답
- DB드라이버는 커넥션 객체를 생성해 클라리언트에 반환

오래걸리기 때문에 커넥션을 미리 생성해두는 커넥션 풀 방식<br>
시작하는 시점에서 커넥션을 미리 확보해둔다 기본값은 보통 10<br>
대표적인 오픈소스는 `commons-dbcp2`,`tomcat-jdbc pool`,`HikariCP(default)`

`DriverManager.getConnection()` 을 사용하다가 `HikariCP`에서 커넥션을 얻으려면 커넥션을 얻는 코드를 변경해야한다. 자바에선 이런 문제를 해결하기 위해 커넥션을 얻는 방법을 추상화 한 `DataSource`가 있다. 핵심 기능은 커넥션 조회 하나.

DriverManager 는 DataSource 를 사용하지 않고 직접 커넥션을 얻는 방법이지만 스프링은 공통적인 추상화를 위해 DriverManagerDataSource 라는 DataSource 구현체를 만들어두었다.

**DataSource 사용 코드**
```java
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
    //Connection conn3 = dataSource.getConnection();
    //Connection conn4 = dataSource.getConnection();
    log.info("conn1: {}, class:{}", conn1, conn1.getClass());
    log.info("conn2: {}, class:{}", conn2, conn2.getClass());
  }
}
```

**DatsSoruce 사용하여 Repository 작성**
```java
public class MemberRepositoryV1 {
  private final DataSource dataSource;

  public MemberRepositoryV1(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  public Member save(Member member) throws SQLException {
    String sql = "insert into member(member_id, money) values (?, ?)";
    Connection conn = null;
    PreparedStatement pstmt = null;
  
    try {
      conn = getConnection();
      pstmt = conn.prepareStatement(sql);
      pstmt.setString(1, member.getMemberId());
      pstmt.setInt(2, member.getMoney());
      pstmt.executeUpdate();
      return member;
    } catch (SQLException e) {
      log.error("db error", e);
      throw e;
    } finally {
      close(conn, pstmt, null);
    }
  }

  //findById()
  //update()
  //delete()

  private void close(Connection conn, Statement stmt, ResultSet rs) {
    JdbcUtils.closeResultSet(rs);
    JdbcUtils.closeStatement(stmt);
    JdbcUtils.closeConnection(conn); // connection pool 은 pool 에 반환한다.
  }

  private Connection getConnection() throws SQLException {
    Connection conn = dataSource.getConnection();
    log.info("get connection={}, class={}", conn, conn.getClass());;
    return conn;
  }
}
```

`DataSource`를 의존관계로 외부에서 주입받아 사용한다. 직접만든 `DBConnectionUtil`를 사용하지 않고 표준 인터페이스이기 때문에 `DriverManagerDataSource`,`HikariDataSource`등의 구현체를 변경해도 문제 없다.<br>
`JdbcUtils`사용하여 여러 리소스들을 close 할 때 편하게 종료할 수 있다.

**V1 테스트코드 변경사항**
```java
class MemberRepositoryV1Test {
  MemberRepositoryV1 repository;

  @BeforeEach
  void beforeEach() {
    // 기본 DriverManager - 항상 새로운 커넥션을 획득
    //DriverManagerDataSource dataSource = new DriverManagerDataSource(URL, USERNAME, PASSWORD);
    // 커넥션 풀링
    HikariDataSource dataSource = new HikariDataSource();
    dataSource.setJdbcUrl(URL);
    dataSource.setUsername(USERNAME);
    dataSource.setPassword(PASSWORD);
  
    repository = new MemberRepositoryV1(dataSource);
  }

  //crud()
}

```
Repository 를 만들 때 DataSource 를 만들어 DI 한다.


## 트랜잭션 이해
트랜잭션은 ACID: 원자성(Atomicity),일관성(Consistency),격리성(Isolation),지속성(Durability)을 보장해야 한다.

**트랜잭션 격리 수준 - Isolation level**
- READ UNCOMMITED(커밋되지 않은 읽기)
- READ COMMITTED(커밋된 읽기)
- REPEATABLE READ(반복 가능한 읽기)
- SERIALIZABLE(직렬화 가능)
<br>

**데이터베이스 서버 연결 구조와 DB 세션에 대해**
- 사용자가 서버 어플리케이션 혹은 DB툴로 클라이언트가 되어 DB 서버에 접근할 경우 DB 서버와 커넥션을 맺게 된다. 이 때 DB는 내부에 세션을 만든다. 해당 커넥션은 이 세션을 통해 실행된다.
- 세션은 트랜잭션을 시작하고 커밋 또는 롤백으로 트랜잭션을 종료한다. 이후 새로운 트랜잭션을 다시 시작할 수 있다.
- 사용자가 커넥션을 닫거나, DB가 세션을 강제로 종료하면 세션은 종료된다.

### 실습
이후 H2 DB 에서 `set autocommit false;`를 사용하여 세션을 2개 만들어 커밋전에 다른 세션에 데이터가 적용되지 않는 실습을 함.<br>
보통 자동 커밋 모드가 기본으로 설정된 경우가 많기 때문에, **수동 커밋 모드로 설정하는 것을 트랜잭션을 시작**한다고 표현할 수 있다.


**트랜잭션 없이 계좌이체 ServiceV1 로직 작성**
```java
@RequiredArgsConstructor
public class MemberServiceV1 {
  private final MemberRepositoryV1 memberRepository;
  
  public void accountTransfer(String fromId, String toId, int money) throws SQLException {
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

//MemberServiceV1Test.java
class MemberServiceV1Test {
  public static final String MEMBER_A = "memberA";
  public static final String MEMBER_B = "memberB";
  public static final String MEMBER_EX = "ex";

  private MemberRepositoryV1 memberRepository;
  private MemberServiceV1 memberService;

  @BeforeEach
  void before() {
    DriverManagerDataSource dataSource = new DriverManagerDataSource(URL, USERNAME, PASSWORD);
    memberRepository = new MemberRepositoryV1(dataSource);
    memberService = new MemberServiceV1(memberRepository);
  }

  @AfterEach()
  void after() throws SQLException {
    memberRepository.delete(MEMBER_A);
    memberRepository.delete(MEMBER_B);
    memberRepository.delete(MEMBER_EX);
  }

  @Test
  @DisplayName("정상 이체")
  void accountTransfer() throws SQLException {/* 생략 */}

  @Test 
  @DisplayName("이체중 예외 발생")
  void accountTransferEx() throws SQLException {
  //given
    Member memberA = new Member(MEMBER_A, 10000);
    Member memberEx = new Member(MEMBER_EX, 10000);
    memberRepository.save(memberA);
    memberRepository.save(memberEx);

    //when
    assertThatThrownBy(() -> {
      memberService.accountTransfer(memberA.getMemberId(), memberEx.getMemberId(), 2000);
    }).isInstanceOf(IllegalStateException.class);

    //then
    assertThat(memberRepository.findById(MEMBER_A).getMoney()).isEqualTo(8000);
    assertThat(memberRepository.findById(MEMBER_EX).getMoney()).isEqualTo(10000);
  }
}
```

이체중 예외 발생 테스트에서 `MEMBER_A`의 금액은 8000으로 2000원 감소되게 되지만 `MEMBER_EX`의 금액은 12000원이 아닌 10000으로 이체가 되지 않았다. `MEMBER_A`의 돈만 2000원이 날아갔다.

트랜잭션을 적용하여 V1의 문제를 해결해보자. 트랜잭션을 어디에서 시작하고 어디에서 commit으로 트랜잭션을 종료 해야할까?
<br><br>
![비즈니스 로직과 트랜잭션](images/pdf3_bizLogicAndTransaction.png)

**트랜잭션 적용한 RepositoryV2, ServiceV2**
```java
  //MemberRepositoryV2.java
  public Member findById(final Connection conn, String memberId) throws SQLException {
    String sql = "select * from member where member_id = ?";
    PreparedStatement pstmt = null;
    ResultSet rs = null;
    
    try {
      pstmt = conn.prepareStatement(sql);
      pstmt.setString(1, memberId);
      rs = pstmt.executeQuery();
      if (rs.next()) {
        Member member = new Member();
        member.setMemberId(rs.getString("member_id"));
        member.setMoney(rs.getInt("money"));
        return member;
      } else {
        throw new NoSuchElementException("Member not found memberId : " + memberId);
      }
    } catch (SQLException e) {
      log.error("db error", e);
      throw e;
    } finally {
      // 커넥션은 여기서 닫지 않는다!
      JdbcUtils.closeResultSet(rs);
      JdbcUtils.closeStatement(pstmt);
    }
  }

//MemberServiceV2.java
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
    // findById 와 update 메서드 사용
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
```

개미친 코드 탄생<br>
커넥션 유지가 필요하기에 repository 에서 커넥션을 열거나 닫지 않고 서비스에서 처리하여 파라미터로 넘겨준다.

## 스프링과 문제 해결 - 트랜잭션

## 자바 예외 이해

## 스프링과 문제 해결 - 예외 처리, 반복


















