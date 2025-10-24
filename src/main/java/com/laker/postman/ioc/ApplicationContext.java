package com.laker.postman.ioc;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 轻量级IOC容器
 * 负责管理所有的Bean实例，支持依赖注入
 */
@Slf4j
public class ApplicationContext {

    /**
     * 单例实例
     */
    private static volatile ApplicationContext instance;

    /**
     * Bean定义注册表 key: beanName, value: BeanDefinition
     */
    private final Map<String, BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<>();

    /**
     * Bean类型索引 key: class type, value: beanName list
     */
    private final Map<Class<?>, List<String>> typeIndexMap = new ConcurrentHashMap<>();

    /**
     * ========== 三级缓存机制（解决循环依赖） ==========
     */

    /**
     * 一级缓存：单例对象缓存
     * 存放完全初始化好的Bean实例
     */
    private final Map<String, Object> singletonObjects = new ConcurrentHashMap<>();

    /**
     * 二级缓存：早期单例对象缓存
     * 存放提前暴露的Bean实例（已实例化但未初始化完成）
     */
    private final Map<String, Object> earlySingletonObjects = new ConcurrentHashMap<>();

    /**
     * 三级缓存：单例工厂缓存
     * 存放Bean工厂，用于获取提前暴露的Bean实例
     */
    private final Map<String, ObjectFactory<?>> singletonFactories = new ConcurrentHashMap<>();

    /**
     * 正在创建的Bean集合，用于标记Bean创建状态
     */
    private final Set<String> singletonsCurrentlyInCreation = ConcurrentHashMap.newKeySet();

    /**
     * 保存带有@PreDestroy方法的Bean实例，用于容器关闭时调用
     */
    private final Map<Object, List<Method>> preDestroyMethods = new ConcurrentHashMap<>();

    private ApplicationContext() {
    }

    /**
     * 获取IOC容器单例实例
     */
    public static ApplicationContext getInstance() {
        if (instance == null) {
            synchronized (ApplicationContext.class) {
                if (instance == null) {
                    instance = new ApplicationContext();
                }
            }
        }
        return instance;
    }

    /**
     * 扫描指定包下的所有类，注册带有@Component注解的类
     */
    public void scan(String... basePackages) {
        for (String basePackage : basePackages) {
            try {
                scanPackage(basePackage);
            } catch (Exception e) {
                log.error("Failed to scan package: {}", basePackage, e);
            }
        }
    }

