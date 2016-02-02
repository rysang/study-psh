

# 내용정리 #

템플릿이란, 바뀌는 성질이 다른 코드 중에서 변경이 거의 일어나지 않으며 일정한 패턴으로 유지되는 특성을 가진 부분을 자유롭게 변경되는 성질을 가진 부분으로부터 독릭시켜서 효과적으로 활용할 수 있도록 하는 방법이다.

## 3.1 다시 보는 초난감 DAO ##

UserDAO 에는 아직 예외처리에 대한 문제점이 남아 있다. JDBC 코드라면 반드시 지켜야 할 리소스 반환 문제다.

그래서 우리는 try/catch/finally 구문을 사용해서 수정해보도록 한다.

```
public void deleteAll() throws SQLException {
   Connection c = null;
   PreparedStatement ps = null;

   try {
      c = dataSource.getConnection();
      ps = c.preareStatement("delete from users");
      ps.executeUpdate();
   } catch (SQLException e) {
      throw e;
   } finally {
      if(ps != null) {
         try { ps.close(); } catch (SQLException e) { }
      }
      if(c != null) {
         try { c.close(); } catch (SQLException e) { }
      }
   }
}
```

조회의 경우 ResultSet 도 마찬가지로 리소스를 반환하도록 한다.


## 3.2 변하는 것과 변하지 않는 것 ##

try/catch/finally 를 사용하고 나서 발견되는 문제점은 모든 메소드마다 해당 구문이 반복된다는 것이다.
그래서 변하는 부분을 메소드로 추출해 본다.

```
public void deleteAll() throws SQLException {
   ...
   try {
      c = dataSource.getConnection();
      ps = makeStatement(c);
      ps.executeUpdate();
   } catch (SQLException e) {
      ...
   } finally {
      ...
   }
}

private PreparedStatement makeStatement(Connection c) throws SQLException {
   PreparedStatement ps;
   ps = c.prepareStatement("delete from users");
   return ps;
}
```

하지만 이 경우 분리시킨 메소드를 다른 곳에서 재사용할수가 없다. 뭔가 거꾸로 된 듯한 느낌이다.

**템플릿 메소드 패턴의 적용**

템플릿 메소드 패턴을 적용해서 분리해보도록 한다. UserDAO의 makeStatement 메소드를 추상메소드로 두고, 각 기능을 담당하는 메소드를 서브 클래스에 적용해본다.

```
abstract protected PreparedStatement makeStatement(Connection c) throws SQLException;
```

```
public class UserDaoDeleteAll extends UserDAO {
   protected PreparedStatement makeStatement(Connection c) throws SQLException {
      PreparedStatement ps = c.prepareStatement("delete from users");
      return ps;
   }
}
```

하지만 이 방법은 DAO 로직마다 상속을 통해 새로운 클래스를 만들어야 하는 단점이 생겼다. 또한 확장구조가 이미 클래스를 설계하는 시점에서 고정되어서 유연성이 떨어진다는 점이다.

**전략 패턴(Strategy Pattern)의 적용**

템플릿 메소드 패턴보다 유연하고 확장성이 뛰어난 것이 Context(맥락)와 Strategy(전략)으로 분리해서 클래스 레벨에서는 인터페이스를 통해서만 의존하도록 만드는 전략패턴이다.

deleteAll 메소드에서 변하지 않는 부분을 contextMethod 로 두고, PrepaaredStatement 를 만들어 주는 외부 기능이 Strategy 부분이 되는 것이다.

```
...
public interface StatementStrategy {
   PreparedStatement makePreparedStatement(Connection c) throws SQLException;
}
```

```
...
public class DeleteAllStatement implements StatementStrategy {
   public PreparedStatement makePreparedStatement(Connection c) throws SQLException {
      PreparedStatement ps = c.prepareStatement("delete from users");
      return ps;
   }
}
```

```
public void deleteAll() throws SQLException {
   ...
   try {
      c = dataSource.getConnection();
      StatementStrategy strategy = new DeleteAllStatement();
      ps = strategy.makePreparedStatement(c);
      ps.executeUpdate();
   } catch {
      ...
   } finally {
      ...
   }
}
```

전략패턴을 이용해서 컨텍스트는 그대로 유지되면서 전략을 바꿔쓸 수 있게 되었다. 하지만 컨텍스트 안에서 이미 구체적인 DeleteAllStatement 클래스를 사용하도록 고정되어 있다.

**DI 적용을 위한 클라이언트/컨텍스트 분리**

