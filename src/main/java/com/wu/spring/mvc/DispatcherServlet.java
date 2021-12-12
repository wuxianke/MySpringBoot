package com.wu.spring.mvc;

import com.wu.spring.annotation.mvc.PathVariable;
import com.wu.spring.annotation.mvc.RequestMapping;
import com.wu.spring.annotation.mvc.RequestParam;
import com.wu.spring.annotation.mvc.ResponseBody;
import com.wu.spring.constants.RequestMethod;
import com.wu.spring.ioc.ClassSetHelper;
import com.wu.spring.ioc.DefaultBeanFactory;
import com.wu.spring.utils.ConfigUtil;
import freemarker.template.Configuration;
import freemarker.template.TemplateExceptionHandler;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class DispatcherServlet extends HttpServlet {
    //bean工厂
    private static DefaultBeanFactory beanFactory = DefaultBeanFactory.getInstance();
    //请求到Handler的映射
    private Map<Request, Handler> handlerMapping = new HashMap<>();
    //Handler到Handler的映射
    private Map<Handler, HandlerAdapter> adapterMapping = new HashMap<>();
    //FreeMarker配置对象
    private Configuration cfg = null;

    @Override
    public void init(ServletConfig config) throws ServletException {
        try {
            // 初始化 这两个类。
            //Class.forName(AOPHelper.class.getName());
            //Class.forName(MapperHelper.class.getName());
            beanFactory.refresh();
        } catch (ClassNotFoundException e1) {
            // 捕捉这两个类不存在的异常
            e1.printStackTrace();
        } catch (Exception e) {
            // 捕捉bean容器刷新异常
            e.printStackTrace();
        }
        //请求解析
        //initMultipartResolver();
        ////多语言、国际化
        //initLocaleResolver();
        ////主题View层的
        //initThemeResolver();
        //=========== 重要 =========
        try {
            //解析url和Method的关联关系
            // 匹配请求以及handler.
            initHandlerMappings();
            System.out.println("initHandlerMappings...");
            //适配器（匹配的过程）
            initHandlerAdapters();
            System.out.println("initHandlerAdapters...");
            initFreemarkerResolver();
            System.out.println("InitFreemarkerResolver...");
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }


    /**
     * @throws Exception 解析url和Method的关联关系，也就是找到当前请求的url应该使用什么handler
     */
    private void initHandlerMappings() throws Exception {
        // 看bean工厂是否初始化完成。
        if (beanFactory.isEmpty()) {
            throw new Exception("ioc容器未初始化");
        }
        // 获得有Controller注解的类集合
        Set<Class<?>> classSet = ClassSetHelper.getControllerClassSet();
        for (Class<?> clazz : classSet) {
            String url = "";
            RequestMethod requestMethod = RequestMethod.GET;
            // 判断该类是否还被RequestMapping所标注
            if (clazz.isAnnotationPresent(RequestMapping.class)) {
                RequestMapping requestMapping = clazz.getAnnotation(RequestMapping.class);
                // 获得该注解规定url地址
                url = requestMapping.value();
                // 该注解规定的请求方法，默认为GET
                requestMethod = requestMapping.method();
            }
            //当找到了Controller类后，接下来就要找每个url对应的handler,即方法。
            Method[] methods = clazz.getMethods();
            //找到有RequestMapping注解的方法，然后放进handler集合中。
            for (Method method : methods) {
                // 不是则跳过
                if (!method.isAnnotationPresent(RequestMapping.class)) {
                    continue;
                }
                RequestMapping requestMapping = method.getAnnotation(RequestMapping.class);
                // 完整的url地址为类的url与该方法的url拼接
                String url2 = (url + requestMapping.value()).replaceAll("/+", "/");
                Request req = new Request(requestMethod, url2);
                // handler实际上是个处理器，记录着对应的类对象以及处理方法
                Handler handler = new Handler(beanFactory.getBean(clazz), method);
                // 将handler和请求对应保存至map中
                // req 作为key，handler作为value.
                handlerMapping.put(req, handler);
                System.out.println("Mapping: " + url2 + " to  :" + method.toString());
            }
        }
    }

    private void initHandlerAdapters() throws Exception {
        if (handlerMapping.isEmpty()) {
            throw new Exception("handlerMapping 未初始化");
        }
        // 参数类型作为key，参数的索引号作为值进行对应。
        Map<String, Integer> paramMapping = null;
        for (Map.Entry<Request, Handler> entry : handlerMapping.entrySet()) {
            // 为每个handler 都进行参数的匹配
            paramMapping = new HashMap<String, Integer>();
            Request req = entry.getKey();
            Handler handler = entry.getValue();
            Class<?>[] parameterTypes = handler.method.getParameterTypes();
            //有顺序，但是通过反射，没法拿到我们参数名字
            //因为每个参数上面是可以加多个注解的，所以是二维数组,第一位表示参数位置，第二位表示注解个数
            Annotation[][] pa = handler.method.getParameterAnnotations();
            //匹配自定参数列表
            // 表示参数的位置，用索引i表示
            for (int i = 0; i < pa.length; i++) {
                // 该参数位置的参数类型。
                Class<?> type = parameterTypes[i];
                if (type == HttpServletRequest.class || type == HttpServletResponse.class) {
                    paramMapping.put(type.getName(), i);
                    continue;
                }
                // 获得该参数位置的所有注解。注解的值进行一一匹配。
                for (Annotation annotation : pa[i]) {
                    String paramName;
                    if (annotation instanceof RequestParam) {
                        paramName = ((RequestParam) annotation).value();
                        // 判断参数是否为空。不为空则将参数与位置对应存储
                        if (!"".equals(paramName.trim())) {
                            paramMapping.put(paramName, i);
                        } else if (annotation instanceof PathVariable) {
                            paramName = ((PathVariable) annotation).value();
                            if (!"".equals(paramName.trim())) {
                                paramMapping.put(paramName, i);
                            }
                        } else if (annotation instanceof ResponseBody) {
                            paramMapping.put("ResponseBody", i);
                        }
                    }
                }
            }
            adapterMapping.put(handler, new HandlerAdapter(paramMapping));
        }
    }

    /**
     * Freemarker的初始化配置
     *
     * @throws Exception
     */
    private void initFreemarkerResolver() throws Exception {
        cfg = new Configuration(Configuration.VERSION_2_3_22);
        cfg.setDirectoryForTemplateLoading(new File(this.getClass().getResource("/").getPath() + ConfigUtil.getAppJspPath()));
        cfg.setDefaultEncoding("UTF-8");
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
    }

    /**
     * 处理GET请求
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req, resp);
    }

    /**
     * 处理POST请求,在这里调用自己写的Controller的方法
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        System.out.println("request method is " + req.getMethod() + " url is " + req.getRequestURI());
        try {
            doDispatch(req, resp);
        } catch (Exception e) {
            resp.getWriter().write("500 Exception, Msg :" + Arrays.toString(e.getStackTrace()));
        }
    }


    /**
     * @param request 请求
     * @return 返回自定义的请求结构，"请求方法：url"
     */
    private Request createRequest(HttpServletRequest request) {
        String url = request.getRequestURI();
        String contextPath = request.getContextPath();
        url = url.replace(contextPath, "").replaceAll("/+", "/");
        return new Request(Enum.valueOf(RequestMethod.class, request.getMethod().toUpperCase()), url);
    }

    /**
     * 首先根据请求找到handler
     *
     * @param request 请求
     * @return 返回对应的handler
     */
    private Handler getHandler(HttpServletRequest request) {
        if (handlerMapping.isEmpty()) {
            System.out.println("handlerMapping is empty");
            return null;
        }
        // 获得自定义的request结构
        Request req = createRequest(request);
        System.out.println("getHandler... url is" + req.requestPath);
        // 找到与请求方法匹配的handler
        for (Request request1 : handlerMapping.keySet()) {
            if (request1.equals(req)) {
                return handlerMapping.get(request1);
            }
        }
        System.out.println("Can not find handler");
        return null;
    }

    /**
     * 找到handler后应该进行请求参数与方法参数的对应。
     *
     * @param handler 处理器
     * @return 参数匹配Map
     */
    private HandlerAdapter getHandlerAdapter(Handler handler) {
        if (handler == null || adapterMapping == null || adapterMapping.isEmpty()) {
            System.out.println("adapter is wrong");
            return null;
        }
        return adapterMapping.get(handler);
    }

    /**
     * @param request 请求
     * @return 由PathVariable 参数与值对应的Map
     */
    private Map<String, String> getPathVariableMap(HttpServletRequest request) {
        Request req = createRequest(request);
        for (Request request1 : handlerMapping.keySet()) {
            // 要注意，这里的equals 是重写了的。
            if (request1.equals(req)) {
                return Request.parsePathVariable(request1.requestPath, req.requestPath);
            }
        }
        return null;
    }

    /**
     * 处理客户端传来的请求，根据请求以及handler的类型，做出响应。
     * @param request 请求
     * @param response 响应
     * @throws Exception
     */
    private void doDispatch(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Handler handler = getHandler(request);
        if (handler == null) {
            response.getWriter().write("404 Handler Not Found");
        }
        HandlerAdapter handlerAdapter = getHandlerAdapter(handler);
        // 执行HandlerAdapter, 解析参数
        if (handlerAdapter != null) {
            Map<String, String> pathVariableMap = getPathVariableMap(request);
            Object data = handlerAdapter.handle(request, response, handler, pathVariableMap);
            if (isResponseBody(handler)) {
                ResultResolverHandler.handleJsonResult(data, response);
            } else if (data instanceof ModelAndView) {
                ResultResolverHandler.handlerFreemarkerResult(data, cfg, request, response);
            } else {
                ResultResolverHandler.handleStringResult(data, response);
            }
        } else {
            response.getWriter().write("404 HandlerAdapter Not Found");
        }
    }

    /**
     * handler中的方法是否被ResponseBody注解
     *
     * @param handler 请求
     * @return boolean
     */
    private boolean isResponseBody(Handler handler) {
        return handler.method.isAnnotationPresent(ResponseBody.class) || handler.controller.getClass().isAnnotationPresent(ResponseBody.class);
    }

    /**
     * 请求解析
     *
     *
     */
    private void initMultipartResolver() {
    }

    /**
     * 多语言、国际化
     *
     *
     */
    private void initLocaleResolver() {
    }

    /**
     * 主题View层的
     *
     *
     */
    private void initThemeResolver() {
    }

}
