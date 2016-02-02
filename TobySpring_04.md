

# 정리 #

개요..

## 4.1 사라진 SQL Exception ##

3장에서 JdbcContext에서 JdbcTemplate를 적용하는 코드를 만든 후 각 메소드에서 예외처리의 차이점을 발견할 수 있다.

```
public void deleteAll() throws SQLException {
   this.jdbcContext.executeSql("delete from users");
}
```

```
public void deleteAll() {
   this.jdbcTemplate.update("delete from users");
}
```

**예외 블랙홀**

예외를 처리할 때 반드시 지켜야 할 핵심 원칙은 한 가지다. 모든 예외는 적절하게 복구되든지 아니면 작업을 중단시키고 운영자 또는 개발자에게 분명하게 통보돼야 한다.

다음과 같은 코드들은 예외를 무시하거나 잡아먹어 버리면서 심각한 상황을 초래할 수 있다.

  1. 초난감 예외처리 코드 1
```
} catch (SQLException e) {
}
```
  1. 초난감 예외처리 코드 2
```
} catch (SQLException e) {
   System.out.println(e);
}
```
  1. 초난감 예외처리 코드 3
```
} catch (SQLException e) {
   e.printStackTrace();
}
```
  1. 그나마 나은 예외처리 (?)
```
} catch (SQLException e) {
   e.printStackTrace();
   System.exit(1);
}
```

**무의미하고 무책임한 throws**

예외처리를 throws 를 통해 예외를 무조건 던져버리는 선언을 모든 메소드에 기계적으로 넣는 무책임한 throws 선언은 의미있는 정보를 얻을 수가 없어 적절한 처리가 될 수 있는 기회도 얻지 못하게 된다.


**예외의 종류와 특징**

예외의 종류에는 크게 3가지가 있다.

1. Error

java.lang.Error 클래스의 서브클래스로, 시스템에 뭔가 비정상적인 상황이 발생했을 경우에 사용되며 주로 VM 에서 발생시키는 것이기 때문에 어플리케이션 코드에서는 특별한 신경을 쓰지 않아도 된다. OutOfMemoryError 나 ThreadDeath 같은 에러가 이에 해당한다.

2. Exception 과 체크 예외

Exception 을 상속받는 서브 클래스 중 RuntimeException 은 상속받지 않는 서브 클래스이다. 이런 체크 예외가 발생할 수 있는 메소드를 사용할 경우 반드시 예외처리가 필요하다. catch 문이나 throws 로 처리해줘야 컴파일 에러가 나지 않는다. IOException 이나 SQLException 이 이에 해당한다.

3. RuntimeException 과 언체크예외

java.lang.RuntimeException 을 상속받은 예외들은 예외처리를 강제하지 않기 때문에 언체크예외라고 불린다. 또는 대표 클래스 이름을 따서 런타임예외라고도 불린다. 이 예외는 주로 프로그램의 오류가 있을 때 발생하도록 의도된 것들로 코드에서 미리 조건을 체크하도록 주의 깊게 만든다면 피할 수 있다. NullPointerException 이나 IllegalArgumentException 이 이에 해당한다.

체크 예외가 예외처리를 강제하는 것 때문에 예외 블랙홀이나 무책임한 throws 같은 코드가 남발되는 현상이 있어서 최근에는 예상 가능한 예외상황을 다루는 예외를 체크 예외로 만들지 않는 경향이 있다.

**예외처리 방법**

1. 예외 복구

예외상황을 파악하고 문제를 해결해서 정상 상태로 돌려놓는 것이다. 예를 들어 파일을 읽으려 했는데 없을 경우 사용자에게 알려주고 다른 파일을 이용하도록 하는 것 처럼 예외로 인해 기본 작업 흐름이 불가능하면 다른 작업 흐름으로 자연스럽게 유도해주는 것이다.
예외처리 코드를 강제하는 체크 예외들은 이렇게 예외를 어떤 식으로든 복구할 가능성이 있는 경우에 사용한다. API 를 사용하는 개발자로 하여금 예외상황이 발생할 수 있음을 인식하도록 도와주고 이에 대한 적절한 처리를 시도해보도록 요구하는 것이다.

