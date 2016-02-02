

# 내용정리 #

## 6.1 트랜잭션 코드의 분리 ##

upgradeLevels 메소드는 다음과 같이 구성되어 있다.

```
public void upgradeLevels() throws Exception {
   // 트랜잭션경계설정

   // 비즈니스 로직

   // 트랜잭션경계설정
}
```

트랜잭션 경계설정 부분을 클래스로 빼고, 이를 DI 의 실제 사용할 오브젝트 클래스를 감추고 인터페이스를 통해 간접적으로 접근하게 하는 개념을 이용해본다.

UserService 인터페이스로 만들고 기존의 소스는 UserService 를 구현하면서 비즈니스로직만 담고 있는  UserServiceImpl 클래스로 만든다. 그리고 UserService 를 구현하면서 트랜잭션경계설정만 담고 있는 UserServiceTx 클래스도 만든다. 그래서 UserServiceTx 에게 실제적인 로직 처리 작업을 위임한다.

```
public class UserServiceTx implements UserService {
   UserService userService;
   public void setUserService(UserService userService) {
      this.userService = userService;
   }

   PlatformTransactionManager transactionManager;
   public void setTransactionManager(PlatformTransactionManager transactionManager) {
      this.transactionManager = transactionManager;
   }

   public void add(User user) { userService.add(user); }
 
   public void upgradeLevels(User user) { 
      TransactionStatus status = this.transactionManager.getTransaction(new DefaultTransactionDefinition());
      try {
         userService.upgradeLevels(); 
         this.transactionManager.commit(status);
      } catch (RuntimeException e) {
         this.transactionManager.rollback(status);
         throw e;
      }
   }
}
```

기능을 분리 후 구성관계는 다음과 같다.

Client(UserServiceTest) -> UserServiceTx -> UserServiceImpl

xml 설정파일도 다음과 같이 수정한다.

```
<bean id="userService" class="springbook...UserServiceTx">
   <property name="transactionManager" ref="transactionManager" />
   <property name="userService" ref="userServiceImp;" />
</bean>
<bean id="userServiceImpl" class="springbook...UserServiceImpl">
   <property name="userDao" ref="userDao" />
   <property name="mailSender" ref="mailSender" />
</bean>
```

TestUserSerivce 는 UserServiceImpl 을 상속받도록 수정한다.

```
static class TestUserSerivce extends UserServiceImpl {
   ...
}
```

UserServiceTest를 수정한다.

```
@Autowired
UserService userService; // type 이 두개 존재하면 이름으로 찾는다.
@Autowired
UserServiceImpl userServiceImpl;

@Test
public void upgradeAllOrNothing() throws Exception {
   TestUserService testUserService = new TestUserService(users.get(3).getId());
   testUserService.setUserDao(userDao);
   testUserService.setMailSender(mailSender);

   UserServiceTx txUserService = new UserServiceTx();
   txUserService.setTransactionManager(transactionManager);
   txUserService.setUserService(testUserService);

   ...
   try {
      txUserService.upgradeLevels();
      fail("TestUserServiceException expected");
   }
   ...
```


## 6.2 고립된 단위 테스트 ##

UserServiceTest 라는 테스트 대상이 테스트 단위인 것처럼 보이지만 사실은 그 뒤의 의존관계를 따라 등장하는 오브젝트와 서비스, 환경이 모두 합쳐져 테스트 대상이 되고 있는 것이다. 그래서 테스트의 대상, 환경 등에 종속되고 영향 받지 않도록 고립시킬 필요가있다. MockMailSender 를 이용한 것 처럼 MockUserDao 를 만들어서 사용한다.

```
static class MockUserDao extends UserDao {
   private List<User> users;
   private List<User> updated = new ArrayList();

   private MockUserDao(List<User> users) { this.users = users; }
   private List<User> getUpdated() { return this.updated; }

   public List<User> getAll() { return this.users; }
   public void update(User user) { updated.add(user); }

   public void add(User user) { throw new UnsupportedOperationException(); }
   ...
}
```

```
@Test
public void upgradeLevels() throws Exception {
   UserServiceImpl userServiceImpl = new UserServiceImpl();

   MockUserDao mockUserDao = new MockUserDao(this.users);
   userServiceImpl.setUserDao(mockUserDao);
 
   MockMailSender mockMailSender = new MockMailSender();
   userServiceImppl.setMailSender(mockMailSender);

   userServiceImpl.upgradeLevels();

   List<User> updated = mockUserDao.getUpdated();
   assertThat(updated.size(), is(2));

   ...
}
```

앞으로 '테스트 대상 클래스를 목 오브젝트 등의 테스트 대역을 이용해 의존 오브젝트나 외부의 리소스를 사용하지 않도록 고립시켜서 테스트하는 것'을 단위 테스트라고 보고, 2개 이상의 성격이나 계층이 다른 오브젝트가 연동하도록 만들어 테스트 하거나, 외부의 DB 나 파일, 서비스 등의 리소스가 참여하는 2개 이상의 단위가 결합해서 동작하는 테스트를 통합 테스트라고 보겠다.

항상 단위 테스트를 먼저 고려해야 하며 DAO 테스트는 DB 외부 리소스를 사용하기 때문에 통합테스트로 분류된다. 스프링 테스트 컨텍스트 프레임워크를 이용하는 테스트는 통합 테스트이다.

