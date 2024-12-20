package study.rpc.extension;

import lombok.extern.slf4j.Slf4j;
import study.rpc.utils.StringUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * SPI加载器，负责动态加载实现类的核心逻辑
 * 一个SPI加载器需要实现的功能：
 * 1. 接口加载：根据扩展点接口加载其所有实现类
 * 2. 实现类实例化：根据扩展名获取对应实现类的实例
 * 3. 配置文件管理：按规定规则读取配置文件，解析扩展名称与实现类的映射
 * 4. 缓存管理：扩展类和实例都被缓存，避免重复加载和创建。作用：延迟加载+线程安全
 * 5. 多线程管理：在并发场景下安全加载和访问扩展点实例
 */
@Slf4j
public final class ExtensionLoader <T> {

    //扩展实现类配置文件存放的根目录
    private static final String SERVICE_DIRECTORY = "META-INF/extensions/";
    // 当前加载的扩展类型（即接口类型）
    private final Class<?> type;

    // 缓存 ExtensionLoader 实例，用于存储不同类型的 ExtensionLoader
    // 每个接口类型对应一个 ExtensionLoader 实例
    private static final Map<Class<?>, ExtensionLoader<?>> EXTENSION_LOADERS = new ConcurrentHashMap<>();

    //缓存所有扩展类的映射，按扩展名存储对应的类
    //用Holder封装的意义：
    //1、线程安全，可以保证只有一个线程在对Map操作
    //2、延迟加载，Map不会在系统启动时立即加载，而是等到第一次访问时才加载。
    private final Holder<Map<String, Class<?>>> cachedClasses = new Holder<>();

    //缓存每个扩展点名称对应的实例，保证按照名称获取实例的单例性
    //为什么不直接使用T，而是使用Holder<Object>
    //直接使用 Map<String, T>，实例需要立即创建或在首次访问时初始化。
    //使用 Holder 时，实例的创建是延迟的，即只有在需要时才真正创建实例。
    //支持延迟加载：通过 Holder，你可以先占位（存储一个空的 Holder 对象），然后在需要时才真正创建实例。
    //例如，当调用 holder.get() 时，可以检查是否已经创建实例，如果没有，则通过 createExtension 创建。
    //线程安全：在并发环境中，多个线程可能同时访问 cachedInstances。
    //使用 Holder 可以通过 synchronized 块保护实例的创建过程，确保实例的唯一性。
    //引用封装：Holder 是一个指针式的引用封装，可以随时更新内部的值，而不需要改变 Map 中的键值对。
    private final Map<String, Holder<Object>> cachedInstances = new ConcurrentHashMap<>();
    // 缓存扩展类实例，确保每个扩展类只被实例化一次
    private static final Map<Class<?>, Object> EXTENSION_INSTANCES = new ConcurrentHashMap<>();

    // 构造函数，指定扩展类型（接口）
    //type表示的是？(任意类型)的运行时表示（反射机制），取名为type，≠ 一个描述类型语言变量
    private ExtensionLoader(Class<?> type) {
        this.type = type;
    }

    /**
     * 获取扩展点的加载器
     */
    //Class<S>表示类型S的运行时表示
    public static <S> ExtensionLoader<S> getExtensionLoader(Class<S> type) {

        //检查传入的扩展点类型，确保是带有SPI注解的可扩展的接口类
        if (type == null) {
            throw new IllegalArgumentException("Extension type should not be null");
        }
        if (!type.isInterface()) {
            throw new IllegalArgumentException("Extension type must be an interface");
        }
        if (type.getAnnotation(SPI.class) == null) {
            throw new IllegalArgumentException("Extension type must be annotated by @SPI");
        }

        //从缓存中获取加载器，如果不存在就创建新的
        ExtensionLoader<S> extensionLoader = (ExtensionLoader<S>) EXTENSION_LOADERS.get(type);
        //未获取成功
        if (extensionLoader == null) {
            //只有在指定的键不存在时，才会将键值对添加到映射中
            EXTENSION_LOADERS.putIfAbsent(type, new ExtensionLoader<>(type));
            extensionLoader = (ExtensionLoader<S>) EXTENSION_LOADERS.get(type);
        }
        return extensionLoader;
    }

    /**
     * 根据指定扩展名称获取扩展点实例（实现类实例）
     * 保证了懒加载：只有在实际需要时才进行初始化。
     * 多线程管理：采用双重检查锁定
     * 第一轮检查 if (holder == null) 是为了确保 缓存中是否已经存在对应的扩展点实例。
     * 第二轮检查 if (instance == null) 是为了 确保实例的创建过程是线程安全的。
     */
    public T getExtension(String name) {
        // 判断字符串是否为空或仅包含空白字符
        if (StringUtil.isBlank(name)) {
            throw new IllegalArgumentException("Extension name should not be null or empty.");
        }

        //现在尝试从缓存中获取扩展实例
        Holder<Object> holder = cachedInstances.get(name);
        //如果实例还没创建，则进行创建并缓存
        if (holder == null) {
            cachedInstances.putIfAbsent(name, new Holder<>());
            holder = cachedInstances.get(name);
        }
        Object instance = holder.get();
        if (instance == null) {
            //进入同步块，线程安全地创建和缓存扩展点实例
            //在多线程环境下，多个线程可能同时访问相同的扩展点，确保只有一个线程能创建实例，其他线程能够复用这个实例。
            //同步块（synchronized）是一个用来控制对共享资源访问的机制，确保在同一时刻只有一个线程能访问某个代码块。
            synchronized (holder) {
                if (instance == null) {
                    instance = createExtension(name);
                    holder.set(instance);
                }
            }
        }
        return (T) instance;
    }

