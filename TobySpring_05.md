

# 정리 #

지금까지 만든 DAO 에 트랜잭션을 적용해보면서 스프링이 어떻게 성격이 비슷한 여러 종류의 기술을 추상화하고 이를 일관된 방법으로 사용할 수 있도록 지원하는지를 살펴볼 것이다.

## 5.1 사용자 레벨 관리 기능 추가 ##

CRUD 의 기초적인 작업만 있었던 UserDao 를 통해 사용자의 레벨을 조정해주는 기능을 만들도록 한다.

**Level이늄**

DB 에 사용자레벨 필드를 추가하고 User에 도 사용자레벨 프로퍼티를 추가한다. 이때 프로퍼티를 숫자로 지정할 경우 의미없는 숫자가 사용될 가능성도 있기 때문에 자바 5 이상에서 제공하는 이늄(enum)을 이용해보도록 한다.

```
public enum Level {
   BASIC(1), SILVER(2), GOLD(3); // 세개의 이늄 오브젝트 정의
   private final int value;
   Level(int value) { // DB에 저장할 값을 넣어줄 생성자를 만들어준다
      this.value = value;
   }

   public int initValue() {
      return value;
   }

   public static Level valueOf(int value) {
      switch(value) {
         case 1 : return BASIC;
         case 2 : return SILVER;
         case 3 : return GOLD;
         default : throw new AssertionError("Unknown value : "+value); 
      }
   }
}
```

User, UserDaoTest, UserDaoJdbc, RowMapper 등을 모두 수정하고 사용자를 수정하는 update 메소드를 추가한다.

**UserService.upgrateLevels()**

사용자 관리 로직은 어디에 두어야 좋을까? DAO 는 데이터를 어떻게 가져오고 조작할지를 다루는 곳이니 적당하지 않다. 사용자 관리 비즈니스 로직을 담을 Service 클래스를 하나 추가하도록 한다.

UserService 는 인터페이스 타입으로 UserDao 를 DI 받아 사용하도록 한다. 그러기 위해서는 UserService 도 스프링의 빈으로 등록 되어야 한다. UserService 를 위한 테스트 클래스도 추가한다.

**upgradeLevels() 메소드**

```
public void upgradeLevels() {
   List< User > users = userDao.getAll(); // 사용자 정보를 모두 가져옴
   for(User user : users) {
      Boolean changed = null;
      // 각 레벨에 맞는 조건을 검사하고 changed 변수에 변경여부를 담는다.
      if(user.getLevel() == Level.BASIC && user.getLogin() >= 50) {
         user.setLevel(Level.SILVER);
         changed = true;
      } else if(user.getLevel() == Level.SILVER && user.getRecommend() >= 30) {
         user.setLevel(Level.GOLD);
         changed = true;
      } else if(user.getLevel() == Level.GOLD) { changed = false; }
      } else changed = false;
      if(changed) userDao.update(user); // 변경이 있는 User 만 업데이트 한다
   }
}
```

```
@Test
public void upgradeLevels() {
   userDao.deleteAll();
   for(User user : user) userDao.add(user);
   userService.upgradeLevels();
   // 예상 레벨을 검증
   checkLevel(users.get(0), Level.BASIC);
   checkLevel(users.get(1), Level.SILVER);
   checkLevel(users.get(2), Level.SILVER);
   checkLevel(users.get(3), Level.GOLD);
   checkLevel(users.get(4), Level.GOLD);
}

private void checkLevel(User user, Level expectedLevel) { // 중복되는 코드를 헬퍼 메소드로 분리
   User userUpdate = userDao.get(user.getId());
   assertThat(userUpdate.getLevel(), is(expectedLevel));
}
```

**UserService.add()**

사용자 레벨 변경 기능은 완료하였지만 사용자가 처음 등록 되었을 때 기본적으로 BASIC 레벨을 가져야 하는 코드를 추가하도록 한다. 이 로직은 DAO 나 User 클래스에 책임을 주기에는 적절하지 못하므로 UserService 에 넣어보도록 한다. add() 를 호출 할 때 Level 이 비어있으면 BASIC 을 넣어주도록 하는 것이다.