**목 프레임워크**

목 프레임웍 중 Mockito 프레임워크는 콕 클래스를 일일이 준비해둘 필요가 없다. 다음과 같이 스태틱 메소드인 mock 으로 호출하게 한다.

```
UserDao mockUserDao = mock(UserDao.class);
when(mockUserDao.getAll()).thenReturn(this.users); // 스텁 기능 추가
verify(mockUserDao, times(2)).update(any(User.class)); // 두번 호출됐는지 확인
```

```
@Test
public void mockUpgradeLevels() throws Exception {
   UserServiceImpl userServiceImpl = new UserServiceImpl();
   
   UserDao mockUserDao = mock(UserDao.class);
   when(mockUserDao.getAll()).thenReturn(this.users);
   userServiceImpl.setUserDao(mockUserDao);

   MailSender mockMailSender = mock(MailSender.class);
   userServiceImp.setMailSender(mockMailSender);

   ...
   
   verify(mockUserDao, times(2)).update(any(User.class));
   ...

   // 전달된 파라미터를 가져와 내용을 검증
   ArgumentCaptor<SimpleMessage> mailMessageArg = ArgumentCaptor.forClass(SimpleMessage.class);
   verify(mockMailSender, times(2)).send(mailMessageArg.capture());
   List<SimpleMailMessage> mailMessages = mailMessageArg.getAllValues();
   assertThat(mailMessages.get(0).getTo()[0], is(users.get(1).getEmail()));
   ...
}
```


## 6.3 다이내믹 프록시와 팩토리 빈 ##

트랜잭션 부가기능과 비즈니스 로직 핵심기능을 분리하고 클라이언트에서 부가기능을 가진 클래스를 이용하고, 해당 클래스에서 다시 비즈니스 로직을 가진 핵심기능을 사용하는 구조가 되었다.

만약 클라이언트가 핵심기능을 가진 클래스를 직접 사용한다면 부가기능이 적용될 기회가 없게 된다. 그래서 부가기능을 가진 클래스는 핵심기능을 가진 클래스와 같은 인터페이스를 구현해서 클라이언트와 핵심기능을 가진 클래스 사이에 마치 자신이 핵심기능을 가진 클래스인 것 처럼 끼어들어야 한다.

이렇게 마치 자신이 클라이언트가 사용하려고 하는 실제 대상인 것처럼 위장해서 클라이언트의 요청을 받아주는 것을 프록시라고 부르며, 프록시를 통해 최종적으로 요청을 위임받아 처리하는 실제 오브젝트를 타깃이라고 부른다.

프록시의 사용 목적은 두 가지로 구분할 수 있다. 첫째, 클라이언트가 타깃에 접근하는 방법을 제어하기 위해서다. 두 번째는 타깃에 부가적인 기능을 부여해주기 위해서이다. 이 두 가지 목적에 따라서 디자인 패턴에서는 다른 패턴으로 구분한다.

**데코레이터 패턴**

타깃에 부가적인 기능을 런타임 시 다이내믹하게 부여해주기 위해 프록시를 사용하는 패턴이다. 대표적인 예로 BufferedInputStream 이 있다. 트랜잭션 부가기능을 부여해준 UserServiceTx 도 이에 속한다.

```
   InputStream is = new BufferedInputStream(new FileInputStream("a.txt"));
```

**프록시 패턴**

프록시를 사용하는 방법 중에서 타깃에 대한 접근 방법을 제어하려는 목적을 가진 경우다. add 메소드에서 UpsupportedOperationException 예외가 발생하게 한 것도 접근권한 제어용 프록시라고 볼 수 있다.

**다이내믹 프록시**

프록시는 기존 코드에 영향을 주지 않으면서 타깃의 기능 확장, 접근 방법을 제어할 수 있는 유용한 방법이지만 일일이 모든 인터페이스를 구현해서 클래스를 새로 정의해야 하는 불편함이 있다.

프록시는 타깃오브젝트로 위임하는 기능과 부가기능 수행으로 기능이 나뉠 수 있다. 이 때 부가기능 코드가 중복될 가능성이 많으며, 일일이 만들어 줘야 하는 단점을 JDK 의 java.lang.reflect 리플렉션기능을 이용해서 해결할 수 있다.

다이내믹 프록시는 이런 리플렉션 기능을 이용해서 프록시를 만들어준다. 이는 자바의 코드 자체를 추상화해서 접근하도록 만든 것이다.

```
public class RelectionTest {
   @Test
   public void invokeMethod() throws Exception {
      String name = "Spring";
      
      assertThat(name.length(), is(6));
      Method lengthMethod = String.class.getMethod("length");
      assertThat((Integer)lengthMethod.invoke(name), is(6));      
   }
}
```

다이내믹 프록시를 이용한 프록시를 만들어 본다.

```
interface Hello { // 인터페이스
   String sayHello(String name);
   String sayHi(String name);
   String sayThankYou(String name);
}
```

```
public class HelloTarget implements Hello { // 구현 클래스
   public String sayHello(String name) { return "Hello" + name; }
   public String sayHi(String name) { return "Hi" + name; }
   public String sayThankYou(String name) { return "ThankYou" + name; }
}
```