2. 예외처리 회피

예외처리를 자신이 담당하지 않고 throws 문으로 자신을 호출한 쪽으로 던져버리는 것이다.
jdbcContext 나 jdbcTemplate 이 사용하는 콜백 오브젝트는 SQLException 을 처리하는 일이 자신이 하는 역할이 아니라고 보기 때문에 템플릿으로 던져 버린다. 이처럼 예외를 회피하는 것은 의도가 분명해야 하며, 템플릿/콜백처럼 긴밀한 관계에 있는 다른 오브젝트에게 책임을 지게 하거나 자신을 사용하는 쪽에서 예외를 다루는게 최선의 방법이라는 분명한 확신이 있어야 한다.

3. 예외 전환 exception translation

예외처리와 마찬가지로 밖으로 던지지만 발생한 예외를 그대로 넘기는 게 아니라 적절한 예외로 전환해서 던지는 것이다. 이는 보통 두 가지 목적으로 사용된다.

첫째는 내부에서 발생한 예외가 적절한 의미를 부여해주지 못해서 의미를 분명하게 해줄수 있는 예외로 바꿔주기 위해서다. 예를 들어 사용자 정보 등록 시 중복된 아이디가 문제가 될 때 SQLException 이 떨어지지만 이를 좀 더 의미있는 DuplicateKeyException 으로 전환해주는 것이다.

```
pubilc void add(User user) throws DuplicateUserIdException, SQLException {
   try {
      ...
   } catch (SQLException e) {
      if(e.getErrorCode() == MysqlErrorNumbers.ER_DUP_ENTRY) {
         throws new DuplicateUserIdException(e); // 중첩 예외 1
         // throws new DuplicateUserIdException().initCause(e); // 중첩 예외 2
      } else throw e;
   }
}
}
```

보통 전환하는 예외는 원래 발생한 예외를 담아서 중첩예외 nested exception 로 만드는 것이 좋다.

두번째 방법은 예외를 처리하기 쉽고 단순하게 만들기 위해 포장하는 것이다. 주로 예외처리를 강제하는 체크 예외를 언체크 예외인 런타임 예외로 바꾸는 경우에 사용한다. 대표적인 예로 EJBException 을 들 수 있다. 런타임 예외이기 때문에 일일이 예외를 잡거나 다시 던지는 수고를 할 필요가 없다.

```
try {
   ...
} catch(NamingException ne) {
   throw new EJBException(ne);
} catch(SQLException se) {
   throw new EJBException(se);
} catch(RemoteException re) {
   throw new EJBException(re);
}
```

이런 예외는 잡아도 복구할 만한 방법이 없기 때문에 런타임 에러로 전환하는 것이 적절하지만 애플리케이션 로직상에서 예외조건이 발견되거나 예외상황이 발생할 수도 있는 의도적으로 던지는 예외는 체크예외를 사용하는 것이 적절하다. 비즈니스적인 의미가 있는 예외는 이에 대한 적절한 대응이나 복구 작업이 필요하기 때문이다.

일반적으로 체크 예외를 계속 throws 를 사용해 넘기는건 무의미하다. 어차피 복구가 불가능한 예외라면 가능한 한 빨리 런타임 예외로 포장해 던지게 해서 다른 계층의 메소드를 작성할 때 불필요한 throws 선언이 들어가지 않도록 해줘야 한다.

**예외처리 전략**

자바 초기부터 있었던 JDK 의 API 와 달리 최근에는 API 가 발생시키는 예외를 체크 예외 대신 언체크 예외로 정의하는 것이 일반화 되고 있다. 항상 복구할 수 있는 예외가 아니라면 언체크 예외/런타임 예외로 던지도록 하는 것이다.

add 메소드의 예외처리를 다시 살펴본다.

