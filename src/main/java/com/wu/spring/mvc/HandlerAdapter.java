package com.wu.spring.mvc;

import com.alibaba.fastjson.JSON;
import com.wu.spring.annotation.mvc.PathVariable;
import com.wu.spring.annotation.mvc.RequestBody;
import com.wu.spring.annotation.mvc.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

public class HandlerAdapter {
    private Map<String, Integer> paramMapping;

    public HandlerAdapter(Map<String, Integer> paramMapping) {
        this.paramMapping = paramMapping;
    }

    /**
     * 首先获得方法的参数，然后通过反射调用对应的method.
     *
     * @param request         请求
     * @param response        响应
     * @param handler         处理器
     * @param pathVariableMap
     * @return
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     */
    public Object handle(HttpServletRequest request, HttpServletResponse response, Handler handler, Map<String, String> pathVariableMap) throws InvocationTargetException, IllegalAccessException {
        if (handler.method.getParameterCount() == 0) {
            return handler.method.invoke(handler.controller);
        }
        //先是获取参数的类型，这个是用来将String类型转换成所需要。
        Class<?>[] parameterTypes = handler.method.getParameterTypes();
        // 获取参数
        Parameter[] parameters = handler.method.getParameters();
        // 根据参数类型的大小创建参数值数组
        Object[] paramValues = new Object[parameterTypes.length];
        String requestName = HttpServletRequest.class.getName();
        if (this.paramMapping.containsKey(requestName)) {
            Integer requestIndex = this.paramMapping.get(requestName);
            paramValues[requestIndex] = request;
        }
        String responseName = HttpServletResponse.class.getName();
        if (this.paramMapping.containsKey(responseName)) {
            Integer responseIndex = this.paramMapping.get(responseName);
            paramValues[responseIndex] = response;
        }
        //注入消息体的内容，这种情况一般是Post请求时。
        String bodyContent=null;
        try {
            bodyContent = getBodyContent(request);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        if(request.getMethod().equals("POST") && bodyContent!=null) {
            Integer index=-1;
            for(Integer i=0;i<parameters.length;i++) {
                if(parameters[i].isAnnotationPresent(RequestBody.class)) {
                    index=i;
                    break;
                }
            }
            if(index>=0) {
                // 通过解析body，将body中的参数放入对应位置。
                Object body = JSON.parseObject(bodyContent, parameterTypes[index]);
                paramValues[index]=body;
            }
        }
        //解析RequestParam和pathVariable中的参数
        // 首先获得请求中的key与value.
        Map<String, String[]> parameterMap = request.getParameterMap();
        for (Map.Entry<String, Integer> entry : paramMapping.entrySet()) {
            Integer index = entry.getValue();
            if (paramValues[index] != null) {
                continue;
            }
            if (!parameterMap.containsKey(entry.getKey())) {
                if (parameters[index].isAnnotationPresent(PathVariable.class)) {
                    if (pathVariableMap != null && pathVariableMap.containsKey(entry.getKey())) {
                        paramValues[index] = caseStringValue(pathVariableMap.get(entry.getKey()), parameterTypes[index]);
                    } else {
                        paramValues[index] = caseStringValue(parameters[index].getAnnotation(PathVariable.class).defaultValue(), parameterTypes[index]);
                    }
                } else {
                    String value = parameters[index].isAnnotationPresent(RequestParam.class) ? parameters[index].getAnnotation(RequestParam.class).defaultValue() : "";
                    paramValues[index] = value;
                }
            } else {
                String value = Arrays.toString(parameterMap.get(entry.getKey())).replaceAll("\\[|\\]", "")
                        .replaceAll(",\\s", ",");
                paramValues[index] = caseStringValue(value, parameterTypes[index]);
            }
        }
        return handler.method.invoke(handler.controller, paramValues);
    }

    /**
     * 转换参数类型, 由于请求得到的是String类型，需要转化为method要求的类型。
     * @param value 请求得到的参数值
     * @param parameterType 方法要求的参数类型
     * @return 参数值的类型进行转换。
     */
    private Object caseStringValue(String value, Class<?> parameterType) {
        if(parameterType == String.class){
            return value;
        } else if (parameterType == Integer.class) {
            return Integer.valueOf(value);
        }else if (parameterType == int.class){
            return Integer.valueOf(value);
        }else{
            return null;
        }
    }

    /**
     * 获取HttpServletRequest的body中的内容，以字符串的形式返回
     * @param request 请求
     * @return body的内容，以字符串形式。
     */
    private String getBodyContent(HttpServletRequest request) throws IOException {
        // 如果该请求是POST请求，那么将请求体的内容转成字符串
        if (request.getMethod().equals("POST")){
            StringBuffer stringBuffer = new StringBuffer();
            BufferedReader bufferedReader = null;
            try {
                bufferedReader = request.getReader();
                char[] charBuffer = new char[128];
                int bytesRead;
                while ( (bytesRead = bufferedReader.read(charBuffer)) != -1){
                    stringBuffer.append(charBuffer, 0, bytesRead);
                }
            } catch (IOException e){
                throw e;
            } finally {
                if (bufferedReader != null){
                    try {
                        bufferedReader.close();
                    }catch (IOException e){
                        throw e;
                    }
                }
            }
            return stringBuffer.toString();
        }
        return null;
    }

}