    /**
     * 扫描包
     */
    private void scanPackage(String basePackage) throws Exception {
        String packagePath = basePackage.replace('.', '/');
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Enumeration<URL> resources = classLoader.getResources(packagePath);

        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            String protocol = resource.getProtocol();

            if ("file".equals(protocol)) {
                String filePath = resource.getFile();
                scanFile(new File(filePath), basePackage);
            }
        }
    }

    /**
     * 递归扫描文件
     */
    private void scanFile(File file, String packageName) {
        if (!file.exists()) {
            return;
        }

        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.isDirectory()) {
                        scanFile(f, packageName + "." + f.getName());
                    } else if (f.getName().endsWith(".class")) {
                        String className = packageName + "." + f.getName().replace(".class", "");
                        try {
                            Class<?> clazz = Class.forName(className);
                            if (clazz.isAnnotationPresent(Component.class)) {
                                registerBean(clazz);
                            }
                        } catch (ClassNotFoundException | NoClassDefFoundError e) {
                            log.warn("Failed to load class: {}", className);
                        }
                    }
                }
            }
        }
    }

    /**
     * 注册Bean
     */
    private void registerBean(Class<?> clazz) {
        Component component = clazz.getAnnotation(Component.class);
        String beanName = component.value();

        if (beanName.isEmpty()) {
            // 默认使用类名首字母小写作为bean名称
            beanName = getBeanName(clazz);
        }

        // 检查作用域：默认为单例，除非明确指定为 prototype
        boolean singleton = true;
        if (clazz.isAnnotationPresent(Scope.class)) {
            Scope scope = clazz.getAnnotation(Scope.class);
            singleton = !Scope.PROTOTYPE.equals(scope.value());
        }

        BeanDefinition beanDefinition = new BeanDefinition(beanName, clazz, singleton);

        beanDefinitionMap.put(beanName, beanDefinition);

        // 建立类型索引
        typeIndexMap.computeIfAbsent(clazz, k -> new ArrayList<>()).add(beanName);

        // 同时索引所有接口和父类
        indexInterfaces(clazz, beanName);

        log.debug("Registered bean: {} -> {} (singleton={})", beanName, clazz.getName(), singleton);
    }

    /**
     * 索引接口和父类
     */
    private void indexInterfaces(Class<?> clazz, String beanName) {
        // 索引接口
        Class<?>[] interfaces = clazz.getInterfaces();
        for (Class<?> iface : interfaces) {
            typeIndexMap.computeIfAbsent(iface, k -> new ArrayList<>()).add(beanName);
        }

        // 索引父类
        Class<?> superClass = clazz.getSuperclass();
        if (superClass != null && superClass != Object.class) {
            typeIndexMap.computeIfAbsent(superClass, k -> new ArrayList<>()).add(beanName);
            indexInterfaces(superClass, beanName);
        }
    }

    /**
     * 手动注册Bean实例
     */
    public void registerBean(String beanName, Object bean) {
        BeanDefinition beanDefinition = new BeanDefinition(beanName, bean.getClass(), true);
        beanDefinitionMap.put(beanName, beanDefinition);

        // 直接放入一级缓存（已经是完整的Bean实例）
        singletonObjects.put(beanName, bean);

        // 建立类型索引
        typeIndexMap.computeIfAbsent(bean.getClass(), k -> new ArrayList<>()).add(beanName);
        indexInterfaces(bean.getClass(), beanName);

        log.debug("Registered bean instance: {} -> {}", beanName, bean.getClass().getName());
    }

    /**
     * 根据名称获取Bean
     */
    @SuppressWarnings("unchecked")
    public <T> T getBean(String beanName) {
        BeanDefinition beanDefinition = beanDefinitionMap.get(beanName);
        if (beanDefinition == null) {
            throw new NoSuchBeanException(beanName);
        }

        return (T) getBean(beanDefinition);
    }

    /**
     * 根据类型获取Bean
     */
    public <T> T getBean(Class<T> requiredType) {
        List<String> beanNames = typeIndexMap.get(requiredType);

        if (beanNames == null || beanNames.isEmpty()) {
            throw new NoSuchBeanException(requiredType);
        }

        if (beanNames.size() > 1) {
            throw new BeanException("Multiple beans of type '" + requiredType.getName() + "' found: " + beanNames);
        }

        return getBean(beanNames.get(0));
    }

    /**
     * 根据BeanDefinition获取Bean实例（支持三级缓存）
     */
    private Object getBean(BeanDefinition beanDefinition) {
        String beanName = beanDefinition.getName();

        if (beanDefinition.isSingleton()) {
            // 使用三级缓存获取单例Bean
            return getSingleton(beanName, () -> createBean(beanDefinition));
        } else {
            // 非单例直接创建
            return createBean(beanDefinition);
        }
    }

    /**
     * 获取单例Bean（三级缓存核心方法）
     *
     * @param beanName Bean名称
     * @param singletonFactory Bean工厂
     * @return Bean实例
     */
    private Object getSingleton(String beanName, ObjectFactory<?> singletonFactory) {
        // 1. 先从一级缓存获取完全初始化的Bean
        Object singletonObject = singletonObjects.get(beanName);

        // 2. 如果一级缓存没有，且Bean正在创建中（说明存在循环依赖）
        if (singletonObject == null && singletonsCurrentlyInCreation.contains(beanName)) {
            // 加锁保证线程安全
            synchronized (this.singletonObjects) {
                // 双重检查
                singletonObject = singletonObjects.get(beanName);
                if (singletonObject == null) {
                    // 从二级缓存获取早期引用
                    singletonObject = earlySingletonObjects.get(beanName);

                    // 3. 如果二级缓存也没有，从三级缓存获取
                    if (singletonObject == null) {
                        ObjectFactory<?> factory = singletonFactories.get(beanName);
                        if (factory != null) {
                            try {
                                // 从工厂获取早期引用
                                singletonObject = factory.getObject();
                                // 放入二级缓存，移除三级缓存
                                earlySingletonObjects.put(beanName, singletonObject);
                                singletonFactories.remove(beanName);
                                log.debug("Resolved circular dependency for bean: {}", beanName);
                            } catch (Exception e) {
                                throw new BeanCreationException(beanName, "Failed to get early reference", e);
                            }
                        }
                    }
                }
            }
        }

        // 4. 如果所有缓存都没有，创建新的Bean
        if (singletonObject == null) {
            // 使用同步锁保证单例Bean只被创建一次
            synchronized (this.singletonObjects) {
                // 再次检查缓存（可能其他线程已创建）
                singletonObject = singletonObjects.get(beanName);
                if (singletonObject == null) {
                    // 标记Bean正在创建
                    singletonsCurrentlyInCreation.add(beanName);

                    try {
                        // 调用工厂方法创建Bean
                        singletonObject = singletonFactory.getObject();

                        // 从二级缓存移除（如果存在）
                        earlySingletonObjects.remove(beanName);
                        // 从三级缓存移除（如果存在）
                        singletonFactories.remove(beanName);
                        // 放入一级缓存
                        singletonObjects.put(beanName, singletonObject);

                        log.debug("Created and cached singleton bean: {}", beanName);
                    } catch (Exception e) {
                        // 创建失败，清理状态
                        singletonsCurrentlyInCreation.remove(beanName);
                        earlySingletonObjects.remove(beanName);
                        singletonFactories.remove(beanName);
                        throw new BeanCreationException(beanName, e);
                    } finally {
                        // 创建完成，移除创建标记
                        singletonsCurrentlyInCreation.remove(beanName);
                    }
                }
            }
        }

        return singletonObject;
    }

    /**
     * 创建Bean实例（支持三级缓存解决循环依赖）
     */
    private Object createBean(BeanDefinition beanDefinition) {
        String beanName = beanDefinition.getName();
        Class<?> beanClass = beanDefinition.getBeanClass();

        try {
            // 1. 实例化Bean（仅创建对象，不注入依赖）
            Object instance = instantiateBean(beanClass);

            // 2. 如果是单例Bean，提前暴露到三级缓存（解决循环依赖的关键）
            if (beanDefinition.isSingleton()) {
                final Object finalInstance = instance;
                // 将Bean工厂放入三级缓存
                singletonFactories.put(beanName, () -> getEarlyBeanReference(beanName, finalInstance));
                log.debug("Added bean factory to third-level cache for: {}", beanName);
            }

            // 3. 属性注入（此时如果有循环依赖，会从三级缓存获取早期引用）
            injectFields(instance);

            // 4. 调用初始化方法
            invokePostConstruct(instance);
            invokeInitializingBean(instance);

            // 5. 保存销毁方法
            registerPreDestroyMethods(instance);

            log.debug("Created bean: {}", beanName);
            return instance;

        } catch (BeanCreationException e) {
            // 直接重新抛出 BeanCreationException
            throw e;
        } catch (Exception e) {
            throw new BeanCreationException(beanName, e);
        }
    }

    /**
     * 实例化Bean（使用构造函数）
     */
    private Object instantiateBean(Class<?> beanClass) throws Exception {
        // 查找带@Autowired的构造函数
        Constructor<?> targetConstructor = null;
        Constructor<?>[] constructors = beanClass.getDeclaredConstructors();

        for (Constructor<?> constructor : constructors) {
            if (constructor.isAnnotationPresent(Autowired.class)) {
                targetConstructor = constructor;
                break;
            }
        }

        if (targetConstructor != null) {
            // 使用带@Autowired的构造函数
            Class<?>[] paramTypes = targetConstructor.getParameterTypes();
            Object[] params = new Object[paramTypes.length];

            for (int i = 0; i < paramTypes.length; i++) {
                params[i] = getBean(paramTypes[i]);
            }

            targetConstructor.setAccessible(true);
            return targetConstructor.newInstance(params);
        } else {
            // 使用无参构造函数
            return beanClass.getDeclaredConstructor().newInstance();
        }
    }

    /**
     * 获取早期Bean引用（用于解决循环依赖）
     * 这个方法返回的是实例化但未完全初始化的Bean
     *
     * @param beanName Bean名称
     * @param bean Bean实例
     * @return 早期Bean引用
     */
    private Object getEarlyBeanReference(String beanName, Object bean) {
        // 这里可以返回代理对象（如果需要AOP）
        // 目前直接返回原始对象
        log.debug("Getting early reference for bean: {}", beanName);
        return bean;
    }

    /**
     * 字段注入
     */
    private void injectFields(Object instance) {
        Class<?> clazz = instance.getClass();

        while (clazz != null && clazz != Object.class) {
            Field[] fields = clazz.getDeclaredFields();

            for (Field field : fields) {
                if (field.isAnnotationPresent(Autowired.class)) {
                    Autowired autowired = field.getAnnotation(Autowired.class);

                    try {
                        Object dependency = getBean(field.getType());
                        field.setAccessible(true);
                        field.set(instance, dependency);
                        log.debug("Injected field '{}' in bean '{}'", field.getName(), instance.getClass().getSimpleName());
                    } catch (NoSuchBeanException e) {
                        if (autowired.required()) {
                            throw new BeanCreationException(
                                instance.getClass().getSimpleName(),
                                "Failed to inject required field '" + field.getName() + "': " + e.getMessage(),
                                e
                            );
                        } else {
                            log.debug("Skipped optional field injection: {}.{}", clazz.getSimpleName(), field.getName());
                        }
                    } catch (Exception e) {
                        throw new BeanCreationException(
                            instance.getClass().getSimpleName(),
                            "Failed to inject field '" + field.getName() + "'",
                            e
                        );
                    }
                }
            }

            clazz = clazz.getSuperclass();
        }
    }

    /**
     * 调用@PostConstruct方法
     */
    private void invokePostConstruct(Object instance) {
        Class<?> clazz = instance.getClass();

        while (clazz != null && clazz != Object.class) {
            Method[] methods = clazz.getDeclaredMethods();

            for (Method method : methods) {
                if (method.isAnnotationPresent(PostConstruct.class)) {
                    // 验证方法签名
                    if (method.getParameterCount() != 0) {
                        throw new BeanCreationException(
                            instance.getClass().getSimpleName(),
                            "@PostConstruct method '" + method.getName() + "' must have no parameters"
                        );
                    }

                    try {
                        method.setAccessible(true);
                        method.invoke(instance);
                        log.debug("Invoked @PostConstruct method: {}.{}", clazz.getSimpleName(), method.getName());
                    } catch (Exception e) {
                        throw new BeanCreationException(
                            instance.getClass().getSimpleName(),
                            "Failed to invoke @PostConstruct method '" + method.getName() + "'",
                            e
                        );
                    }
                }
            }

            clazz = clazz.getSuperclass();
        }
    }

    /**
     * 调用InitializingBean接口的afterPropertiesSet方法
     */
    private void invokeInitializingBean(Object instance) {
        if (instance instanceof InitializingBean initializingBean) {
            try {
                initializingBean.afterPropertiesSet();
                log.debug("Invoked InitializingBean.afterPropertiesSet() for: {}",
                        instance.getClass().getSimpleName());
            } catch (Exception e) {
                throw new BeanCreationException(
                    instance.getClass().getSimpleName(),
                    "Failed to invoke afterPropertiesSet()",
                    e
                );
            }
        }
    }

    /**
     * 注册@PreDestroy方法
     */
    private void registerPreDestroyMethods(Object instance) {
        List<Method> destroyMethods = new ArrayList<>();
        Class<?> clazz = instance.getClass();

        while (clazz != null && clazz != Object.class) {
            Method[] methods = clazz.getDeclaredMethods();

            for (Method method : methods) {
                if (method.isAnnotationPresent(PreDestroy.class)) {
                    if (method.getParameterCount() != 0) {
                        throw new BeanCreationException(
                            instance.getClass().getSimpleName(),
                            "@PreDestroy method '" + method.getName() + "' must have no parameters"
                        );
                    }

                    method.setAccessible(true);
                    destroyMethods.add(method);
                }
            }

            clazz = clazz.getSuperclass();
        }

        if (!destroyMethods.isEmpty()) {
            preDestroyMethods.put(instance, destroyMethods);
        }
    }

    /**
     * 销毁所有Bean，调用@PreDestroy方法和DisposableBean接口
     */
    public void destroy() {
        log.info("Destroying all beans...");

        // 1. 先调用所有@PreDestroy方法
        for (Map.Entry<Object, List<Method>> entry : preDestroyMethods.entrySet()) {
            Object instance = entry.getKey();
            List<Method> methods = entry.getValue();

            for (Method method : methods) {
                try {
                    method.invoke(instance);
                    log.debug("Invoked @PreDestroy method: {}.{}",
                            instance.getClass().getSimpleName(), method.getName());
                } catch (Exception e) {
                    log.error("Failed to invoke @PreDestroy method: {}", method.getName(), e);
                }
            }
        }

        // 2. 然后调用DisposableBean接口的destroy方法
        for (Object instance : singletonObjects.values()) {
            if (instance instanceof DisposableBean disposableBean) {
                try {
                    disposableBean.destroy();
                    log.debug("Invoked DisposableBean.destroy() for: {}",
                            instance.getClass().getSimpleName());
                } catch (Exception e) {
                    log.error("Failed to invoke DisposableBean.destroy() for: {}",
                            instance.getClass().getName(), e);
                }
            }
        }

        preDestroyMethods.clear();
        log.info("All beans destroyed");
    }

    /**
     * 获取Bean名称（类名首字母小写）
     */
    private String getBeanName(Class<?> clazz) {
        String simpleName = clazz.getSimpleName();
        return Character.toLowerCase(simpleName.charAt(0)) + simpleName.substring(1);
    }

    /**
     * 获取所有Bean名称
     */
    public Set<String> getBeanNames() {
        return new HashSet<>(beanDefinitionMap.keySet());
    }

    /**
     * 检查是否包含指定名称的Bean
     */
    public boolean containsBean(String beanName) {
        return beanDefinitionMap.containsKey(beanName);
    }

    /**
     * 检查指定Bean是否为单例
     */
    public boolean isSingleton(String beanName) {
        BeanDefinition beanDefinition = beanDefinitionMap.get(beanName);
        return beanDefinition != null && beanDefinition.isSingleton();
    }

    /**
     * 清空容器
     */
    public void clear() {
        // 先销毁所有Bean
        destroy();

        beanDefinitionMap.clear();
        typeIndexMap.clear();
        singletonObjects.clear();
        earlySingletonObjects.clear();
        singletonFactories.clear();
        singletonsCurrentlyInCreation.clear();
        log.info("ApplicationContext cleared");
    }
}

