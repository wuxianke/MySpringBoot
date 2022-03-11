package com.wu.spring.utils;

import java.io.File;
import java.io.FileFilter;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * @author Cactus
 * 类加载的相关工具类
 */
public class ClassUtil {
    /**
     * 获取类加载器
     */
    public static ClassLoader getClassLoader() {
        ClassLoader cl = null;
        try {
            // 获取当前线程的类加载器
            cl = Thread.currentThread().getContextClassLoader();
        } catch (Throwable ex) {
            // Cannot access thread context ClassLoader - falling back to system class loader...
        }
        if (cl == null) {
            // No thread context class loader -> use class loader of this class.
            cl = ClassUtil.class.getClassLoader();
        }
        return cl;
    }


    /**
     * @param className     类名
     * @param isInitialized 是否对该类进行初始化
     * @return 获得类对象
     */
    public static Class<?> loadClass(String className, boolean isInitialized) {
        Class<?> cls;
        try {
            cls = Class.forName(className, isInitialized, getClassLoader());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        return cls;
    }

    /**
     * 加载类（默认将初始化类），实际上默认的Class.forName方法会对类进行初始化。
     */
    public static Class<?> loadClass(String className) {
        return loadClass(className, true);
    }

    /**
     * 获取指定包名下的所有类，在配置文件中会配置
     */
    public static Set<Class<?>> getClassSet(String packageName) {
        Set<Class<?>> classSet = new HashSet<Class<?>>();
        try {
            Enumeration<URL> urls = getClassLoader().getResources(packageName.replace(".", "/"));
            while (urls.hasMoreElements()) {
                URL url = urls.nextElement();
                if (url != null) {
                    String protocol = url.getProtocol();
                    if (protocol.equals("file")) {
                        String packagePath = url.getPath().replaceAll("%20", " ");
                        addClass(classSet, packagePath, packageName);
                    } else if (protocol.equals("jar")) {
                        JarURLConnection jarURLConnection = (JarURLConnection) url.openConnection();
                        if (jarURLConnection != null) {
                            JarFile jarFile = jarURLConnection.getJarFile();
                            if (jarFile != null) {
                                Enumeration<JarEntry> jarEntries = jarFile.entries();
                                while (jarEntries.hasMoreElements()) {
                                    JarEntry jarEntry = jarEntries.nextElement();
                                    String jarEntryName = jarEntry.getName();
                                    if (jarEntryName.endsWith(".class")) {
                                        String className = jarEntryName.substring(0, jarEntryName.lastIndexOf(".")).replaceAll("/", ".");
                                        doAddClass(classSet, className);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return classSet;
    }
    /**
     * 将指定路径下的类对象添加到集合classSet中
     *
     * @param classSet    集合
     * @param packagePath 包的路径
     * @param packageName 包的名字
     */
    private static void addClass(Set<Class<?>> classSet, String packagePath, String packageName) {
        File[] files = new File(packagePath).listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return (file.isFile() && file.getName().endsWith(".class")) || file.isDirectory();
            }
        });
        for (File file : files) {
            String fileName = file.getName();
            // 当前file是文件时
            if (file.isFile()) {
                // 将文件名的后缀去掉
                String className = fileName.substring(0, fileName.lastIndexOf("."));
                if (packageName != null && packageName != "") {
                    className = packageName + "." + className;
                }
                doAddClass(classSet, className);
            } else {  // 如果不是文件，说明下面还有包，继续向下寻找类文件
                String subPackagePath = fileName;
                if (packagePath != null && packagePath != "") {
                    subPackagePath = packagePath + "/" + subPackagePath;
                }
                String subPackageName = fileName;
                if (packageName != null && packageName != "") {
                    subPackageName = packageName + "." + subPackageName;
                }
                addClass(classSet, subPackagePath, subPackageName);
            }
        }
    }

    /**
     * 加载并添加类对象
     * @param classSet 类对象集合
     * @param className  类名
     */
    private static void doAddClass(Set<Class<?>> classSet, String className) {
        // 只是想获得类对象，不想要初始化
        Class<?> cls = loadClass(className, false);
        classSet.add(cls);
    }


}