전략패턴에서 컨텍스트가 어떤 전략을 따를지 결정하는 것은 클라이언트가 결정해야 한다. 그래서 클라이언트를 두려고 보니 1장에서 사용했던 오브젝트 팩토리 사용과 비슷하게 되었다.

컨텍스트에 해당하는 부분은 별도의 메소드로 독립시키고 deleteAll 메소드를 클라이언트로 사용하도록 한다. 이때 전략 인터페이스인 StatementStrategy 는 메소드의 파라미터로 지정될 것이다.

```
public void jdbcContextWithStatementStrategy(StatementStrategy stmt) throws SQLException {
   Connection c = null;
   PreparedStatement ps = null;
   try {
      c = dateSource.getConnection();
      ps = stmt.makePreparedStatement(c); // 전략 파라미터 사용
      ps.executeUpdate();
   } cateh (SQLException e) {
      ...
   } finally { 
      ...
   }
}
```

```
public void deleteall() throws SQLException {
   StatementStrategy st = new DeleteAllStatement(); // 클라이언트가 전략 생성
   jdbcContextWithStatementStrategy(st); // 호출
}
```

전략패턴의 구조를 제대로 갖춘 이 소스는 비록 클라이언트와 컨텍스트를 클래스로 분리하지는 않았지만 의존관계와 책음으로 볼 때 DI 구조라고 이해할 수도 있다. 두개의 오브젝트가 하나의 클래스 안에 담긴 이런 경우에는 DI가 매우 작은 단위의 코드와 메소드 사이에서 일어나기도 한다. 이를 코드에 의한 DI라는 의미로 수동DI 라고 부를 수도 있다.


## 3.3 JDBC 전략 패턴의 최적화 ##

이번엔 add 메소드도 전략패턴을 이용해서 개선해본다.

```
public class AddStatement implements StatementStrategy {
   User user;

   public AddStatement(User user) {
      this.user = user;
   }

   public PreparedStatement makePreparedStatement(Connection c) throws SQLException {
      PreparedStatement ps = c.prepareStatement("insert into users(id, name, password) values (?, ?, ?)");
      ps.setString(1, user.getId());
      ps.setString(2, user.getName());
      ps.setString(3, user.getPassword());
      return ps;
   }
}
```

```
public void add(User user) throws SQLException {
   StatementStrategy st = new AddStatement(user);
   jdbcContextWithStatementStrategy(st);
}
```

이 때 user 객체는 생성자를 통해 제공받도록 한다. 앞으로 비슷한 기능의 DAO 메소드가 필요할 때 마다 Strategy 전략과 jdbcContextWithStatmentStrategy 컨텍스트를 활용할 수 있게 되었다.

**전략과 클라이언트의 동거**

이렇게 개선을 했는 데도 아직 문제가 남아 있다. DAO 메소드 마다 새로운 StatementStrategy 구현 클래스를 생성해야 하는 것과 DAO 메소드에서 전달해야 할 User 같은 부가적인 정보가 있는 것이다.

AddStatementStrategy 가 쓰이는 곳은 add 메소드 한군데 이므로 클래스 파일이 많아 지는 것을 해결할 수 있도록 로컬 클래스를 사용해보도록 한다.

```
public void add(final User user) throws SQLException { // 로컬변수에 직접 접근할 수 있도록 final 로 선언
   class AddStatement implements StatementStrategy {
      public PreparedStatement makePreparedStatement(Connection c) throws SQLException {
         PreparedStatement ps = c.prepareStatement("insert into users(id, name, password) values (?, ?, ?)");
         ps.setString(1, user.getId());
         ps.setString(2, user.getName());
         ps.setString(3, user.getPassword());
         return ps;
      }
   }
   
   StatementStrategy st = new AddStatement();
   jdbcContextWithStatementStrategy(st);
}
```

이를 통해 클래스 파일을 하나 줄일 수 있고, 로컬변수를 바로 가져다 쓸수 있게 되었다.

**익명 내부 클래스(anonymous inner class)**

방금 작성된 클래스는 add 메소드에서만 사용할 용도로 만들어 졌다. 그러므로 익명 내부 클래스를 이용해서 클래스 이름도 제거해보도록 한다.


익명 내부 클래스를 사용하는 방법은 다음과 같다.

new 인터페이스이름() { 클래스 본문 };