```
public class UppercaseHandler implements InvocationHandler { // InvocationHandler 구현
   Object target;
   private UppercaseHandler(Object target) { this.target = target; }
   public Object invoke(Object proxy, Method method, Object[] args) throws Throwble {
      Object ret = method.invoke(target, args);
      if(ret instanceof String) { // 리턴타입이 String 인 경우에만
         return ((String)ret).toUpperCase(); // 대문자로 변경
      } else { return ret; }
   }
}
```

```
Hello proxiedHello = (Hello) Proxy.newProxyInstance(
   getClass().getClassLoader(), // 클래스 로더
   new Class[] { Hello.class }, // 구현할 인터페이스의 배열
   new UppercaseHandler(new HelloTarget())); // 부가기능과 위임코드를 담은 InvocationHandler 
```

InvocationHandler 로 인해 어떤 종류의 인터페이스를 구현한 타깃이든 상관없이 재사용할 수 있고, 호출하는 메소드의 이름, 파라미터 개수와 타입, 리턴 타입등의 정보를 가지고 부가적인 기능을 적용할 메소드를 선택할 수 있다.

**다이내믹 프록시를 이용한 트랜잭션 부가기능**

```
public class TransactionHandler implements InvocationHandler {
   private Object target;
   private PlatformTransactionManager;
   private String pattern;

   public void setTarget(Object target) { this.target = target; }
   public void setTransactionManager(PlatformTransactionManager transactionManager) { this.transactionManager = transactionManager; }
   public void setPattern(String pattern) { this.pattern = pattern; }

   public Object invoke(Object proxy, Method method, Object[] args) throws Throwble {
      if(method.getName().startsWith(pattern)) {
         return invokeInTransaction(method, args);
      } else {
         return method.invoke(target, args);
      }
   }

   private Object invokeInTransaction(Method method, Object[] args) throws Throwble {
      TransactionStatus status = this.transactionManager.getTransaction(new DefaultTransactionDefinition());
      try {
         Object ret = method.invoke(target, args);
         this.transactionManager.commit(status);
         return ret;
      } catch (InvocationTargetException e) { // InvocationTargetException 으로 포장되서 전달됨
         this.transactionManager.rollback(status);
         throw e.getTargetException();
      }
   }
}
```

```
@Test
public void upgradeAllOrNothing() throws Exception {
   ...
   TransactionHandler txHandler = new TransactionHandler();
   txHandler.setTarget(testUserService);
   txHandler.setTransactionManager(transactionManager);
   txHandler.setPattern("upgradeLevels");
   UserService txUserService = (UserService) Proxy.newProxyInstance(
      getClass().getClassLoader(),
      new Class[] { UserService.class }, 
      txHandler);
   ...
}
```

**다이내믹 프록시를 위한 팩토리 빈**

다이내믹 프록시를 이용해서 UserService 를 구성해봤지만 이를 DI 할 수가 없는 문제점이 발생했다. 스프링에서는 클래스 정보를 가지고 디폴트 생성자를 통해 오브젝트를 만드는 방법 외에도 팩토리 빈을 이용한 생성방법이 있다. 팩토리 빈은 스프링을 대신해서 오브젝트의 생성로직을 담당하도록 만들어진 특별한 빈을 말한다.

```
public class Message {
   String text;
   private Message(String text) { this.text = text; }
   public String getText() { return text; }
   public static Message newMessage(String text) { return new Message(text); }
}
```

```
public class MessageFactoryBean implements FactoryBean<Message> {
   String text;
   public void setText(String text) { this.text = text; }
   public Message getObject() throws Exception {
      return Message.newMessage(this.text);
   }
   public Class<? extends Message> getObjectType() {
      return Message.class;
   }
   public boolean isSingleton() { return false; }
}
```

```
<bean id="message" class="springbook...MessageFactoryBean">
   <property name="text" value="Factory Bean" />
</bean>
```

```
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class FactoryBeanTest {
   @Autowired
   ApplicationContext context;
   @Test
   public void getMessageFromFactoryBean() {
      Object message = context.getBean("message");
      assertThat(message, is(Message.class)); // 타입이 MessageFactoryBean 이 아닌 getObject 의 리턴값인 Message 이다.
      ...
      Object factory = context.getBean("&message"); // MessageFactoryBean 자체를 리턴
      assertThat(factory, is(MessageFactoryBean.class)); 
   }
}
```

TransactionHandler를 이용하는 다이내믹 프록시를 생성하는 팩토리 빈 클래스를 만들어 본다.

```
public class TxProxyFactoryBean implements FactoryBean<Object> { // 범용적으로 사용
   private Object target;
   private PlatformTransactionManager;
   private String pattern;
   Class<?> serviceInterface;

   public void setTarget(Object target) { this.target = target; }
   public void setTransactionManager(PlatformTransactionManager transactionManager) { this.transactionManager = transactionManager; }
   public void setPattern(String pattern) { this.pattern = pattern; }
   public void setServiceInterface(Class<?> serviceInterface) { this.serviceInterface = serviceInterface;

   public Object getObject() throws Exception {
      TransactionHandler txHandler = new TransactionHandler();
      txHandler.setTarget(target);
      txHandler.setTransactionManager(transactionManager);
      txHandler.setPattern(pattern);
      return Proxy.newProxyInstance(
         getClass().getClassLoader(),
         new Class[] { serviceInterface },
         txHandler);
   }

   public Class<?> getObjectType() {
      return serviceInterface;
   }

   public boolean isSingletom() { return false; }
}
```