```
public class DuplicateUserIdException extends RuntimeException {
   public DuplicateUserIdException(Throwable cause) {
      super(cause);
   }
}
```

DuplicateUserIdException도 굳이 체크 예외로 둬야 하는 것은 아니다. 어디에서든 DuplicateUserIdException을 잡아서 처리할 수 있다면 굳이 체크 예외로 만들지 않고 RuntimeException 을 상속받아서 런타임예외로 만드는 것이 낫다. 대신 add 메소드는 명시적으로 DuplicateUserIdException 을 던진다고 선언해야 의미 있는 정보를 전달해줄 수 있다. 런타임 에러도 throws 로 선언할 수 있으니 문제 될 것은 없다.

```
public void add() throws DuplicateUserIdException {
   try {
      ...
   } catch (SQLException e) {
      if(e.getErrorCode() == MysqlErrorNumbers.ER_DUP_ENTRY) {
         throws new DuplicateUserIdException(e); // 예외 전환
      } else throw new RuntimeException(e); // 예외 포장
   }  
}
```

**애플리케이션 예외**

런타임 예외 중심의 전략은 낙관적인 예외처리 기법이었다. 일단 잡고 보도록 강제하는 체크 예외는 비관적인 접근방법이었다.

반면에 시스템 또는 외부의 예외상황이 아니라 애플리케이션 자체의 로직에 의해 의도적으로 발생시키고 반드시 catch 해서 무엇인가 조치를 취하도록 요구하는 애플리케이션 예외가 있다.

이런 예외상황에 대해 리턴값을 달리 줘서 구분을 할 경우 표준의 정의가 필요하게 된다.
다른 방법으로 예외 상황에 따른 예외를 던지도록 만드는 것이다. 예외상황에 대한 처리는 catch 불록에 모아둘 수 있어 if 문을 남발하지 않을 수 있다. 이때 사용하는 예외는 의도적으로 체크 예외로 만든다.

**SQLExcepion 은 어떻게 됐나?**

SQLException 은 코드 레벨에서 복구할 방법이 없다. 그래서 스프링의 JdbcTemplate 는 템플릿과 콜백안에서 발생하는 모든 SQLExceptino 을 런타임 예외인 DataAccessException 으로 포장해서 던져준다. 그렇기 때문에 DAO 메소드에서 SQLException 이 모두 사라진 것이다.

## 4.2 예외 전환 ##

예외전환의 목적은 두 가지이다.

첫째, 런타임예외로 포장해서 굳이 필요하지 않은 catch/throws 를 줄여주는 것
둘째, 로우레벨의 예외를 좀 더 의미있고 추상화된 예외로 바꿔서 던져주는 것

스프링의 JdbcTemplate 이 던지는 DataAccessException 은 런타임예외로 SQLException 을 포장해주며, 상세한 예외정보를 의미있고 일관성 있는 예외로 전환해준다.

**JDBC의 한계**

JDBC는 자바를 이용해 DB 에 접근하는 방법을 추상화된 API 형태로 정의해놓고 각 DB 업체가 JDBC 표준을 따라 만들어진 드라이버를 제공하게 해준다. JDBC 의 Connection, Statement, ResultSet 등의 표준 인터페이스를 통해 그 기능을 제공해주지만 DB 변경에 대한 유연한 코드를 보장해주지는 못한다.

첫째, 비표준 SQL

SQL 은 어느정도 표준화된 언어이고 몇가지 표준 규약이 있긴 하지만 대부분의 DB 는 표준을 따르지 않는 비표준 문법과 기능도 제공한다. 비표준 SQL 이 DAO 코드에 들어간다면 해당 DAO 는 특정 DB 에 대해 종속적인 코드가 되고 만다. 이 문제의 해결방법은 호환이 가능한 표준 SQL 만 사용하는 방법과 DB 별로 별도의 DAO 를 만드는 방법이 있을 수 있다.

둘째, 호환성 없는 SQLException 의 DB 에러정보