```
public void add(final User user) throws SQLException {
   jdbcContextWithStatementStrategy(
      new StatementStrategy() {
         public PreparedStatement makePreparedStatement(Connection c) throws SQLException {
            PreparedStatement ps = c.prepareStatement("insert into users(id, name, password) values (?, ?, ?)");
            ps.setString(1, user.getId());
            ps.setString(2, user.getName());
            ps.setString(3, user.getPassword());
            return ps;
         }
      }
   );
}

public void deleteAll() throw SQLException {
   jdbcContextWithStatementStrategy(
      new StatementStrategy() {
         public PreparedStatement makePreparedStatement(Connection c) throws SQLException {
            return c.prepareStatement("delete from users");
         }
      }
   );
}
```

## 3.4 컨텍스트와 DI ##

jdbcContextWithStatementStrategy 메소드는 모든 DAO 에서 사용가능하다. 그러므로 독립적인 클래스로 분리시켜본다.

```
...
public class JdbcContext {
   private DataSource dataSource;
   public void setDataSource(DataSource dataSource) {
      this.dataSource = dataSource; // DataSource 에 의존하고 있으므로 DI 받게 해준다.
   }
   public void workWithStatementStrategy(StatementStrategy stmt) throws SQLException {
      Connection c = null;
      PreparedStatement ps = null;
      try {
         c = this.dataSource.getConnection();
         ps = stmt.makePreparedStatement(c);
         ps.executeUpdate();
      } catch (SQLException e) {
         ...
      } finally {
         ...
      }
   }
}
```

```
public class UserDao {
   ...
   private JdbcContext jdbcContext;
   public void setJdbcContext(jdbcContext) {
      this.jdbcContext = jdbcContext;
   }

   public void add(final User user) throws SQLException {
      this.jdbcContext.workWithStatementStrategy(
         new StatementStrategy() {...};
      );
   }

   public void deleteAll() throws SQLException {
      this.jdbcContext.workWithStatementStrategy(
         new StatementStrategy() {...};
      );
   }
}
```

UserDao 는 JdbcContext 에 의존하고 있으므로 DI 받을 수 있도록 한다.

**빈 의존관계 변경**

바뀐 코드를 따라서 ApplicationContext 설정도 바뀌어야 한다.

```
<beans>
   <bean id="userDao" class="springbook...UserDao">
      <property name="jdbcContext" ref="jdbcContext" />
   </bean>

   <bean id="jdbcContext" class="springbook...JdbcContext">
      <property name="dataSource" ref="dataSource" />
   </bean>

   <bean id="dataSource" class="org....SimpleDriverDataSource">
      ...
   </bean>
</beans>
```

스프링의 DI 는 인터페이스를 사이에 두고 의존 클래스를 바꿔서 사용하도록 하는게 목적이지만 jdbcContext 는 그렇지 못하고 있다. 하지만 굳이 인터페이스를 통해서 사용하게 하지 않아도 된다.

첫번째 이유로 jdbcContext 가 스프링의 싱글톤 레지스트리에서 관리되는 싱글톤 빈이 되기 때문이다. jdbcContext 의 경우 그 자체로 독립적인 JDBC 컨텍스트를 제공해주는 일종의 서비스 오브젝트로서 의미가 있다.

두번째 이유로 DI 를 위해서는 주입되는 오브젝트와 주입받는 오브젝트 양쪽 모두다 스프링의 빈으로 등록되어야 하는데, jdbcContext 가 DI 를 통해 다른 빈에 의존하고 있기 때문이다.


강력한 결합을 가진 관계를 허용하면서도 싱글톤으로 만들고, DI 필요성을 위해 스프링의 빈으로 등록해서 UserDao 에 DI 되도록 만들어져도 좋은 것이다.

**코드를 이용한 수동 DI**

jdbcContext 를 스프링의 빈으로 등록하는 대신 사용할 수 있는 방법이 있다. UserDao 내부에서 직접 DI 를 하는 것이다. 이렇게 사용하려면 싱글톤으로 사용하는 것은 불가능 하지만, DAO 마다 하나의 jdbcContext 를 가지게 한다면 DAO 의 갯수에 따라 생성될 것이고 이는 메모리에 주는 부담도 거의 없을 것이다.

jdbcContext 의 생성과 책임은 UserDao가 갖게 되더라도, 이미 jdbcContext 의 정체를 알고 있으니 문제가 되지는 않을 것이다.

하지만 jdbcContext 에서 dataSource 를 주입받으려면 jdbcContext 가 스프링의 빈이 되어야만 했는데 이 문제는 UserDao 에서 dataSource 의 DI 까지 맡기도록 해서 해결해본다.

