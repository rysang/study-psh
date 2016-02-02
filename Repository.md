

# Repository 정리 #

## 1. Repository 정의 ##

Mediates between the domain and data mapping layers
using a collection-like interface for accessing domain objects.

Repository 란 도메인 객체에 접근하기 위해 컬렉션과 같은 인터페이스를 사용하는 , 데이터 매핑 레이어와 도메인 사이의 중재자 이다.

- PEAA -

## 2. 도메인 레이어 ##

여기에서 말하는 도메인은 우리가 흔히 사용했던 dto 와는 좀 다르다. 다음 소스와 같이 dto 가 테이블에 1:1로 매핑되고 상태정보만을 가지고 있는 것에 반해 여기에서 보는 도메인은 서비스 레이어 단에 있었던 해당 도메인이 종속된 로직들까지 포함하고 있다. (isVipCustomer 메소드)

```
class Customer {	
   Integer customerId;
   String name;
   …

   public Integer getCustomerId() {
      return customerId;
   }
   public void setCustomerId(Integer customerId) {
      this.customerId = customerId;
   }
  …
}
```

```
class CustomerService {
   CustomerDao customerDao;
   ...

   public boolean isVipCustomer
                              (Customer customer, PointRule pointRule) {
      return customer.customerLevel > 
                              pointRule.getVipLevel(customer.registered);
   }
   
   …
}
```

```
class Customer {	
   Integer customerId;
   String name;
   …

   public Integer getCustomerId() {
      return customerId;
   }
   public void setCustomerId(Integer customerId) {
      this.customerId = customerId;
   }
   …

   public boolean isVipCustomer(PointRule pointRule) {
      return this.customerLevel > pointRule.getVipLevel(this.registerd);
   }
}
```

## 3. 도메인 레이어에서의 Repository 사용 예 ##

기존에는 서비스 레이어에서 담당했던 데이터엑세스레이어와의 연동을 repository 를 통해 도메인레이어에서도 직접 할 수 있다. (이것은 구현의 문제로 꼭 도메인레이어에서만 사용하는 것은 아니다.)

```
class CustomerService {
   ...
   PointRuleDao pointRuleDao;
   
   public void addPoints() {
      ...
      PointRule pointRule = pointRuleDao.getCurrentPointRule();
      for(Customer customer : customers) {
         customer.doVipPointUpgrade(pointRule);
      }
   }
}
```

```
class Customer {
   PointRuleRepository pointRuleRepository;
   ...
   
   public void doVipPointUpgrade() {
      PointRule pointRule = pointRuleRepository.getCurrentPointRule();
      if(isVipCustomer(pointRule) {
         customer.setPoint(customer.getPoint() + VIP_POINTS);
      }
      ...
   }
}
```

## 4. DAO vs Repository ##

![http://pds11.egloos.com/pds/200811/23/18/f0081118_4928dae8d3c79.jpg](http://pds11.egloos.com/pds/200811/23/18/f0081118_4928dae8d3c79.jpg)

출처 : Domain-Driven Design의 적용-2.AGGREGATE와 REPOSITORY 2부
, 이터니티 블로그, http://aeternum.egloos.com/1165089

그림에서 보는 것 처럼 Order 와 OrderLineItem 을 하나의 단위로 보고 OrderLineItem 이 Order 에 종속된다면 이는 OrderRepository 를 사용해서 Order 와 OrderLineItem 컬렉션을 관리하게 된다. 이 때 Order 와 OrderLineItem 을 하나의 개념단위로 묶인 Aggregate 라고 본다.

| DAO | Repository |
|:----|:-----------|
| 테이블 단위 별로 매핑됨 | 개념단위 별로 매핑됨 |
| CRUD 쿼리와 1:1 매칭되는 세밀한 단위의 오퍼레이션을 제공 | 메모리에 로드된 객체 컬렉션에 대한 집합 처리를 위한 인터페이스를 제공 |
|     | 하나의 Repository 에서 다수의 DAO 를 호출할 수 있음 |
| 퍼시스턴스 레이어에 속함 | 도메인 레이어에 속함 |

DAO 에서 CRUD 쿼리랑 1:1 로 매핑되는 오퍼레이션을 제공한다면 Repository 자체는 퍼시스턴스 매커니즘에 대한 어떤 가정도 하지 않아서 다른 레이어에 대한 영향을 최소화 한다.

## 5. Swapping Repository Strategies ##

Repository 는 전략패턴을 이용해서 퍼시스턴스 레이어 접근방법에 대해서 쉽게 변경할 수도 있다.

```
abstract class Repository { 
   private RepositoryStrategy strategy;
   protected List matching(aCriteria) {
      return strategy.matching(aCriteria);
   }
}
```

```
public class RelationalStrategy implements            
                                            RepositoryStrategy { 
   protected List matching(Criteria criteria) {
      Query query = new Query(myDomainObjectClass())
      query.addCriteria(criteria);
      return query.execute(unitOfWork());
   }
}
```

```
public class InMemoryStrategy implements 
                                             RepositoryStrategy { 
   private Set domainObjects;
   protected List matching(Criteria criteria) {
      List results = new ArrayList();
      Iterator it = domainObjects.iterator();
      while (it.hasNext()) {
         DomainObject each = (DomainObject) it.next();
         if (criteria.isSatisfiedBy(each))
         results.add(each);
      }
       return results;
   }
}
```

# 출처 및 참고자료 #

**Patterns of Enterprise Application Architecture, Martin Fowler**


**Domain Driven Design, Eric Evans**

**스프링프레임워크와 DDD, 마소 (toby, 이일민)**