문제는 DB 마다 SQL 만 다른 것이 아니라 에러의 종류와 원인도 제각각이라는 점이다. 그래서 JDBC 는 데이터 처리 중에 발생하는 다양한 예외를 그냥 SQLException 하나에 모두 담아버리나 그 안에 담긴 에러코드는 DB 별로 모두 다르다. 그래서 SQLException 은 예외가 발생했을 때의 DB 상태를 담은 SQL 상태정보를 getSQLState() 메소드로 제공하지만 정확하지 않아 신뢰할 수 없다.

**DB 에러 코드 매핑을 통한 전환**

스프링은 SQLException의 비표준 에러코드와 SQL 상태정보에 대한 해결책으로 에러코드매핑파일을 만들어 놓고 이를 이용한다. DB별 에러코드를 참고해서 발생한 예외의 원인이 무엇인지 해석해주는 기능을 만드는 것이다. DB 종류에 상관없이 동일한 상황에서 일관된 예외를 전달받을 수 있다면 효과적인 대응이 가능하다.

```
<bean id="Oracle" class="org.springframework.jdbc.support.SQLErrorCodes">
   <property name="badSqlGrammarCodes"> // 예외종류 클래스
      <value>900,903,904,917,936,942,17006</value> // 에러코드
   </property>
   ...
```

JdbcTemplate 은 SQLException 을 단지 런타임예외인 DataAccessException 으로 포장하는 것이 아니라 DB 에러코드를 DataAccessException 계층구조의 클래스 중 하나로 매핑해준다.

**DAO 인터페이스와 DataAccessException 계층구조**

DataAccessException 은 JDBC의 SQLException 을 전환하는 용도로만 만들어 진 것이 아니라 JDBC 외의 자바 데이터 액세스 기술에서 발생하는 예외에도 적용된다. JDO, JPA, 하이버네이트, iBatis 도 있다. DataAccessException 은 의미가 같은 예외라면 데이터 액세스 기술의 종류와 상관없이 일관된 예외가 발생하도록 만들어준다.

DAO를 따로 만들어서 사용하는 이유는 데이터 액세스 로직을 담은 코드를 성격이 다른 코드에서 분리해놓기 위해서다. 또한 전략 패턴을 적용해 구현 방법을 변경해서 사용할 수 있게 만들기 위해서이다. 그런데 DAO 의 사용기술과 구현코드는 전략패턴과 DI 를 통해서 DAO 를 사용하는 클라이언트에게 감출 수 있지만, 메소드 선언에 나타나는 예외정보가 문제가 될 수 있다. UserDAO 의 인터페이스를 분리해서 기술에 독립적인 인터페이스로 만들려면 다음과 같이 정의해야 한다.

```
public interface UserDao {
   public void add(User user); 
   ...
}
```

하지만 DAO 에서 사용하는 데이터 액세스 기술의 API 가 예외를 던지기 때문에 throws SQLException 이 선언되어야 한다. 하지만 JDBC 가 아닌 다른 데이터 액세스 기술로 변경될 경우 던져지는 예외가 달라지기 때문에 SQLException 을 던져주도록 선언한 인터페이스 메소드는 사용할 수 없다. 이에 가장 단순한 해결방법은 throws Exception 이지만 무책임한 선언이 된다.

다행히도 JDBC 보다 늦게 등장한 기술들은 체크 예외 대신에 런타임 예외를 사용하므로 SQLException 을 던지는 JDBC 는 DAO 메소드 내에서 런타임 예외로 포장해서 던져줄 수 있다. 그러면 위 코드처럼 사용이 가능해진다. 하지만 데이터 액세스 기술이 달라지면 같은 상황에서도 다른 종류의 예외가 던져진다는 점이 아직 문제로 남아 있다.