그렇다면 스프링의 설정파일은 applicationContext 에서 userDao 와 dataSource 만 정의한 후에 소스는 다음과 같이 변경한다.

```
public class UserDao {
   ...
   private JdbcContext jdbcContext;
   public void setDataSource(DataSource dataSource) {
      this.jdbcContext = new JdbcContext();
      this.jdbcContext.setDataSource(dataSource);
   }
   ...
}
```

이 방법으로 굳이 인터페이스를 두지 않아도 될 만큼 긴밀한 관계를 갖는 두 오브젝트를 따로 빈으로 분리하지 않고 내부에서 직접 만들어서 사용하면서도 다른 오브젝트에 대한 DI 를 적용할 수 있다는 점이다.

클래스를 DI 로 사용했을 때는 의존관계가 설정파일을 통해 명확하게 드러나지만 DI 의 근본적인 원칙에 부합하지 않는 구체적인 클래스와의 관계가 설정에 직접 노출된다는 단점이 있었다.

반면 수동으로 DI 하는 방법은 그 관계를 외부에 노출시키지 않지만 싱글톤으로 만들 수 없고, DI 작업을 위한 부가적인 코드가 필요하게 되는 단점이 있다.

## 3.5 템플릿과 콜백 ##

지금까지 개선해 온 코드는 복잡하지만 바뀌지 않는 일정한 패턴을 갖는 작업 흐름이 존재하고 그 중 일부분만 자주 바꿔서 사용해야 하는 경우에 적합한 구조다. 전략패턴의 기본구조에 익명내부클래스를 활용한 방식인데 이 부분을 스프링에서는 템플릿/콜백 패턴이라고 부른다. 컨텍스트가 템플릿이고 익명내부클래스로 만들어지는 오브젝트는 콜백(펑셔널 오브젝트)이다.

**콜백** : 실행되는 것을 목적으로 다른 오브젝트의 메소드에 전달되는 오브젝트를 말한다.

**템플릿/콜백의 특징**

콜백은 보통 작업 흐름 중 특정 기능을 위해 한 번 호출되는 경우가 일반적이기 때문에 단일 메소드 인터페이스를 사용한다.

이런 템플릿/콜백 방식은 전략 패턴과 DI의장점을 익명내부클래스사용 전략과 결합한 독특한 활용법이라고 이해할 수 있다.

**편리한 콜백의 재활용**

익명내부클래스 사용으로 상대적으로 코드의 작성 및 읽기가 조금 불편한 점을 콜백의 분리와 재활용을 통해 익명내부클래스의 사용을 최소화 해본다.

```
public void deleteAll() throws SQLException {
   executeSql("delete from users");
}

private void eexecuteSql(final String query) throws SQLException {
   this.jdbcContext.workWithStatementStrategy(
      new StatementStrategy() {
         public PreparedStatement makePreparedStatement(Connection c) throws SQLException {
            return c.prepareStatement(query);
         }
      }
   );
}
```

executeSql 메소드에 바뀌지 않는 모든 부분을 빼내고, 바뀌는 부분인 SQL 문장만 파라미터로 받아서 사용하게 하였다. 또한 이 메소드는 UserDao 에서만 사용하기는 아까우니 jdbcContext 클래스 안으로 옮기도로록 한다. (이 때 접근자는 public 이 되어야 할 것이다)

```
public void deleteAll() throws SQLException {
   this.jdbcContext.executeSql("delete from users");
}
```

```
public void eexecuteSql(final String query) throws SQLException {
   workWithStatementStrategy(
      new StatementStrategy() {
         public PreparedStatement makePreparedStatement(Connection c) throws SQLException {
            return c.prepareStatement(query);
         }
      }
   );
}
```

이로 인해 jdbcContext 안에 클라이언트, 템플릿, 콜백이 모두 함께 공존하면서 동작하는 구조가 되었다.

## 3.6 스프링의 JdbcTemplate ##

앞서 만들었던 jdbcContext 와 유사한, 스프링에서 제공하는 JdbcTemplate 을 사용해보도록 한다.

```
public class UserDao {
   ...
   private JdbcTemplate jdbcTemplate;

   public void setDataSource(DataSource dataSource) {
      this.jdbcTemplate = new JdbcTemplate(dataSrouce);
      this.dataSource = dataSource;   
   }
}
```

**update**

