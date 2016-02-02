

# 내용정리 #

## 1.1 초난감 DAO ##

제목은 초난감DAO 이지만 많이 보고 익숙한 코드였다.


## 1.2 DAO의 분리 ##

**관심사의 분리 Separation of Concerns**
: 관심이 같은 것끼리는 하나의 객체 안으로 또는 친한 객체로 모이게 하고, 관심이 다른 것은 가능한 따로 떨어져서 서로 영향을 주지 않도록 분리하는 것

중복된 코드가 존재하는 UserDao 에 있는 2개의 메소드에서 중복 코드를 새로운 메소드로 추출해 보지만 (alt+shift+m이 떠오른다ㅋ)

```
public void add(User user) throws ClassNotFoundException, SQLException {
   Connection c = getConnection();
   ....
}

public void get(String id) throws ClassNotFoundException, SQLException {
   Connection c = getConnection();
   ....
}

private Connection getConnection() throws ClassNotFoundException, SQLException {
   ...
   return DriverManager.getConnection(....);
}
```

여전히 UserDao 에서 디비 접근 방법에 대한 정보를 가지고 있음이 문제가 되었다.

그래서 UserDao 를 추상클래스, 추상메소드로 변경하고 이를 상속한 각각의 Dao 가 getConnection 메소드를 오버라이딩 할 수 있게 하였다. 이 과정에서 두 가지 디자인 패턴이 사용되었다.