```
public void add(User user) {
   if (user.getLevel() == null) user.setLevel(Level.BASIC);
   userDao.add(user);
}
```

```
@Test
public void add() {
   userDao.deleteAll();
   User userWithLevel = users.get(4);
   User userWithoutLevel = users.get(0);
   userWithoutLevel.setLevel(null);
   
   userService.add(userWithLevel);
   userService.add(userWithoutLevel);
  
   User userWithLevelRead = userDao.get(userWithLevel.getId());
   User userWithoutLevelRead = userDao.get(userWithoutLevel.getId());

   assertThat(userWithLevelRead.getLevel(), is(userWithLevel.getLevel()));
   assertThat(userWithoutLevelRead.getLevel(), is(userWithoutLevel.getLevel()));
}
```

**코드 개선**

  * 코드에 중복된 부분은 없는가?
  * 코드가 무엇을 하는 것인지 이해하기 불편하지 않은가?
  * 코드가 자신이 있어야 할 자리에 있는가?
  * 변경이 일어난다면 어떤것이 있을 수 있고, 그 변화에 쉽게 대응할 수 있게 작성되어 있는가?

upgradeLevels() 는 자주 변경될 가능성이 있는 구체적인 내용이 추상적인 로직의 흐름과 함께 섞여 있으므로 이를 리팩토링 한다. 일단 작업의 기본 흐름만 만든 후 구체적인 메소드를 만들어보도록 한다.

```
public void upgradeLevels() {
   List< User > users = userdao.getall();
   for(User user : users) {
      if(canUpgradeLevel(user)) {
         upgradeLevel(user);
      }
   } 
}

private boolean canUpgradeLevel(User user) {
   Level currentLevel = user.getLevel();
   switch(currentLevel) {
      case BASIC : return (user.getLogin() >= 50);
      case SILVER : return (user.getRecommend() >= 30);
      case GOLD : return false;
      default throw new IllegalArgumentException("Unknown Level : "+currentLevel);
   }
}

private void upgradeLevel(User user) {
   if(user.getLevel() == Level.BASIC) user.setLevel(Level.SILVER);
   else if(user.getLevel() == Level.SILVER) user.setLevel(Level.GOLD);
   userDao.update(user);
}
```

이때 upgradeLevel() 메소드를 Level enum 에서 다음 단계 레벨정보를 담을 수 있도록 수정해서 변경해보도록 한다.

```
public enum Level {
   GOLD(3, null), SILVER(2, GOLD), BASIC(1, SILVER);

   private final int value;
   private final Level next;

   Level(int value, Level next) {
      this.value = value;
      this.next = next;
   }

}
```

```
public void upgradeLevel() { // User 클래스에 추가
   Level nextLevel = this.level.nextLevel();
   if(nextLevel == null) {
      throw new IllegalStateException(this.level + "은 업그레이드가 불가능합니다");
   } else {
      this.level = nextLevel;
   }
}
```

```
private void upgradeLevel(User user) {
   user.upgradeLevel();
   userDao.update(user);
}
```

이렇게 개선된 코드들은 각자 자기 책임에 충실한 작업만 하고 있으니 코드를 이해하기도 쉬우며 변경이 필요할 떄 어디를 수정해야 할지도 쉽게알 수 있게 되었다.

```
@Test
public void upgradeLevels() {
   userDao.deleteAll();
   for(User user : users) userDao.add(user);
   
   userService.upgradeLevels();
 
   checkLevelUpgraded(users.get(0), false);
   checkLevelUpgraded(users.get(1), true);
   checkLevelUpgraded(users.get(2), false);
   checkLevelUpgraded(users.get(3), true);
   checkLevelUpgraded(users.get(4), false);
}

private void checkLevelUpgraded(User user, boolean upgraded) {
   User userUpdate = userDao.get(user.getId());
   if(upgraded) {
      assertThat(userUpdate.getLevel(), is(user.getLevel().nextLevel()));
   } else {
      assertThan(userUpdate.getLevel(), is(user.getLevel());
   }
}
```

