

애니프레임에서는 특정 비즈니스 로직 수행 시 발생할 수 있는 예외에 스프링의 AOP 를 통해서 공통적인 로그 및 메시지 처리를 수행하도록 한다.

참고 : http://dev.anyframejava.org/anyframe/doc/core/3.2.0/corefw/guide/aop-example-exception.html

# applicationContext-aop-exception.xml #

after-throwing 으로 선언되어 특정 메소드가 수행 중 예외사항이 발생하는 경우 수행된다.

```
   <bean id="exceptionTransfer" class="integration.anyframe.services.aop.aspect.ExceptionTransfer" />

   <aop:config>
      <aop:aspect ref="exceptionTransfer">
         <!--// 적용대상 메소드 설정 -->
         <aop:pointcut id="getMethods" expression="execution(public * integration.anyframe.services..*Impl.get*(..))" />
          <!--// exception 매개변수의 예외 타입으로 예외 발생 시 transfer 메소드 호출 -->
         <aop:after-throwing method="transfer" throwing="exception" pointcut-ref="getMethods" />       
      </aop:aspect>
   </aop:config>
```

# `UserServiceImpl.java` #

해당 클래스의 getUser 메소드는 AOP 에서 정의한 pointcut 표현식에 부합하며 사용자 정보가 없을 경우 예외를 발생시킨다.

```
public UserVO getUser(String userId) throws Exception {
   UserVO userVO = userDAO.getUser(userId);

   if (userVO == null) {
      if (LOGGER.isDebugEnabled()) {
         LOGGER.debug(messageSource.getMessage("debug.user.get"
                     , new String[] { userId }, Locale.getDefault()));
      }
      throw new EmpException(messageSource.getMessage("debug.user.get"
                     , new String[] { userId }, Locale.getDefault()), null); // 예외 발생
   }

   return userVO;
}
```

# `ExceptionTransfer.java` #

AOP 에서 정의한 ExceptionTransfer 클래스

```
public class ExceptionTransfer implements ApplicationContextAware {
   private MessageSource messageSource;

   public void setApplicationContext(ApplicationContext applicationContext) {
      this.messageSource = (MessageSource) applicationContext.getBean("messageSource");
   }

   // pointcut 에 정의된 메소드에서 에러가 발생했을 때 transfer 메소드가 호출된다.
   // 발생한 Exception 은 EmpException, QueryServiceException, 기타로 구분되며 
   // JointPoint 로 타겟클래스와 메소드명을 조합한 message key 를 이용해서 해당 메시지를 얻어내고 
   // error 레벨의 로그를 남긴 후 EmpException 으로 전환하여 throw 한다.
   public void transfer(JoinPoint thisJoinPoint, Exception exception) throws EmpException {
      String pkgName = thisJoinPoint.getTarget().getClass().getName().toLowerCase();
      int lastIndex = pkgName.lastIndexOf(".");
      String className = pkgName.substring(lastIndex + 1);
      String opName = (thisJoinPoint.getSignature().getName()).toLowerCase();

      Log logger = LogFactory.getLog(thisJoinPoint.getTarget().getClass());

      if (exception instanceof EmpException) {
         EmpException empEx = (EmpException) exception;
         logger.error(empEx.getMessage(), empEx);
         throw empEx;
      }

      if (exception instanceof QueryServiceException) {
         logger.error(messageSource.getMessage("error." + className + "." + opName + ".query", new String[] {}, Locale.getDefault()), exception);
         throw new EmpException(messageSource.getMessage("error." + className + "." + opName + ".query"
                           , new String[] {}, Locale.getDefault()), exception);
      } else {
         logger.error(messageSource.getMessage("error." + className + "." + opName
                           , new String[] {}, Locale.getDefault()), exception);
         throw new EmpException(messageSource.getMessage("error." + className + "." + opName
                           , new String[] {}, Locale.getDefault()), exception);
      }
   }
}
```

# message-user.properties #

message key 와 message value 값으로 구성되어 있는 메시지 프로퍼티이다.

```
...
debug.user.get=User ID : {0} does not exist. // 메시지 정의
...
```

# `EmpException.java` #

애니프레임의 BaseException 을 상속받은 사용자 정의 Exception 이다. BaseException 은 Exception 을 상속 받고 있다.

```
import org.springframework.context.MessageSource;
import anyframe.common.exception.BaseException;

public class EmpException extends BaseException { 
   ...
   public EmpException(String message) {
      super(message);
   }
   ...
}
```

# 실행 #

UserServiceImpl 실행 중 예외가 발생하면 콘솔에서 message-user.properties 에서 정의된 대로 로그가 찍힌 것을 확인할 수 있다.

```
2008-12-29 21:42:04,390 ERROR [integration.anyframe.services.aop.service.UserServiceImpl] 
    User ID : test does not exist.
com.sds.emp.common.EmpException: User ID : test does not exist.
	at integration.anyframe.services.aop.service.UserServiceImpl.getUser(UserServiceImpl.java:39)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	// ...
Exception in thread "main" com.sds.emp.common.EmpException: User ID : test does not exist.
	at integration.anyframe.services.aop.service.UserServiceImpl.getUser(UserServiceImpl.java:39)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	// ...
```