```
<bean id="userService" class="springbook...TxProxyFactoryBean">
   <property name="target" ref="userServiceImpl" />
   <property name="transactionManager" ref="transactionManager" />
   <property name="pattern" value="upgradeLevels" />
   <property name="serviceInterface" value="springbook...UserService" />
</bean>
```

테스트의 upgradeAllOrNothing 메소드는 수동 DI 를 통해 직접 다이내믹 프록시를 만들어서 사용하니 팩토리 빈이 적용되지 않는다. 그래서 빈으로 등록된 TxProxyFactoryBean 을 가져와서 프록시를 만들어 볼 수 있다.

```
public class UserServiceTest {
   ...
   @Autowired ApplicationContext context;
   ...
   @Test
   @DirtiesContext
   public void upgradeAllOrNothing() throws Exception {
      ...
      TxProxyFactoryBean txProxyFactoryBean = context.getBean("&userService", TxProxyFactoryBean.class);
      txProxyFactoryBean.setTarget(testUserServiceImpl);
      UserService txUserService = (UserService) txProxyFactoryBean.getObject();
      ...
   }
```

프록시 팩토리 빈을 사용해서 타깃 인터페이스를 구현하는 클래스를 일일이 만드는 번거로움을 제거했다. 하나의 핸들러 메소드를 구현하는 것만으로 수많은 메소드에 부가기능을 부여해줄 수 있으니 부가기능 코드의 중복 문제도 사라졌다.

하지만 프록시를 통해 타깃에 부가기능을 제공하는 것은 메소드 단위로 일어나는 일으로 하나의 클래스 안에 존재하는 여러 개의 메소드에 부가기능을 한 번에 제공하는 건 어렵지 않았지만 한 번에 여러개의 클래스에 공통적인 부가기능을 제공할 수는 없다.

또한 하나의 타깃에 여러개의 부가기능을 적용할 수 없다.

그리고 TransactionHandler 오브젝트가 프로시 팩토리 빈 개수만큼 만들어지는 문제점이 있다. TransactionHandler 는 타깃 오브젝트를 프로퍼티로 갖고 있기 때문에 트랙잭션 부가기능을 제공하는 동일한 코드임에도 불구하고 타깃 오브젝트가 달라지면 새로운 TransactionHandler 오브젝트를 만들어내야 한다.


## 6.4 스프링의 프록시 팩토리 빈 ##

지금까지의 과정을 스프링에서 어떻게 제공하고 있는지 확인해본다.

스프링에서 제공하는 ProxyFactoryBean 은 TxProxyFactoryBean 과 달리 순수하게 프록시를 생성하는 작업만을 담당하고 프록시를 통해 제공해줄 부가기능은 별도의 빈에 둘 수 있다. ProxyFactoryBean 은 MethodInterceptor 인터페이스를 구현한 부가기능을 사용하는데 이는 InvocationHandler 와는 달리 타깃 오브젝트에 대한 정보도 함께 제공받는다.

```
public class DynamicProxyTest {
   ...
   @Test
   public void proxyFactoryBean() {
      ProxyFactoryBean pfBean = new ProxyFactoryBean();
      pfBean.setTarget(new HelloTarget());
      pfBean.addAdvice(new UppercaseAdvice());
      
      Hello proxiedHello = (Hello) pfBean.getObject();
      assertThat(proxiedHello.sayHello("toby"), is("HELLO TOBY"));
      ...
   }

   static class UppercaseAdvice implements MethodInterceptor {
      public Object invoke(MethodInvocation invocation) throws Throwable {
         String ret = (String) invocation.proceed();
         return ret.toUpperCase();
      }
   }
   ...
}
```

MethodInvocation은 일종의 콜백 오브젝트로, proceed() 메소드를 실행하면 타깃 오브젝트의 메소드를 내부적으로 실행해주는 기능이 있다. 바로 이점이 JDK 의 다이내믹 프록시를 직접 사용하는 코드와 스프링이 제공해주는 ProxyFactoryBean 을 사용하는 코드의 가장 큰 차이점이며 장점이다. 템플릿 역할을 하는 MethodInvocation을 싱글톤으로 두고 공유할 수 있다.

ProxyFactoryBean 에 MethodInterceptor 를 설정해 줄 때 set 이 아닌 addAdvice 메소드를 사용하는 것은 여러개의 MethodInterceptor 를 추가할 수 있다는 점으로 앞서 봤던 프록시 팩토리 빈의 단점을 해결할 수 있다.

또한 Hello 라는 인터페이스를 제공해주는 부분도 없어졌다. setInterfaces() 메소드를 통해서 구현할 수 있지만 ProxyFactoryBean 에 있는 인터페이스 자동 검출 기능을 사용해서 정보를 알아낸다.

이렇듯 어드바이스는 타깃 오브젝트에 종속되지 않는 순수한 부가기능을 담은 오브젝트이다.

