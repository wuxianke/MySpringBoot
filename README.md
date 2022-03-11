

## 项目描述：

出于学习目的，手写的简易版仿Springboot框架。实现了IOC，AOP，MVC，Mybatis的关键功能，此外还能通过注解实现事务管理，可进行简单的增删改查和实现动态页面。可通过@Bean连接外置的Redis客户端，用Redis实现用户登录缓存功能。相关注解功能基本与Springboot相同。项目结构清晰，有详细的代码注释。

## 功能实现：

1. 实现了IOC容器和依赖注入，用单例模式实现Bean工厂类，采用三级缓存解决循环依赖问题，并实现了多个注解的功能，如 @Autowired，@Qualifier，@Resource，@Configuration和@Bean等注解
2. 通过CGLib动态代理实现了AOP（面向切面编程），通过注解@Before，@After，@Around，@Afterthrowing，@AfterReturning来实现多种通知方式，对于多个同类增强方法，可以按照等级先后执行。还通过动态代理还实现了Spring的事务管理，在类或者方法上加上@Transactional即可获得事务管理功能，可配置事务的四种隔离级别、回滚异常类和六种传播行为。 
3. MVC模块实现了DispatcherServlet解析和处理请求的主要流程。handerAdapter对请求处理完成 之后根据实际返回的类型来进行解析，并返回给用户；如果有@ResponseBody注解，则解析为 Json字符串返回。如果返回一个ModelAndView对象，则使用内置的Freemarker解析器进行解析，解析渲染之后将html页面返回给用户。 
4. 实现了类似于Mybatis的功能，通过在@Mapper声明的接口上使用注解@Select，@Update， @Insert，@Delete方法上声明要执行的sql语句，然后可以通过动态代理来生成实际的代理对象， 调用Executor来执行实际的Sql语句，生成的代理对象通过IOC容器也可以自动注入到其他对象的属性中。 

## 模块的说明： 

com.wu.spring包下实现了Spring的IOC,AOP,MVC的功能模块；    

com.wu.mybatis包下实现了仿Mybatis的功能模块，可通过注解进行数据库增删改查，完成对象关系映射；    

com.wu.demo包下是一个通过mysql和redis实现的注册登录demo。  

## 项目运行方法：

1.本项目使用的是MySQL8.0，修改resources下的application.properties配置文件，修改数据库的url，账号，密码的配置，同时还需要修改Redis数据库的账号密码及端口（不修改也可，使用的是我的服务器的redis），即修改com.wu.demo.configuration包下的Jedis Bean的手动配置。  

2.在数据库中生成表，字段名和类型见User类，字段名一定要相等。

  ![image-20220310202316227](https://picture-1252827130.cos.ap-shanghai.myqcloud.com/picture/image-20220310202316227.png)

![image-20220310202328532](https://picture-1252827130.cos.ap-shanghai.myqcloud.com/picture/image-20220310202328532.png)

3.启动主类Application.java，使用postman进行接口测试。  

![image-20220310202354292](https://picture-1252827130.cos.ap-shanghai.myqcloud.com/picture/image-20220310202354292.png)



## 相关功能实现示例：

### 循环依赖问题

在Service层分别定义了两个类，它们内部会相互依赖，如图：

![image-20220310195138604](https://picture-1252827130.cos.ap-shanghai.myqcloud.com/picture/image-20220310195138604.png)

通过三级缓存，解决依赖循环，测试结果：

![image-20220310195410635](https://picture-1252827130.cos.ap-shanghai.myqcloud.com/picture/image-20220310195410635.png)

### 实现多种通知方式：

定义pointCut切点，在同一个切点实现了多个通知方法，并对同一类型按照order进行排序

![image-20220310200114374](https://picture-1252827130.cos.ap-shanghai.myqcloud.com/picture/image-20220310200114374.png)

### 可根据不同请求，返回字符串或一个简单的页面：

字符串：

![image-20220310201403877](https://picture-1252827130.cos.ap-shanghai.myqcloud.com/picture/image-20220310201403877.png)

简单的页面视图：

![image-20220310201431590](https://picture-1252827130.cos.ap-shanghai.myqcloud.com/picture/image-20220310201431590.png)