deleteAll 메소드에서 적용했던 콜백은 StatementStrategy 인터페이스의 makePreparedStatement 메소드이다. 이에 대응되는 JdbcTemplate 의 콜백은 PreparedStatementCreator 인터페이스의 createPreparedStatement 메소드이다. 하지만 executeSql 메소드처럼 SQL 문장만 파라미터로 넘길 수도 있다.

```
public void deleteAll() {
   this.jdbcTemplate.update("delete from users");
}
```

하지만 바인딩 되어야 할 필요가 있는 add 메소드의 경우 SQL 문장과 함께 가변인자로 선언된 파라미터를 제공해주면 된다.

```
   this.jdbcTemplate.update("insert into users(id, name, password) values (?, ?, ?)", user.getId(), user.getName(), user.getPassword());
```

**queryForInt**

getCount 메소드의 경우 쿼리를 실행하고 ResultSet 을 통해 결과값을 가져온다. 이런 흐름을 가진 코드에서 사용할 수 있는 템플릿은 PreparedStatementCreator 콜백과 ResultSetExtractor 콜백을 파라미터로 받는 query 메소드이다.

```
public int getCount() {
   return this.jdbcTemplate.query(new PreparedStatementCreator() {
         public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
            return con.prepareStatement("select count(*) from users");
         }
      }, new ResultSetExtractor<Integer>() { // 제네릭스 타입 파라미터
         public Integer extractData(ResultSet rs) throws SQLException, DataAccessException {
            rs.next();
            return rs.getInt(1);
         }
      }
   });
}
```

위 코드의 ResultSetExtractor는 제네릭스 타입 파라미터를 가지고 있다. SQL 의 실행 결과가 하나의 저수값이 되는 경우는 자주 볼 수 있기 때문에 이런 콜백을 내장하고 있는 queryForInt() 라는 편리한 메소드가 있다.

```
public int getCount() {
   return this.jdbcTemplate.queryForInt("select count(*) from users");
}
```

**queryForObject**

이번에는 get 메소드에 jdbcTemplate 을 적용해본다. 리턴 되는 값이 getCount 처럼 단순하지 않고 User 오브젝트를 리턴하기 때문이다.

그러기 위해 ResultSetExtractor대신 RowMapper 콜백을 사용해보록 하겠다. ResultSetExtractor 와 RowMapper 는 템플릿으로부터 ResultSet 을 전달받고 필요한 정보를 추출해서 리턴하는 방식이다. 하지만 ResultSetExtractor는 ResultSet 을 한 번 전달받아 알아서 추출 작업을 모두 진행하고 최종결과만 리턴해주는데 반해, RowMapper 는 ResultSet 의 로우 하나를 매핑하기 위해 사용되기 때문에 여러번 호출될 수 있다는 점이 다르다.

```
public User get(String id) {
   return this.jdbcTemplate.queryForObject("select * from users where id = ?", 
      new Object[]{id}, // 뒤에 다른 파라미터가 있기 때문에 가변인자 대신 배열을 이용
      new RowMapper<User>() {
         public User mapRow(ResultSet rs, int rowNum) throws SQLException {
            User user = new User();
            user.setId(rs.getString("id");
            ...
            return user;
         }  
      });
}
```

이 queryForObject 메소드는 SQL 실행 시 한개의 로우만 얻을 것이라고 기대한다. 그리고 하나가 아니라면 EmptyResultDataAccessException 예외를 던지도록 되어 있다.

**query**

하나의 로우가 아닌 다수의 로우를 가져오게 되는 getAll 메소드를 만들어보자. 여러개이니 리턴은 User 오브젝트의 컬렉션인 List

&lt;User&gt;

 타입으로 돌려주는게 가장 좋은 방법일 것이다.

```
public List<User> getAll() {
   return this.jdbcTemplate.query("select * from users order by id", 
      new RowMapper<User>() {
         public User mapRow(ResultSet rs, int rowNum) throws SQLException {
            User user = new User();
            user.setId(rs.getString("id"));
            ...
            return user;
         }
   });
}
```

**중복제거**

get 메소드와 getAll 메소드의 RowMapper 가 똑같이 사용되고 있음을 발견할 수 있다. RowMapper 콜백은 userMapper 라는 인스턴스 변수로 만들도록 한다.