**포인트컷**

invocationHandler 를 구현했을 때는 pattern 정보를 직접 받아 트랜잭션 적용 대상을 판별하였다. 스프링의 MethodInterceptor 는 여러 프록시가 공유해서 사용하므로 여기에 트랜잭션 적용 대상 이름 패턴을 넣어주는 것은 곤란하다. 대신 프록시에 부가기능 적용 메소드를 선택하는 기능을 넣도록 한다.

프록시는 클라이언트로부터 요청을 받으면 먼저 포인트컷에세 부가기능을 부여할 메소드인지를 확인해달라고 요청한다. 포인트컷은 Pointcut 인터페이스를 구현해서 만들면 된다. 프록시는 포인트컷으로부터 부가기능을 적용할 대상 메소드인지 확인 받으면 MethodInterceptor 타입의 어드바이스를 호출한다.

```
@Test
public void pointcutAdvisor() {
   ProxyFactoryBean pfBean = new ProxyFactoryBean();
   pfBean.setTarget(new HelloTarget());

   NameMatchMethodPointcut pointcut = new NameMatchMethodPointcut();
   pointcut.setMappedName("sayH*");
   
   pfBean.setAdvisor(new DefaultPointcutAdvisor(pointcut, new UppercaseAdvice()));
   ...
   assertThat(proxiedHello.sayThankYou("toby"), is("Thank You toby"));
}
```

어드바이저 = 포인트컷 + 어드바이스

**ProxyFactoryBean 적용**

```
...
public class TransactionAdvice implements MethodInterceptor {
   PlatformTransactionManager transactionManager;
   public void setTransactionManager(PlatformTransactionManager transactionManager) {
      this.transactionManager = transactionManager;
   }
   public Object invoke(MethodInvocation invocation) throws Throwble {
      TransactionStatus status = this.transactionManager.getTransaction(new DefaultTransactionDefinition());
      try {
         Object ret = invocation.proceed();
         this.transactionManager.commit(status);
         return ret;
      } catch (RuntimeException e) { // InvocationHandler 와 달리 InvocationTargetExceptino 으로 포장돼서 오지 않는다.
         this.transactionManager.rollback(status);
         throw e;
      }
   }
}
```

```
...
<bean id="transactionPointcut" class="org.springframework.aop.support.NameMatchMethodPointcut">
   <property name="mappedName" value="upgrade*" />
</bean>

<bean id="transactionAdvice" class="org.springframework.aop.support.DefaultPointcutAdvisor">
   <property name="advice" ref="transactionAdvice" />
   <property name="pointcut" ref="transactionPointcut" />
</bean>

<bean id="userService" class="org.springframework.aop.framework.ProxyFactoryBean">
   <property name="target" ref="userServiceImpl" />
   <property name="interceptorNames">
      <list>
         <value>transactionAdvisor</value>
      </list>
   </property>
</bean>
...
```

```
@Test
@DirttiesContext
public void upgradeAllOrNothing() {
   ...
   ProxyFactoryBean txProxyFactoryBean = context.getBean("&userService", ProxyFactoryBean.class);
   txProxyFactoryBean.setTarget(testUserService);
   UserService txUserService = (UserService) txProxyFactoryBean.getObject();
   ...
```

ProxyFactoryBean 은 DI, 템플릿/콜백, 서비스 추상화 등의 기법이 모두 적용되었다. 그 덕분에 독립적이고 여러 프록시가 공유할 수 있는 어드바이스와 포인트컷으로 확장 기능을 분리할 수 있어졌다.

## 6.5 스프링 AOP ##

스프링의 ProxyFactoryBean 을 통해 거의 모든 문제를 해결했지만 부가기능의 적용이 필요한 타깃 오브젝트마다 거의 비슷한 내용의 ProxyFactoryBean 빈 설정정보를 추가해주는 부분이다.

이는 BeanPostProcessor 인터페이스를 구현해서 만드는 빈 후처리기를 통해 해결할 수 있다. 그 중 하나인 DefaultAdvisorAutoProxyCreator 가 빈으로 등록되어 있으면 빈 오브젝트가 생성될때마다 빈 후처리기에 보내서 후처리 작업을 요청한다.

ProxyFactoryBean 에서의 포인트컷은 메소드 선택만 필요했지만 사실 getClassFilter 메소드를 통해 클래스도 거를 수 있다.

```
...
public class NameMatchClassMethodPointcut extends NameMatchMethodPointcut {
   public void setMappedName(String mappedClassName) {
      this.setClassFilter(new SimpleClassFilter(mappedClassName));
   }
   static class SimpleClassFilter implements ClassFilter {
      String mappedName;
      private SimpleClassFilter(String mappedName) {
         this.mappedName = mappedName;
      }
      public boolean matches(Class<?> clazz) {
         return PatternMatchUtils.simpleMatch(mappedName, clazz.getSimpleName());
      }
   }
}
```

