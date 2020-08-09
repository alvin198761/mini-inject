package org.alvin.mini_inject;

import org.alvin.mini_inject.annotations.*;
import org.alvin.mini_inject.plugins.MiniPlugin;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

public class InjectContext {

    private static Map<Class<?>, Object> instanceMap = new ConcurrentHashMap<>();

    private static List<Class> clazzList = new ArrayList<>();

    private static Properties env = new Properties();

    private static String[] args;

    /**
     * 插件安装
     *
     * @param plugin
     */
    public static void install(MiniPlugin plugin) {
        instanceMap.putAll(plugin.doRun(clazzList));
    }

    /**
     * 业务实例对象创建和获取
     *
     * @param cl
     * @return
     */
    private static <T> T createInstance(Class<? extends T> cl) {
        if (!instanceMap.containsKey(cl)) {
            try {
                instanceMap.put(cl, cl.newInstance());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return (T) instanceMap.get(cl);
    }

    /**
     * 扫描业务类
     *
     * @param packageURL
     * @throws Exception
     */
    public static void doScanService(String packageURL) {
        try {
            String packageName = packageURL;
            packageURL = packageURL.replaceAll("[.]", "/");
            URL baseURL = Thread.currentThread().getContextClassLoader().getResource(packageURL);
            if ("file".equals(baseURL.getProtocol())) {
                clazzList.addAll(doDevScan(baseURL, packageName));
            } else if ("jar".equals(baseURL.getProtocol())) {
                clazzList.addAll(doRuntimeScan(baseURL, packageName));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            initComponent(clazzList);
            injectComponent(clazzList);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void injectComponent(List<Class> list) {
        list.forEach(item -> {
            AccessController.doPrivileged(new PrivilegedAction<Object>() {

                @Override
                public Object run() {
                    doSetValue(item);
                    doInject(item);
                    return null;
                }

            });
        });
    }

    /**
     * 配置注入
     *
     * @param clazz
     */
    private static void doSetValue(Class clazz) {
        Object instance = instanceMap.get(clazz);
        if (instance == null) {
            return;
        }
        Field[] fields = clazz.getDeclaredFields();
        if (fields == null || fields.length == 0) {
            return;
        }
        for (Field f : fields) {
            MiniValue miniValue = f.getAnnotation(MiniValue.class);
            if (miniValue == null) {
                continue;
            }
            boolean access = f.isAccessible();
            try {
                f.setAccessible(true);
                String key = miniValue.value();
                f.set(instance, env.getOrDefault(key, key));
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } finally {
                f.setAccessible(access);
            }

        }
    }

    /**
     * 对象注入
     *
     * @param clazz
     */
    private static void doInject(Class clazz) {
        Object instance = instanceMap.get(clazz);
        if (instance == null) {
            return;
        }
        Field[] fields = clazz.getDeclaredFields();
        if (fields == null || fields.length == 0) {
            return;
        }
        for (Field f : fields) {
            MiniInject miniInject = f.getAnnotation(MiniInject.class);
            if (miniInject == null) {
                continue;
            }

            boolean access = f.isAccessible();
            try {
                f.setAccessible(true);
                f.set(instance, createInstance(f.getType()));
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } finally {
                f.setAccessible(access);
            }

        }
    }

    /**
     * 将扫描的类进行过滤，不是该注解的一律放弃掉
     *
     * @param list
     * @throws Exception
     */
    private static void initComponent(List<Class> list) throws Exception {
        list.stream().filter(c -> c.getDeclaredAnnotation(MiniComponent.class) != null).forEach(InjectContext::createInstance);
    }

    /**
     * 运行时只会通过这个方法调用
     *
     * @param baseURL
     * @param packageName
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private static List<Class> doRuntimeScan(URL baseURL, String packageName)
            throws IOException, ClassNotFoundException {
        List<Class> classList = new ArrayList<Class>();
        //
        JarFile jar = ((JarURLConnection) baseURL.openConnection()).getJarFile();
        Enumeration<JarEntry> entries = jar.entries();
        while (entries.hasMoreElements()) {
            JarEntry je = entries.nextElement();
            String urlName = je.getName().replace('/', '.');
            if (!urlName.startsWith(packageName)) {
                continue;
            }
            if (!urlName.endsWith(".class")) {
                continue;
            }
            String classUrl = urlName.substring(0, urlName.length() - 6);
            classList.add(Class.forName(classUrl));
        }
        return classList;
    }

    /**
     * 开发时会用到的方法
     *
     * @param baseURL
     * @param packageName
     * @throws Exception
     */
    private static List<Class> doDevScan(URL baseURL, String packageName) throws Exception {
        String filePath = URLDecoder.decode(baseURL.getFile(), "UTF-8");
        List<Class> classList = new ArrayList<Class>();

        LinkedList<File> files = new LinkedList<File>();
        File dir = new File(filePath);
        if (!dir.exists() || !dir.isDirectory()) {
            throw new Exception("没有找到对应的包");
        }
        files.add(dir);
        // 循环读取目录以及子目录下的所有类文件
        while (!files.isEmpty()) {
            File f = files.removeFirst();
            if (f.isDirectory()) {
                File[] fs = f.listFiles();
                int i = 0;
                for (File childFile : fs) {
                    files.add(i++, childFile);
                }
                continue;
            }
            String subPath = f.getAbsolutePath().substring(dir.getAbsolutePath().length());
            String classUrl = packageName + subPath.replace(File.separatorChar, '.');
            classUrl = classUrl.substring(0, classUrl.length() - 6);
            classList.add(Class.forName(classUrl));
        }
        return classList;
    }

    public static <T> T getInstance(Class<? extends T> cls) {
        return (T) instanceMap.get(cls);
    }

    /**
     * 启动项目
     *
     * @param mainClass
     * @param args
     */
    public static void run(Class<?> mainClass, String[] args) {
        InjectContext.args = args;
        InjectContext.initConfig();
        //加载插件 Todo
        MiniServiceScan miniServiceScan = mainClass.getAnnotation(MiniServiceScan.class);
        doScanService(miniServiceScan.value());
        doRunComponent();
    }

    /**
     * 初始化配置
     */
    private static void initConfig() {
        try {
            InjectContext.env.load(InjectContext.class.getResourceAsStream("/application.properties"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        Path path = Paths.get("config", "application.properties");
        if (Files.exists(path)) {
            try {
                InjectContext.env.load(Files.newInputStream(path));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void doRunComponent() {
        AccessController.doPrivileged(new PrivilegedAction<Object>() {
            @Override
            public Object run() {
                instanceMap.forEach((k, v) -> {
                    Method[] ms = k.getDeclaredMethods();
                    for (Method m : ms) {
                        MiniRun miniRun = m.getAnnotation(MiniRun.class);
                        if (miniRun == null) {
                            continue;
                        }
                        boolean access = m.isAccessible();
                        try {
                            m.setAccessible(true);
                            m.invoke(v);
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        } catch (InvocationTargetException e) {
                            e.printStackTrace();
                        } finally {
                            m.setAccessible(access);
                        }
                    }
                });
                return null;
            }
        });
    }

    /**
     * 根据注解获取类
     *
     * @param annotationClass
     * @return
     */
    public static List<Class> getClazzListByAnnotation(Class annotationClass) {
        return AccessController.doPrivileged(new PrivilegedAction<List<Class>>() {
            public List<Class> run() {
                return clazzList.parallelStream()
                        .filter(item -> item.getAnnotation(annotationClass) != null)
                        .collect(Collectors.toList());
            }
        });
    }

    /**
     * 获取子类
     *
     * @param clazz
     * @param <T>
     * @return
     */
    public static <T> List<Class<T>> getSubClasses(final Class<T> clazz) {
        return AccessController.doPrivileged(new PrivilegedAction<List<Class<T>>>() {
            public List<Class<T>> run() {
                return clazzList.parallelStream().filter(c ->
                        clazz.isAssignableFrom(c) && !Modifier.isAbstract(c.getModifiers())
                ).map(c -> (Class<T>) c).collect(Collectors.toList());
            }
        });
    }

    /**
     * 查找方法带有这个注解的类
     *
     * @param annotation
     * @return
     */
    public static List<Class> getByMethodAnnocation(final Annotation annotation) {
        return AccessController.doPrivileged(new PrivilegedAction<List<Class>>() {
            public List<Class> run() {
                return clazzList.parallelStream().filter(item -> {
                    Method[] ms = item.getDeclaredMethods();
                    for (Method m : ms) {
                        if (m.getDeclaredAnnotation(Annotation.class) != null) {
                            return true;
                        }
                    }
                    return false;
                }).collect(Collectors.toList());
            }
        });
    }

    public static Properties getEnv() {
        return env;
    }
}