업그레이드 조건인 로그인 횟수와 추천횟수가 각 코드에서 중복되오 있으므로 이를 상수로 빼내도록 한다.

```
public static final int MIN_LOGCOUNT_FOR_SILVER = 50;
public static final int MIN_RECOMMEND_FOR_GOLD = 30;

...
      case BASIC : return (user.getLogin() >= MIN_LOGCOUNT_FOR_SILVER);
      case SILVER : return (user.getRecommend() >= MIN_RECOMMEND_FOR_GOLD);
...
```

업그레이드 정책이 변경될 경우를 위해 UserLevelUpgradePolicy 인터페이스를 만들고 UserService 에서 이를 구현한 클래스를 DI 받아서 사용할 수 있다.

```
public interface UserLevelUpgradePolicy {
   boolean canUpgradeLevel(User user);
   void upgradeLevel(User user);
}
```

## 5.2 트랜잭션 서비스 추상화 ##

지금까지 만든 사용자 레벨 업그레이드 코드가 진행중에 예외가 발생해서 작업이 중단된다면 이전 상태로 돌아갈지 바뀐 채로 남아 있을지 확인해보도록 한다.

```
static class TestUserService extends UserService {
   private String id;
   private TestUserService(String id) {
      this.id = id;
   }
   protected void upgradeLevel(User user) { // 오버라이딩 할 수 있게 접근자가 protected 로 변경
      if(user.getId().equals(this.id)) throw new TestUserServiceException();
     super.upgradeLevel(user);
   }
}
```

```
static class TestUserServiceException extends RumtimeException {
}
```

```
@Test
public void upgradeAllOrNothing() {
   UserService testUserService = new TestUserService(users.get(3).getId());
   testUserService.setUserDao(this.userDao);
   userDao.deleteAll();
   for(User user : users) userDao.add(user);

   try {
      testUserService.upgradeLevels(0;
      fail("TestUserServiceException expected");
   } catch (TestUserServiceException e) { }

   checkLevelUpgraded(users.get(1), false);
}
```

위의 테스트는 실패할 것이다. 예외가 발생했지만 그 전에 작업된 내용이 그대로 남아 있기 때문이다. 이는 더 이상 나눌수 없는 단위 작업인 트랜잭션 문제로, upgradeLevels() 메소드가 하나의 트랜잭션안에서 동작하지 않았기 때문이다.

**트랜잭션 경계설정**

위와 같이 여러 작업을 진행중에 예외가 발생되서 중단이 되면 모든 작업을 무효화 하는 트랜잭션 롤백이 진행되어야 하고 성공적으로 마무리 되면 모든 작업을 확정하는 트랜잭션 커밋이 되어야 할 것이다.

```
Connection c = dataSource.getConnection();
c.setAutoCommit(false); // 트랜잭션 시작
try {
   ... // 작업
   c.commit(); // 트랜잭션 커밋
} catch(Exception e) {
   c.rollback(); // 트랜잭션 롤백
} 
c.close();
```

위는 트랜잭션을 적용하는 가장 간단한 예제이다. JDBC 의 트랜잭션은 하나의 Connection 을 가져와 사용하다가 닫는 사이에 일어나므로 자동커밋 옵션을 false 로 두면 시작된다. JDBC 의 기본 설정은 DB 작업을 수행한 직후에 자동으로 커밋이 되므로 여러개의 DB 작업을 모아서 트랜잭션을 만드는 것이다.

setAutoCommit(false) 부터 commit() 이나 rollback() 을 정하는 작업이 트랜잭션의 경계설정이다. 이렇게 하나의 DB 커넥션 안에서 만들어지는 트랜잭션을 로컬 트랜잭션이라고도 한다.

일반적으로 트랜잭션은 커넥션보다도 존재 범위가 짧아 JdbcTemplate의 메소드를 사용하는 UserDao 는 각 메소드마다 하나식의 독립적인 트랜잭션으로 실행될 수 밖에 없다.