```
public class UserDao {
   private RowMapper<User> userMapper = new RowMapper<User>() {
      public User mapRow(ResulSet rs, int rowNum) throws SQLException {
         User user = new User();
         user.setId(rs.getString("id"));
         ...
         return user;
      }
   }; 

   public User get(String id) {
      return this.jdbcTemplate.queryForInt("select * from users where id = ?",
         new Object[]{id}, 
         this.userMapper
      );
   }

   pubblic List<User> getAll() {
      return this.jdbcTemplate.query("select * from users",
         this.userMapper
      );
   }
}
```

이제 UserDao 에는 User 정보를 DB 에 넣거나 가져오거나 조작하는 방법에 대한 핵심적인 로직만 담겨 있다. 만약 사용할 테이블과 필드정보가 바뀌면 UserDao 의 거의 모든 코드가 함께 바뀌게 되며, 이는 응집도가 높다고 볼 수 있다.

JDBC API 를 사용하는 방식, 예외처리, 리소스의 반납, DB 연결에 대한 책임과 관심은 모두 JdbcTemplate 에게 있다. 이런 면에서 UserDao 와 낮은 결합도를 가지고 있게 되었다.

이런 UserDao 에도 개선할 점을 아직도 발견할 수 있다. 첫째, userMapper 가 인스턴스 변수로 설정되어 있고 한 번 만들어지면 변경되지 않는 프로퍼티와 같은 성격을 띠고 있으니 아예 DI 용 프로퍼티로 만들어 버릴 수 있다. 이렇게 되면 UserDao 의 코드를 수정하지 않고도 매핑정보를 변경할 수 있는 장점이 잇다.

두번째로 SQL 문장을 UserDao 코드가 아닌 외부 리소스에 담고 이를 읽어와 사용하게 하는 것이다. 그러면 DB 테이블의 정보가 바뀌어 쿼리문이 수정되어야 할 때 UserDao 클래스를 수정하는 일은 없을 것이다.

요즘엔 초창기에 주로 사용됐던 JdbcTemplate 보단 이것을 확장한 SimpleJdbcTemplate 가 주로 사용된다.

# 생각하기 #

아무리 교육시간에 한번 실습했던 내용이었어도 익명내부클래스 등등 나오기 시작하면서 부터는 책을 일단 쭉- 읽었다. 그냥 단지 자바로 만들어진 코드인데도 정말 익숙치 않은 코드이다. (나만 그렇지 않길... 바라면서...) 그 과정의 이유 등등을 이해하기 위해선 책을 몇번이고 다시 읽어야 했다. 실습은 필수였다.

일단 문제점을 제기하고 그 문제점을 해결해 나가는 과정은 그 동기부여와 흥미유발에 있어서 정말 좋은 관점인 것 같다. (책 칭찬 그만하라고 하셨지만...) 교육 때 박찬욱 강사님(그땐 강사님~)을 따라 JdbcTemplate 까지 가는 과정은 어느 강의 때 보다 재미있었고 흥미로웠고 흥분됐었다. 다만 그 속도를 따라 잡느라(?) 놓치고 일단 코딩한 부분들에 대해서 책을 통해 자세히 알 수 있어서 좋았고 그냥 흘려 들은 강사님의 멘트(?)도 다시금 생각나게 하였다.

strategy 패턴은 템플릿메소드패턴을 나홀로(?) 심화학습을 할 때 책에서 비슷하다~ 라는 식으로 소개 되서 흘려 봤었고 이렇게 바로 나오게 되니 다시 패턴책을 들춰봐야 겠다. (네 ㅋㅋ 제가 전략 패턴 준비하겠습니다 ㅋㅋ)

콜백에 대해서는 액션스크립트나 자바스크립트를 통해서 알고는 있어서 이해를 돕는데 도움이 되었다. 다양한 언어를 해본 것이 (라고 절대 할순 없지만!) 이럴 때 도움을 주기도 하나보다. 액션스크립트에서는 보통 이벤트에 대한 처리를 할 때 사용되었었는데 이렇게 자바에서 다시(?) 만나게 될 줄이야! ㅋㅋㅋ

책에서는 쿼리까지 분리하지는 않았지만 요즘 대부분의 프로젝트에서 쿼리는 따로 관리한다고 생각한다. (스트러츠 1 할 때는 코드에 쿼리가 다 있어서 매번 컴파일하기 불편했다. 좀 오래된 소스였어서...) 일단 우리 프로젝트 같은 경우 anyframe 을 쓰며 xml 로 쿼리를 관리한다. 특히 anyframe 에서 제공하는 find-ListWithPaging 은 정말 편리하게 사용하고 있다. 다른 분들은 어떻게 쓰시는지 궁금합니다. ㅋㅋ