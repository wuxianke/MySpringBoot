package com.wu.spring.mvc;

import com.wu.spring.constants.RequestMethod;
import org.apache.commons.collections4.map.HashedMap;

import java.util.Map;
import java.util.Objects;

public class Request {
    // 请求方法
    protected RequestMethod requestMethod;
    // 请求路径
    protected String requestPath;

    public Request(RequestMethod requestMethod, String requestPath){
        this.requestMethod = requestMethod;
        this.requestPath = requestPath;
    }

    /**
     * @param obj 查看是否是同一路径下的请求
     * @return 是否是同一个对象
     * 方法名和路径都一样就是同一对象
     */
    @Override
    public boolean equals(Object obj) {
        if(this == obj) {
            return true;
        }
        if(obj instanceof Request){
            Request e = (Request) obj;
            if(e.requestMethod.equals(this.requestMethod) && pathMatch(e.requestPath, this.requestPath)){
                return true;
            }
        }
        return false;
    }

    /**
     * @return
     * 返回一个哈希值数组
     */

    @Override
    public int hashCode() {
        return Objects.hash(requestMethod,requestPath);
    }

    @Override
    public String toString() {
        return requestMethod+":"+requestPath;
    }

    public static boolean pathMatch(String path1,String path2) {
        String[] pathSplit1 = path1.split("/");
        String[] pathSplit2 = path2.split("/");
        if(pathSplit1.length!=pathSplit2.length) {
            return false;
        }
        for(int i=0;i<pathSplit1.length;i++) {
            if(!pathSplit1[i].equals(pathSplit2[i])) {
                boolean b1 = (pathSplit1[i].startsWith("{")&&pathSplit1[i].endsWith("}"));
                boolean b2 = (pathSplit2[i].startsWith("{")&&pathSplit2[i].endsWith("}"));
                if( (b1==true && b2==false) || (b2==true && b1==false)) {
                    continue;
                }
                else {
                    return false;
                }
            }
        }
        return true;
    }


    /**
     * 将占位符去掉后，对参数类型与值进行匹配。
     * @param path1 实际请求路径
     * @param path2 方法中标注的请求路径。
     * @return map,key为{}中的值，value为实际请求路径中的值
     */
    public static Map<String, String> parsePathVariable(String path1, String path2) {
        Map<String, String> pathVariableMap=new HashedMap<>();
        String[] pathSplit1 = path1.split("/");
        String[] pathSplit2 = path2.split("/");
        if(pathSplit1.length!=pathSplit2.length) {
            throw new RuntimeException("path1:"+path1+" 与 path2:"+path2+"  长度不匹配");
        }
        for(int i=0;i<pathSplit1.length;i++) {
            if(!pathSplit1[i].equals(pathSplit2[i])) {
                if((pathSplit1[i].startsWith("{")&&pathSplit1[i].endsWith("}"))) {
                    String pathVariableName = pathSplit1[i].replace("{", "").replace("}", "").trim();
                    String pathVariableValue = pathSplit2[i].trim();
                    pathVariableMap.put(pathVariableName, pathVariableValue);
                }
                else {
                    throw new RuntimeException("path1:"+path1+" 与 path2:"+path2+"  内容不匹配");
                }
            }
        }
        return pathVariableMap;
    }

}