**[템플릿 메소드 패턴](http://underclub.tistory.com/124)** :
하위 클래스에서 어떤 구현을 하더라도 상위 클래스에서는 정해진대로 큰 흐름만을 처리한다.
**[팩토리 메소드 패턴](http://underclub.tistory.com/125)** :
상위 클래스에서 훅 메소드 혹은 추상 메소드를 오버라이딩하여 구현하며 슈퍼클래스의 기본 코드에서 독립시킨다.

```
public abstract class UserDao {
    public void add(...) { .......... }
    public void get(...) { .......... }
    public abstract Connection getConnection() throws ClassNotFoundException, SQLException;
}

public class NUserDao extends UserDao {
   public Connection getConnection() throw ClassNotFoundException, SQLException {
      // n사 connection 생성코드
   }
}

public class DUserDao extends UserDao {
   public Connection getConnection() throw ClassNotFoundException, SQLException {
      // d사 connection 생성코드
   }
}
```


하지만 상하위 클래스의 밀접한 관계를 가지는 상속의 단점을 그대로 안고 있는 UserDao 는 다시 리팩토링 되어야 할 것이다. 다중상속이 되지 않아 확장이 어려우며, 밀접한 연결 관계는 변화에 대해 개방적이다.


## 1.3 DAO의 확장 ##

상속의 단점인 확장성을 보완하고 독립을 시키기 위해 getConnection() 을 클래스(SimpleConnectionMaker.java)로 뺀다. 이 때 UserDao 코드를 확인해 보면 생성자에서 SimpleConnectionMaker 를 직접 호출하고 있다.

```
public Class UserDao {
   private SimpleConnectionMaker simpleConnectionMaker;
   public UserDao() {
      simpleConnectoinMaker = new SimpleConnectionMaker();
   }
}
```

이는 N사와 D사가 각자 확장해서 가져갔던 기능을 다시 사용하지 못하게 되었다. UserDao 의 코드가 SimpleConnectionMaker 라는 특정 클래스에 종속되어 있기 때문이다.

**인터페이스의 도입**

클래스를 분리하면서 확장성까지 보완할 수 있는 방법으로 (드디어) 인터페이스가 나왔다. 서로 긴밀한 연결을 갖지 않고 중간에 추상적인 느슨한 연결고리를 만들어 줄 수 있게 되었다.

이렇게 되면 UserDao 는 인터페이스의 메소드를 통해 알 수 있는 기능에만 관심을 가지고, 어떻게 그 기능이 구현되었는지는 알 필요가 없어졌다.

```
public interface ConnectionMaker {
   public Connection makeConnectoin() throws ClassNotFoundException, SQLException;
}

public class DConnectionMaker implements ConnectionMaker {
   ...
   public Connection makeConnection() throws ClassNotFoundException, SQLException {
      // d사 connection 생성코드
   }
}

public class UserDao {
   private ConnectionMaker connectionMaker;
   public UserDao() {
      connectionMaker = new DConnectionMaker();
   }
   .....
}
```

휴- 아직도 갈 길이 멀다. UserDao 생성자에 아직도 클래스 이름이 들어가고 있다. DConnectionMaker 라는 클래스를 알아버려서 불 필요한 의존관계가 생겨나버렸다.

**관계설정 책임의 분리**

우리는 이 쓸데없는 관심을 분리해서 클라이언트에게 "너가 던져줘!" 라고 해줄 것이다. UserDao 에서는 파라미터로 ConnectionMaker 를 구현한 클래스를 받아서 직접적인 연결을 사라지게 할 것이다.

```
public class UserDao {
   private ConnectionMaker connectionMaker;
   public UserDao(ConnectionMaker connectionMaker) {
      this.connectionMaker = connectionMaker;
   }
   .....
}

public class UserDaoTest {
   public static void main(String[] args) throws ClassNotFoundException, SQLException {
      ConnectionMaker connectionMaker = new DConnectionMaker();
      UserDao dao = new UserDao(connectionMaker);
   }
}
```

UserDaoTest 는 UserDao 와 ConnectionMaker 와의 런타임 오브젝트 의존관계를 설정하는 책임을 담당하게 되었다. 이렇게 인터페이스 도입과 클라이언트의 도움을 받는 방법으로 상속을 사용했을 때 보다 훨씬 유연해졌다. ConnectoinMaker 라는 인터페이스를 구현한 클래스라면 그대로 적용할 수 있게 되었다.

이 과정에서 우리는 다음과 같은 원칙과 패턴을 발견할 수 있었다.

**개방폐쇄원칙 Open-Closed Principle** : 클래스나 모듈은 확장에는 열려 있어야 하고 변경에는 닫혀 있어야 한다.

**높은 응집도와 낮은 결합도** :


## 1.4 제어의 역전 ##


## 1.5 스프링의 IoC ##


## 1.6 싱글톤 레지스트리와 오브젝트 스코프 ##


## 1.7 의존관계 주입(DI) ##


## 1.8 XML을 이용한 설정 ##

애노테이션으로 설정된 application context 클래스를 XML 로 변경해본다.
ref 속성으로 여전히 의존관계를 설정할 수 있다.
(d사와 n사는 각자 가져가서 알아서 쓸테지만 ref 를 잘 설명할 수 있을 것 같아 예제를 좀 바꾸어 보았다.)

**applicationContext.xml**
```
<beans>
  <bean id="dConnectionMaker" class="springbook...DConnectionMaker" />
  <bean id="nConnectionMaker" class="springbook...NConnectionMaker" />

  <bean id="userDao" class="springbook...UserDao">
     <property name="connectionMaker" ref="dConnectionMaker" />
  </bean>
</beans>
```

xml을 이용해 application context 를 만들었으니 생성자도 같이 바뀐다.

```
ApplicationContext context = new GenericXmlApplicionContext("applicationContext.xml");
```

**DataSource 구현 클래스 이용하기**

(결국 이 길고 긴 설명을 통해 dataSource DI 까지 겨우 왔다.
당연히 가져다 쓰면 되겠지~ 했던 이 dataSource 까지도 아 네가 이 과정을 거쳐왔구나 하고 깊게 생각하게 된다ㅋ)

```
<bean id="dataSource" class="org.springframewokr.jdbc.datasource.SimpleDriverDataSource">
   <property name="driverClass" value="com.mysql.jdbc.Driver" />
   <property name="url" value="jdbc:mysql://localhost/springbook" />
   <property name="username" value="spring" />
   <property name="password" value="book" />
</bean>

<bean id="userDao" class="springbook...UserDao">
   <bean id="dataSource" ref="dateSource" />
</bean>
```


나는 처음 개발할 때 applicationContext 를 xml 로만 작성해서인지
애노테이션에 적응이 덜 되서 인지
한 눈에 파악할 수 있는 XML 방식이 더 편하고 좋은 것 같다.
여러분의 생각은 어떠신지요??


# 생각하기 #

"여기 xml 에서 정의하시고 블라블라블라~ 그리고 setter 없으면 에러나요!"
"스트러츠했었다고요? 그럼 금방하겠네~ 비슷해요~"
약 1년전, 스트러츠만 하다가 스프링프로젝트에 처음 들어왔을 때
동료가 설명해준 스프링에 대한 나의 기억이다.

이 기억들은 스프링교육이나 책을 공부하면서 점점 의미를 더해가고 있다.
그렇다고 왜 에러가 나는지 몰랐다는 뜻은 아니다. 다만 IoC, DI 라는 개념을 생각해 봤을 때
나는 그저 "그냥 그래야만해~" 라고 깊게 생각하지 않고 개발을 해왔던 것 같다.

왜 xml 이 굳이 분리되서 관리가 되는지, 왜 이렇게 많은지 복잡하다고 생각도 했었다.

스프링에서 제일 매력있는 부분은 객체지향의 장점을 살리는 부분이 아닐까 싶다.