이 문제를 해결하기 위해 DAO 메소드 안으로 upgradeLevels() 메소드의 내용을 옮기는 것은 비즈니스 로직과 데이터 로직을 한데 묶어버리게 되므로 안된다. 그렇다면 UserService 쪽으로 트랜잭션을 가져와야 한다. UserService 에서 Connection 객체를 만들고 각 UserDao 의 메소드에 파라미터로 넘겨야 하는데 여기에도 여러가지 문제점이 발생한다.

첫째, JdbcTemplate 을 더 이상 활용할 수가 없다. try/catch/finally 블록은 UserService 에 존재해야 할 것이다.

둘재, UserService 는 스프링 빈으로 선언해 싱글톤으로 사용되어 있으니 UserService 의 인스턴스 변수에 Connection 을 저장해두면 서로 덮어쓰는 일이 발생할 것이다.

셋째, Connection 파라미터가 UserDao 인터페이스 메소드에 추가 되면 UserDao 는 더 이상 데이터 액세스 기술에 독립적일 수가 없다는 점이다.

넷째, DAO 메소드에 Connection 파라미터를 받게 하면 테스트 코드에서도 Connection 객체를 일일이 만들어야 하는 영향이 미치게 된다.

**트랜잭션 동기화**

먼저 파라미터로 Connection 을 직접 전달하는 문제를 해결하기 위해 스프링이 제안하는 방법은 독립적인 트랜잭션 동기화transaction synchronization 방식이다. 트랜잭션 동기화란 UserService 에서 트랜잭션을 시작하기 위해 만든 Connection 오브젝트를 특별한 저장소에 보관해두고, 이후에 호출되는 DAO의 메소드에서는 저장된 Connection 을 가져다가 사용하게 하는 것이다. 트랜잭션이 모두 종료되면 동기화를 마치면 된다.

```
private DataSource dataSource;
public void setDataSource(DataSource dataSource) {
   this.dataSource = dataSource;
}

public void upgradeLevels() throws Exception {
   TransactionSynchronizationManager.initSynchronization(); // 동기화 작업 초기화
   Connection c = DataSourceUtils.getConnection(dataSource); // 커넥션 생성과 트랜잭션 동기화에 사용하도록 저장소에 바인딩해준다.
   c.setAutoCommit(false);

   try {
      ... // JdbcTemplate 에서는 동기화 저장소에 현재 시작된 트랜잭션을 가진 Connection 오브젝트가 존재하는지 확인 후 이를 발견하고 가져온다. 그리고 이 경우 Connection 을 닫지 않은 채로 작업을 마친다.
      c.commit();
   } catch(Exception e) {
      c.rollback();
      throw e;
   } finally {
      DataSourceUtils.releaseConnection(c, dataSource); // 커넥션 닫기
      // 동기화 작업 종료 및 정리
      TransactionSynchronizationManager.unbindResource(this.dataSource);
      TransactionSynchronizationManager.clearSynchronization();
   }
}
```

DataSource 가 트랜잭션 동기화에 필요하기 때문에 테스트 및 설정파일 수정이 필요하다.

```
@Autowired DataSource dataSource;

@Test
public void upgradeAllOrNothing() extends Exception {
   UserService testUserService = new TestUserService(users.get(3).getId());
   testUserService.setUserDao(this.userDao);
   testUserService.setDataSource(this.dataSource);
   ...
}
```

```
<bean id="userService" class="springbook...UserService">
   <property name="userDao" ref="userDao" />
   <property name="dataSource" ref="dataSource" />
</bean>
```

JdbcTemplate 은 트랜잭션 동기화 저장소에 등록된 DB 커넥션이 없는 경우에는 직접 DB 커넥션을 만들고 트랜잭션을 시작해서 JDBC 작업을 진행하고, 트랜잭션 동기화를 시작해놓았다면 트랜잭션 동기화 저장소에 들어 있는 DB 커넥션을 가져와서 사용한다.