```
...
/* id 는 필요없다. 자동 프록시 생성기 등록 */
<bean class="org.springframework.aop.framework.autoproxy.DefaultAdvixorAutoProxyCreator" /> 
...
<bean id="transactionPointcut" class="springbook...NameMatchClassMethodPointcut">
   <property name="mappedClassName" value="*ServiceImpl" />
   <property name="mappedName" value="upgrade*" />
</bean>
...
<bean id="userService" class="springbook...UserServiceImpl"> /* 원상복구 */
   <property name="userDao" ref="userDao" />
   <property name="mailSender" ref="mailSender" />
</bean>
...
<bean id="testUserService" class="springbook...UserServiceImpl$TestUserServiceImpl" parent="userService" /> /* 스태틱 멤버 클래스, parent 로 userService 빈의 설정을 상속 받음 */
...
```

```
public class UserServiceTest {
   @Autowired
   UserService userService;
   @Autowired
   UserService testUserService;
   ...
   @Test
   public void upgradeAllOrNothing() {
      ...
      try {
         this.testUserService.upgradeLevels();
         ...
      } ...
      ...
   }
   @Test
   public void advisorAutoProxyCreator() { // 프록시로 변경된 오브젝트인이 확인
      assertThat(testUserService, is(java.lang.reflect.Proxy.class));
   }
}
```

**포인트컷 표현식**

앞서 만들었던 NameMatchClassMethodPointcut 은 클래스와 메소드의 이름의 패턴을 독립적으로 비교하도록 두 가진 패턴을 프로퍼티로 넣어줬지만 AspectJExpressionPointcut 은 클래스와 메소드의 선정 알고리즘을 포인트컷 표현식을 이용해 한번에 지정할 수 있게 해준다.

```
execution([접근제한자 패턴] 리턴타입패턴 [패키지와 클래스타입패턴.]메소드이름패턴 (파라미터타입패턴 | "..", ...) [throws 예외패턴]

bean(*Service)  // 아이디가 Service 로 끝나는 모든 빈

@annotation(org.springframework.transaction.annotation.Transactional)
```

```
<bean id="transactionPointcut" class="org.springframework.aop.aspectj.AspeectJExpressionPointcut">
   <property name="expression" value="execution(* *..*ServiceImpl.upgrade*(..))" />
</bean>
```

이 때, TestUserSerivce 의 클래스 명이 TestUserService 여도 포인트컷 표현식에 해당 된다. 슈퍼클래스가 UserServiceImple 이고 UserService 를 구현하였기 때문에 3가지 모두 해당된다.

**트랜잭션 서비스 추상화**

트랜잭션 적용이라는 추상적인 작업내용은 유지한 채로 구체적인 구현방법을 자유롭게 바꿀 수 있도록 서비스 추상화 기법을 적용했다. 이 덕분에 비즈니스 로직 코드는 트랜잭션을 어떻게 처리해야 한다는 구체적인 방법과 서버환경에서 종속되지 않으며 구체적인 구현 내용을 담은 의존 오브젝트는 런타임 시에 다이내믹하게 연결해 준다는 DI를 활용한 전형적인 접근 방법이었다.

**프록시와 데코레이터 패턴**

추상화를 통해 마저 제거하지 못한 트랜잭션의 경계설정을 담당하는 코드는 DI 를 이용한 데코레이터 패턴을 적용하는 것이었다. 트랜잭션을 처리하는 코드는 일종의 데코레이터에 담겨서, 클라이언트와 비즈니스 로직을 담은 타깃 클래스 사이에 존재하도록 만들었다. 클라이언트가 일종의 대리자인 프록시 역할을 하는 트랜잭션 데코레이터를 거쳐서 타깃에 접근할 수 있게 하였다.

**다이내믹 프록시와 프록시 팩토리 빈**

프록시를 이용해 비즈니스 로직에서 트랜잭션 코드를 모두 제거할 수 있었지만, 모든 메소드마다 해당 코드를 넣어 프록시 클래스를 만드는 작업이 문제가 됐다. 이는 JDK 다이내믹 프록시 기술을 적용해서 프록시 클래스 없이도 프록시 오브젝트를 런타임 시에 만들어 주게 하였지만 동일한 기능의 프록시를 여러 오브젝트에 적용할 경우 오브젝트 단위로는 중복이 일어나는 문제를 해결하지 못했고 이는 스프링의 ProxyFactoryBean 을 이용해서 다이내믹 프록시 생성방법에 DI 를 도입했다. 이는 내부적으로 템플릿/콜백 패턴을 활용하므로서 어드바이스와 포인트컷은 프록시에서 분리될 수 있엇고 여러 프록시에서 공유해서 사용할 수 있게 되었다.

**자동 프록시 생성 방법과 포인트컷**

트랜잭션 적용 대상이 되는 빈마다 일일이 프록시 팩토리 빈을 설정해줘야 하는 부담을 빈 생성 후처리 기법을 활용해 컨테이너 초기화 시점에서 자동으로 프록시를 만들어주는 방법을 도입했다. 처음에는 클래스와 메소드 선정 로직을 담은 코드를 직접 만들어서 포인트컷으로 사용했지만 최종적으로는 포인트컷 표현식을 통해 좀 더 편리하고 깔끔한 방법을 활용해서 사용할 수 있었다.

**부가기능의 모듈화**

