package com.wu.spring.tomcat;

import com.wu.spring.ioc.DefaultBeanFactory;
import com.wu.spring.mvc.DispatcherServlet;
import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;

/**
 * 内置Tomcat服务器的配置
 * @author Cactus
 *
 */
public class TomcatServer {
    private Tomcat tomcat;
    private String[] args;

    public TomcatServer(String[] args) {
        this.args = args;
    }
    
    public void startServer() throws LifecycleException{
        
    	Tomcat tomcat = new Tomcat();
    	DefaultBeanFactory beanFactory = DefaultBeanFactory.getInstance();
    	if(!beanFactory.isEmpty()) {
    		System.out.println("beanFactory初始化成功");
    	}
        //设置绑定的ip及端口号
        tomcat.setHostname("localhost");
        tomcat.setPort(8080);
        final Context context = tomcat.addContext("/", null);
        Tomcat.addServlet(context, "dispatch", new DispatcherServlet());
        context.addServletMapping("/", "dispatch");
        try {
            tomcat.init();
            tomcat.start();
            tomcat.getServer().await();
        } catch (LifecycleException e) {
            e.printStackTrace();
        }
    }
}