이는 try/catch/finally 작업흐름지원, SQLException 의 예외변환과 함께 JdbcTemplate 이 제공해주는 세 가지 유용한 기능 중 하나다.

**트랜잭션 서비스 추상화**

지금까지 작업한 내용은 로컬 트랜잭션 안에서 완벽한 코드였다. 하지만 여러개의 DB 를 사용하는 글로벌 트랜잭션이라면 이를 지원하는 JTA Java Transaction API 를 이용해야 한다.

애플리케이션에서는 기존의 방법대로 DB 는 JDBC, 메시징 서버는 JMS 와 같은 API 를 사용해서 필요한 작업을 수행한다. 단 트랜잭션은 JDBC 나 JMS API 를 사용해서 직접 제어하지 않고 JTA 를 통해 트랜잭션 매니저가 관리하도록 위임한다. JTA 를 이용한 전형적인 트랜잭션 처리코드는 다음과 같다.

```
InitialContext cts = new InitialContext();
UserTransaction tx = (UserTransaction) ctx.lookup(USER_TX_JNDI_NAME);
tx.begin();
Connection c = dataSource.getConnection(); // JNDI 로 가져온 dataSource
try {
   ...
   tx.commit();
} catch(Exception e) {
   tx.rollback();
   throw e;
} finally {
   c.close();
}
```

문제는 JDBC 로컬 트랜잭션을 JTA 로 변경할 경우 UserService 의 코드를 수정해야 한다는 점이다. 또한 하이버네이트를 이용한다면 또 달라질 것이다.

트랜잭션 도입으로 DAO 에 의존하게 되는 문제가 발생하였다. 다행히도 트랜잭션의 경계설정을 담당하는 코드는 일정한 패턴을 갖는 유사한 구조이므로 추상화를 생각해 볼 수 있다.

스프링은 트랜잭션 기술의 공통점을 담은 트랜잭션 추상화 기술을 제공하고 있다. 이를 이용하면 애플리케이션에서 직접 각 기술의 트랜잭션 API 를 이용하지 않고도, 일관된 방식으로 트랜잭션을 제어하는 트랜잭션 경계설정 작업이 가능해진다.

```
public void upgradeLevels() {
   PlatformTransactionManager transactionManager = new DataSourceTransactionManager(dataSource); // JDBC 의 로컬 트랜잭션을 이용할 때
   TransactionStatus status = transactionManager.getTransaction(new DefaultTransactionDefinition()); // 트랜잭션 저장
   try {
      ...
      transactionManager.commit(status);
   } catch(RumtimeException e) {
      transactionManager.rollback(status);
      throw e;
   }
}
```

스프링이 제공하는 트랜잭션 경계설정을 위한 추상 인터페이스는 PlatformTransactionManager 다. JDBC 의 로컬 트랜잭션을 이용할 때는 DataSourceTransactionManager 클래스를 사용하고 JTA를 이용하는 글로벌 트랜잭션으로 변경하려면 JTATransactionManager 로 바꿔주면 된다. JTATransactionManager 는 주요 자바 서버에서 제공하는 JTA 정보를 JNDI 를 통해 자동으로 인식하는 기능을 갖고 있다. 하이버네이트는 HibernateTransactionManager 를, JPA 를 적용했다면 JPATransactionManager 를 사용하면 된다. 이를 DI 원칙에 맞게 수정해보도록 한다.

```
public class UserService {
   ...
   private PlatformTransactionManager transactionManager;
   public void setTransactionManager(PlatformTransactionManager transactionManager) {
      this.transactionManager = transactionManager;
   }

   public void upgradeLevels() {
      TransactionStatus status = this.transactionManager.getTransaction(new DefaultTransactionDefinition());
      ...
      try {
         ...
         this.transactionManager.commit(status);
      } catch(RumtimeException e) {
         this.transactionManager.rollback(status);
         throw e;
      }
   }
}
```