트랜잭션이라는 부가기능은 타깃이 존재해야만 의미가 있는 것이었다. 그래서 각 타깃의 모드안에 침투하거나 긴밀한 연결이 필요했지만 이는 DI, 데코레이터 패턴, 다이내믹 프록시, 오브젝트 생성 후처리, 자동 프록시 생성, 포인트 컷과 같은 기법으로 이 문제를 해결할 수 있었다. 덕분에 부가기능은 모듈화 되어 중복되지 않고 변경이 필요할 대 한 곳만 수정하면 되게 되었다. 또 한 포인트 컷이라는 방법을 통해 부가기능을 부여할 대상을 선정함으로써 핵심기능을 담은 코드와 설정에는 전혀 영향을 주지 않아도 됐다.

**AOP : 애스팩트 지향 프로그래밍**

이런 부가기능 모듈, 그 자체로 애플리케이션의 핵심기능을 담고 있지는 않지만 애플리케이션을 구성하는 중요한 한 가지 요소이고 핵심기능에 부가되어 의미를 갖는 특별한 모듈을 애스팩트라고 부른다.

애스팩트는 부가될 기능을 정의한 어드바이스와 적용할 대상을 선정하는 포인트컷을 함께 갖고 있다.

이렇게 애플리케이션의 핵심적인 기능에서 부가적인 기능을 분리해서 애스팩트라는 독특한 모듈로 만들어서 설계하고 개발하는 방법을 애스팩트 지향 프로그래밍, 관점지향프로그래밍 또는 AOP 라고 부른다.

스프링은 독립적으로 개발한 부가기능 모듈을 다양한 타깃 오브젝트의 메소드에 다이내믹하게 적용해주기 위한 프록시를 이용하는 프록시 방식의 AOP 라고 할 수 있다.

이에 비해 AspectJ 는 프록시 처럼 간접적인 방법이 아니라 타깃 오브젝트를 뜯어 고쳐서 부가기능을 직접 넣어주는 직접적인 방법을 사용한다. 이는 스프링같은 DI 컨테이너의 도움을 받아서 자동 프록시 생성 방식을 사용하지 않아도 되며 훨씬 강력하고 유연한 AOP 가 가능하기 대문이다.

**AOP 네임스페이스**

aop 등록을 위해서 자동프록시생성기, 어드바이스, 포인트컷, 어드바이저 총 4개의 빈이 등록되어야 했다. 이를 편리하게 사용하기 위해 aop 스키마를 제공한다.

```
<aop:config>
   <aop:pointcut id="transactionPointcut" expression="execution(* *..*ServiceImpl.upgrade*(..))" />
   <aop:advisor advice-ref="transactionAdvice" pointcut-ref="transactionPointcut" //>
</aop:config>
```


## 6.6 트랜잭션 속성 ##

지금까지 작업해왔던 트랜잭션은 트랜잭션 매니저에게 트랜잭션을 가져오는 것과 commit(), rollback() 중의 하나를 호출하는 것으로 경계설정이 이루어져 있었다. 트랜잭션은 더 이상 쪼갤 수 없는 최소 단위의 작업이라는 개념은 유효하지만 모두 같은 방식으로 동작하지는 않는다. DefaultTransactionDefinition 이 구현하고 있는 TransactionDefinition 인터페이스는 트랜잭션의 동작방식에 영향을 줄 수 있는 네 가지 속성(트랜잭션 전파, 격리수준, 제한시간, 읽기전용)을 정의하고 있다.

**트랜잭션 전파**

이는 트랜잭션의 경계에서 이미 진행 중인 트랜잭션이 있을 때 또는 없을 때 어떻게 동작할 것인가를 결정하는 방식을 말한다.

PROPAGATION\_REQUIRED : 진행 중인 트랜잭션이 없으면 새로 시작하고 이미 있으면 이에 참여한다.DefaultTransactionDefinition 이 이 속성을 가진다.

PROPAGATION\_REQUIRES\_NEW : 항상 새로운 트랜잭션을 시작한다.

PROPAGATION\_NOT\_SUPPORTED : 트랜잭션 없이 동작하도록 만든다.

**격리수준**

적절하게 격리수준을 조정해서 가능한 한 많은 트랜잭션을 동시에 진행시키면서도 문제가 발생하지 않게 하는 제어가 필요하다. DefaultTransactionDefinition 에 설정된 격리수준은 ISOLATION\_DEFAULT 이다.

**제한시간**

트랜잭션을 수행하는 제한시간을 설정할 수 있다. DefaultTransactionDefinition 의 기본 설정은 제한시간이 없는 것이며 트랜잭션을 직접 시작할 수 있는 PROPAGATION\_REQUIRED 나 PROPAGATION\_REQUIRES\_NEW 와 함께 사용해야만 의미가 있다.

**읽기전용**

읽기전용으로 설정해두면 트랜잭션 내에서 데이터를 조작하는 시도를 막아줄 수 있다.

**트랜잭션 인터셉터와 트랜잭션 속성**

메소드별로 다른 트랜잭션 정의를 적용하려면 어드바이스의 기능을 확장해야 하는 TransactionAdvice 대신 TransactionInterceptor 를 사용해보도록 한다. 이는 PlatformTransactionManager 와 트랜잭션 속성인 Properties 타입의 두 가지 프로퍼티를 가지고 있으며 이에는 TransactionDefinition 의 기본 항목에 rollbackOn() 이라는 메소드를 하나 더 가지고 잇는 TransactionAttribute 인터페이스로 정의된다. rollbackOn() 메소드는 어떤 예외가 발생하면 롤백을 할 것인가를 결정하는 메소드이다.