그래서 스프링은 자바의 다양한 데이터 엑세스 기술을 사용할 때 발생하는 예외들을 추상화해서 DataAccessException 계층구조안에 정리해놓았다. 일부 기술에서만 공통적으로 나타나는 예외를 포함해서 데이터 액세스 기술에서 발생 가능한 대부분의 예외를 계층구조로 분류해놓았다.

그 외 템플릿 메소드나 DAO 메소드에서 직접 활용 할 수 있는 IncorrectResultSizeDataAccessException 이나 EmptyResultDataAccessException 등의 예외도 정의되어 있다.

JdbcTemplate 과 같이 스프링의 데이터 액세스 지원 기술을 이용해 DAO 를 만들면 사용 기술에 독립적인 일관성 있는 예외를 던질 수 있다.

**기술에 독립적인 UserDao 만들기**

UserDao 클래스를 인터페이스와 구현으로 분리해본다. 구현 클래스의 이름은 각각의 특징을 따르도록 명명한다.

```
public interface UserDao {
   void add(User user);
   ...
}
```

```
public class UserDaoJdbc implements UserDao {
   ...
}
```

```
<bean id="userDao" class="springbook.dao.UserDaoJdbc">
   <property name="dataSource" ref="dataSource" />
</bean>
```

**DataAccessException 활용 시 주의사항**

이렇게 스프링을 활용하면 DB 종류나 데이터 액세스 기술에 상관없이 키 값이 중복이 되는 상황에서는 동일한 예외가 발생하리라고 기대하지만 DuplicateKeyException 같은 경우 JDBC 를 이용하는 경우에만 발생한다. DataAccessException 이 기술에 상관없이 어느 정도 추상화된 공통 예외로 변환해주긴 하지만 근본적인 한계 때문에 완벽하다고 기대할 수 없다.

스프링은 SQLException 을 DataAccessException으로 전환하는 다양한 방법을 제공한다. SQLException 을 코드에서 직접 전환하고 싶다면 SQLExceptionTranslator 인터페이스를 구현한 SQLErrorCodeSQLExceptionTranslator 를 사용한다. 해당 구현 클래스는 DB의 종류를 알아내기 위해 DataSourde 를 필요로 한다.

```
@Test
public void sqlExceptionTranslate() {
   ...
   try {
      ...
   } catch (DuplicateKeyException ex) {
      SQLException sqlEx = (SQLException) ex.getRootCause();
      SQLExceptionTranslator set = new SQLErrorCodeSQLExceptionTranslator(this.dataSource);
      assertThat(set.translate(null, null, sqlEx), is(DuplicateKeyException.class));
   }
}
```

JDBC 외의 기술을 사용할 때도 DuplicateKeyException 을 발생시키려면 SQLException 을 가져와서 직접 예외 전환을 하는 방법을 생각해볼 수도 있다. 또는 JDBC 를 이용하지만 JdbcTemplate 과 같이 자동으로예외를 전환해주는 스프링의 기능을 사용할 수 없는 경우라도 SQLException 을 그대로 두거나 의미 없는 RuntimeException 으로 뭉뚱그려서 던지는 대신 스프링의 DataAccessException 계층의 예외로 전환하게 할 수 있다.


# 생각하기 #

3장 리뷰 까지 너무 많은 반성과 감탄을 했더니 ㅋㅋ 더 이상 무엇을 써야 할지 고민하느라 이제서야 리뷰를 씁니다. ㅋ
4장을 보면서 회사에 출근하자 마자 프로젝트 소스를 확인해보았더니 throws Exception 이 콘트롤러까지 따라 다니고 있었습니다. 애니프레임을 사용하는데 애니프레임의 최상위 Exception 으로 보이는 BaseException 이 일단 Exception 을 바로 상속하고 있었고요. 그 어디에서도 RuntimeException 으로 전환해주고 있지 않았네요 ㅋㅋ
사실 그럼에도 불구하고 불편함을 크게 느끼지 않았기 때문에 계속 사용하고 있지만 앞으로는 Exception 을 제대로 관리 할 수 있는 습관이 필요하겠네요! (결국 또 반성이 되는건가요...)