```
<bean id="userService" class="springbook...UserService">
   <property name="userDao" ref="userDao" />
   <property name="transactionManager" ref="transactionManager" />
</bean>
<bean id="transactionManager" class="org.springframework.jdbc.datasource.DataSourceTransactionManager">
   <property name="dataSource" ref="dataSource" />
</bean>
```

## 5.3 서비스 추상화와 단일 책임 원칙 ##

지금까지 기술과 서비스에 대한 추상화 기법을 이용해서 특정 기술환경에 종속되지 않는 포터블한 코드를 만들 수 있었다. UserDao 와 UserService 를 코드의 기능적인 관심에 따라 분리하고 서로 불필요한 영향을 주지 않으면서 독립적으로 확장이 가능하도록 만든 것이다. 이는 같은 애플리케이션 로직을 담은 코드지만 내용에 따라 분리하는 같은 계층에서의 수평적인 분리라고 볼 수 있다.

트랜잭션의 추상화는 애플리케이션의 비즈니스 로직과 그 하위에서 동작하는 로우레벨의 트랜잭션 기술이라는 아예 다른 계층의 특성을 갖는 코드를 분리한 것이다.

| UserService -> UserDao            | 애플리케이션 계층  |
|:----------------------------------|:-----------|
| TransactionManagger -> DataSource | 서비스 추상화 계층 |
| JDBC, JTA, JNDI, WAS, Database .. | 기술 서비스 계층   |

이렇게 애플리케이션 로직의 종류에 따른 수평적인 구분이든, 로직과 기술이라는 수직적인 구분이든 모두 결합도가 낮으며 서로 영향을 주지 않고 자유롭게 확장될 수 있는 구조를 만들 수 있는 데는 스프링의 DI 가 중요한 역할을 하고 있다.

**단일 책임 원칙 Single Responsibility Principle**

단일 책임 원칙은 하나의 모듈은 한 가지 책임을 가져야 한다는 의미다. 하나의 모듈이 바뀌는 이유는 한 가지여야 한다고 설명할 수 있다.

UserService 에 JDBC 의 Connection 메소드를 직접 사용하는 트랜잭션 코드가 들어 있었을 때는 사용자 레벨을 어떻게 관리할 것인가와 트랜잭션을 어떻게 관리할 것인가 하는 두 가지 책임을 가지고 있었지만 트랜잭션 서비스의 추상화 방식을 도입하고 이를 DI 를 통해 외부에서 제엏하도록 만들고 나서는 단일 책임을 가지게 되었다.

**단일 책임 원칙의 장점**

적절하게 책임과 관심이 다른 코드를 분리하고, 서로 영향을 주지 않도록 다양한 추상화 기법을 도입하고 애플리케이션 로직과 기술/환경을 분리하는 등의 작업을 통해 어떤 변경이 필요할 대 수정 대상이 명확해진다.

단일 책임 원칙을 잘 지키는 코드를 만들기 위해 인터페이스를 도입하고 이를 스프링이 제공하는 DI 로 연결하였고 그로 인해 개방 패쇄 원칙도 잘 지키고 모듈 간에 결합도가 낮아서 서로의 변경이 영향을 주지 않고 같은 이유로 변경이 단일 책임에 집중되는 응짐도 높은 코드가 나오게 되었다.

스프링의 DI 는 스프링을 DI 프레임워크라고 불릴 만큼 모든 스프링 기술의 기반이 핵심엔진이다.

## 5.4 메일 서비스 추상화 ##

스프링에서 제공하는 MailSender 인터페이스를 통해 테스트할 때 유용하게 쓰일 수 있다.

```
public class UserService {
   ...
   private MailSender mailSender;
   public void setMailSender(MailSender mailSender) {
      this.mailSender = mailSender;
   }
   ...
}
```

```
<bean id="userService" class="springbook...UserService">
   ...
   <property name="mailSender" ref="mailSender" />
</bean>
<bean id="mailSender" class="org.springframework.mail.javamail.JavaMailSenderImpl">
   <property name="host" value="..." />
   ...
</bean>
```