스프링이 제공하는 TransactionInterceptor 에는 두 가지 종류의 예외처리 방식이 있다. 런타임 예외가 발생하면 트랜잭션은 롤백하며 체크 예외를 던지는 경우 이를 예외상황이라 보지 않고 의미가 있는 리턴 방식의 한 가지로 인식해서 트랜잭션을 커밋해버린다. 하지만 이런 기본원칙을 따르지 않을 경우에는 rollbackOn() 이라는 속성을 둬서 기본원칙과 다른 예외처리가 가능하게 해준다.

메소드 패턴에 따라 각기 다른 트랜잭션 속성을 부여할 수 있게 해주는 Properties 의 사용방법은 다음과 같다. 이 중 트랜잭션 전파 항목만 필수이고 나머지는 다 선택사항이다.

```
PROPAGATION_NAME, ISOLATION_NAME, readOnly, timeout_NNNN, -Exception1, -Exception2
```

```
<bean id="transactionAdvice" class="org.springframework.transaction.interceptor.TransactionInterceptor">
   <property name="transactionManager" ref="transactionManager" />
   <property name="transactionAttributes">
      <props>
         <prop key="get*">PROPAGATION_REQUIRED, readOnly, timeout_30</prop>
         <prop key="upgrade*">PROPAGATION_REQUIRES_NEW, ISOLATION_SERIALIZABLE</prop>
         <prop key="*>PROPAGATION_REQUIRED</prop>
      </props>
   </property>
</bean>
```

**tx 네임스페이스를 이용한 설정 방법**

```
<tx:advice id="transactionAdvice" transaction-manager="transactionManager">
   <tx:attributes>
      <tx:method name="get*" propagation="REQUIRED" read-only="true" timeout="30" />
      <tx:method name="upgrade*" propagation="REQUIRED_NEW" read-only="true" isolation="SERIALIZABLE" />
      <tx:method name="*" propagation="REQUIRED" />
   </tx:attributes>
</tx:advice>
```

**포인트컷과 트랜잭션 속성의 적용 전략**

트랜잭션 포인트컷 표현식은 타입 패턴이나 빈 이름을 이용한다.

공통된 메소드 이름 규칙을 통해 최소한의 트랜잭션 어드바이스와 속성을 정의한다.

프록시 방식 AOP 는 같은 타깃 오브젝트 내의 메소드를 호출할 때는 적용되지 않는다.

트랜잭션의 경계설정의 부가기능을 여러 계층에서 중구난방으로 적용하는 건 좋지 않다. 비즈니스 로직을 담고 있는 서비스 계층 오브젝트의 메소드가 트랜잭션 경계를 부여하기에 가장 적절한 대상이다.

```
public interface UserService {
   void add(User user);
   User get(String id);
   List<User> getAll();
   void deleteAll();
   void update(User user); 
   void upgradeLevels();
}
```

```
public class UserServiceImpl implementes UserService {
   UserDao userDao;
   ...
   public void deleteAll() { userDao.deleteAll(); }
   ...   
```

## 6.7 애노테이션 트랜잭션 속성과 포인트컷 ##

세밀한 트랜잭션 속성의 제어가 필요한 경우를 위해 스프링이 제공하는 다른 방법으로 애노테이션이 있다. 설정파일에서 패턴으로 분류 가능한 그룹을 만들어서 일괄적으로 속성을 부여하는 대신에 직접 타깃에 트랜잭션 속성정보를 가진 애노테이션을 지정하는 방법이다.

@Transactional 애노테이션을 트랜잭션 속성정보로 사용하도록 지정하면 스프링은 해당 애노테이션이 부여된 모든 오브젝트를 자동으로 타깃 오브젝트로 인ㄴ식한다. 이는 속성을 정의함과 동시에 포인트컷의 자동등록에도 사용된다.

**대체정책**

스프링은 @Transactional 을 적용할 때 4단계의 대체정책을 이용하게 해준다. 메소드의 속성을 확인할 때 타깃(클래스) 메소드, 타깃 클래스, 선언(인터페이스) 메소드, 선언 클래스 의 순서에 따라 @Transactional 이 적용됐는지 차례로 확인하고 가장 먼저 발견되는 속성정보를 사용하게 하는 방법이다.

@Transactional 을 이용한 트랜잭션 속성을 사용하는데 필요한 설정은 다음과 같다.

```
<tx:annotation-driven />
```

기존 설정파일에 선언된 트랜잭션 속성 정의는 다음과 같이 사용된다.

```
<tx:attributes>
   <tx:method name="get*" read-only="true" />
   <tx:method name="*" />
</tx:attributes>
```

```
@Transactional
public interface UserService {
   void add(User user);
   void deleteAll();
   void update(User user);
   void upgradeLevels();
 
   @Transactional
   User get(String id);
 
   @Transactional
   List<User> getAll();
}
```



## 6.8 트랜잭션 지원 테스트 ##




# 생각하기 #