    private T createExtension(String name) {
        //从扩展名-类缓存中加查找指定名称的类
        Class<?> clazz = getExtensionClasses().get(name);
        //如果找不到该类，则抛出异常
        if (clazz == null) {
            throw new RuntimeException("No such extension of name " + name);
        }

        //尝试从类-实例缓存中获取实例
        T instance = (T) EXTENSION_INSTANCES.get(clazz);
        //实例尚未创建则进行创建
        if(instance == null){
            try{
                EXTENSION_INSTANCES.putIfAbsent(clazz, clazz.newInstance());
                //通过反射动态地创建类的实例(该方法已过时)
                //新方法：通过构造函数创建类的实例：clazz.getDeclaredConstructor().newInstance();
                instance = (T) EXTENSION_INSTANCES.get(clazz);
            }catch(Exception e){
                log.error("Failed to create extension instance: " + name, e);
            }
        }
        return instance;
    }

    /**
     * 接口加载：根据扩展点接口加载其所有实现类
     * 延迟加载机制：只有在需要时才加载扩展类。
     */
    private Map<String, Class<?>> getExtensionClasses() {
        // 首先从缓存中获取扩展-类映射
        Map<String, Class<?>> classes = cachedClasses.get();

        // 如果缓存中没有扩展名-类映射（classes 为 null），则需要加载
        if (classes == null) {
            synchronized (cachedClasses) {
                classes = cachedClasses.get();
                if (classes == null) {
                    classes = new HashMap<>();
                    //通过加载并解析扩展类的配置文件来填充扩展-类映射
                    loadDirectory(classes);
                    cachedClasses.set(classes);
                }
            }
        }
        return classes;
    }

    /**
     * 配置文件管理：按规定规则读取配置文件，解析扩展名称与实现类的映射
     * 寻找符合条件的配置文件
     */
    private void loadDirectory(Map<String, Class<?>> extensionClasses) {

        //配置文件路径格式：根目录(META-INF/extensions)/<接口全限定名>
        //通过 类名.静态成员 访问静态变量或方法是 Java 的基本规则。
        String fileName = ExtensionLoader.SERVICE_DIRECTORY + type.getName();

        try {
            //使用ExtensionLoader的类加载器，加载与这个类相关的资源
            //Thread.currentThread().getContextClassLoader()获取当前线程的上下文类加载器(线程会有不同的类加载器)
            ClassLoader classLoader = ExtensionLoader.class.getClassLoader();

            //遍历所有符合资源路径的URL,只是遍历，需要用nextElement()获取每个资源的url
            Enumeration<URL> urls = classLoader.getResources(fileName);

            if (urls != null) {
                while (urls.hasMoreElements()) {
                    URL resourceUrl = urls.nextElement();
                    // 依次处理读取每一个URL的内容
                    loadResource(extensionClasses, classLoader, resourceUrl);
                }
            }
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    /**
     * 配置文件管理：按规定规则读取配置文件，解析扩展名称与实现类的映射
     * 解析配置文件
     */
    private void loadResource(Map<String, Class<?>> extensionClasses, ClassLoader classLoader, URL resourceUrl) {
        //使用 BufferedReader 包装 InputStreamReader，为文件读取提供缓冲，提高效率。
        //BufferedReader从缓冲区读取文件内容，减少对硬盘访问次数，提供按行读取
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resourceUrl.openStream(), UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // 查找#的位置，便于忽略以 # 开头的注释行,如果没有#返回-1
                final int ci = line.indexOf('#');
                if (ci >= 0) {
                    line = line.substring(0, ci);
                }
                //去掉两端的空白字符
                line = line.trim();

                if (line.length() > 0) {
                    try {
                        final int ei = line.indexOf('=');
                        // 获取扩展名称
                        String name = line.substring(0, ei).trim();
                        //获取类名
                        String clazzName = line.substring(ei + 1).trim();

                        //确保扩展名和类名都不为空
                        if (name.length() > 0 && clazzName.length() > 0) {
                            //使用类加载器加载类，并将扩展名-类映射存入extensionClasses
                            //利用反射机制，根据类的全限定名获取类
                            Class<?> clazz = classLoader.loadClass(clazzName);
                            extensionClasses.put(name, clazz);
                        }
                    } catch (ClassNotFoundException e) {
                        log.error(e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }
}