테스트 용 설정파일

```
<bean id="mailSender" class="springbook...DummyMailSender">
```

이렇게 스프링의 MailSender 를 이용하게 되면 JavaMail 이 아닌 다른 메시징 서버의 API 를 이용할 때도 MailSender 구현 클래스를 만들어서 DI 해주면 되는 장점이 생겨난다.

**테스트 대역**

  * 의존 오브젝트의 변경을 통한 테스트 방법

테스트 대상이 되는 코드를 수정하지 않고 UserService 자체에 대한 테스트에 지장을 주지 않기 위해 도입한 DummyMailSender 처럼 아무런 기능도 없지만 UserService 가 반드시 이용해야 하는 의존 오브젝트의 역할을 해주면서 원할하게 테스트 중에 UserService 의 코드가 실행되게 해준다. 이 같은 의존오브텍트를 협력오브젝트 collaborator 라고도 한다.

  * 테스트 대역의 종류와 특징

이렇게 테스트용으로 사용되는 특별한 오브젝트 들이 있다. 대부분 테스트 대상인 오브젝트의 의존 오브젝트가 되는 것들인데 UserDao 의 DataSource나 UserService 의 MailSender 인터페이스를 구현한 것들이다.  이런 오브젝트를 통틀어 테스트 대역 test double 이라고 부른다.

대표적인 테스트 대역은 테스트 스텁 test stub 이다. 일반적으로 테스트 스텁은 메소드를 통해 전달하는 파라미터와 달리 테스트 코드 내부에서 간접적으로 사용된다. 따라서 DI 를 통해 미리 의존 오브젝트를 테스트 스텁으로 변경해야 한다. DummyMailSender 가 테스트 스텁의 한 예다.

테스트 대상 오브젝트의 메소드가 돌려주는 결과 뿐 아니라 테스트 오브젝트가 간접적으로 의존 오브젝트에 넘기는 값과 그 행위 자체에 대해서도 검증하고 싶다면 테스트 대상의 간접적인 출력 결과를 검증하고 테스트 대상 오브젝트와 의존 오브젝트 사이에서 일어나는 일을 검증할 수 있도록 특별히 설계된 목 오브젝트 mock object 를 사용해야 한다.

  * 목 오브젝트를 이용한 테스트

```
static class MockMailSendeer implements MailSender {
   private List< String > requests = new ArrayList< String >();
   public List< String > getRequests() {
      return requests;
   }
   public void send(SimpleMailMessage mailMessage) throws MailException {
      requests.add(mailMessage.getTo()[0]);
   }
   public void send(SimpleMailMessage[] mailMessage) throws MailException { }
}
```

목 오브젝트로 만든 메일 전송 확인용 클래스이다. 테스트 스텁 처럼 메일을 발송 하는 기능은 없지만 관련 정보를 저장해두는 기능이 있다. 다음과 같이 테스트에서 MockMailSender 오브젝트로부터 UserService 사이에 일어난 일에 대한 결과를 검증할 수 있다.

```
@Test
@DirtiesContext
public void upgradeLevels() throws Exception {
   ...
   MockMailSender mockMailSender = new MockMailSender();
   userService.setMailSender(mockMailSender);
   ...
   List< String > request = mockMailSender.getRequests();
   assertThat(request.size(), is(2));
   assertThat(request.get(0), is(users.get(1).getEmail()));
   assertThat(request.get(1), is(users.get(3).getEmail())));
}
```

# 생각하기 #

목 오브젝트를 이용한 테스트를 보고 있자니 생각나는 일이 있다. 실제로 메일 발송 부분을 테스트 하기 위해서 발송대상 목록을 DB 에서 일단 받아서 확인만 하고 각 사이트별 내 메일계정으로 변경 하고 테스트 한 적이 있다. 이벤트인가 회원가입에 대한 메일이었는데 깜박하고 대상을 바꾸지 않아서 실제로 한명에게 발송되어서 고객대응하시는 분께 설명해두고 혹시라도 연락오면 알려달라고 했었다.