package hibiscus.cetide.app.core;

import hibiscus.cetide.app.AppApplication;
import hibiscus.cetide.app.core.model.RequestInfo;
import org.reflections.Reflections;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URL;
import java.util.*;

@Configuration
@ComponentScan
@Component("startupEventListener")
public class StartupEventListener implements ApplicationListener<ContextRefreshedEvent> {

    public static final List<RequestInfo> requestInfos = new ArrayList<>();

    @Autowired
    private AppConfigProperties appConfigProperties;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        // 当Spring容器完成初始化后执行的操作
        Class<?> mainClass = findMainClass();
        // 主逻辑
        if (mainClass != null) {
            System.out.println("Found main class: " + mainClass.getName());
            if (mainClass.isAnnotationPresent(SpringBootApplication.class)) {
                SpringBootApplication annotation = mainClass.getAnnotation(SpringBootApplication.class);
                String[] scanBasePackages = annotation.scanBasePackages();
                System.out.println("scanBasePackages: " + Arrays.toString(scanBasePackages));
                // 将路径转换为类路径形式
                for (int i = 0; i < scanBasePackages.length; i++) {
                    scanBasePackages[i] = scanBasePackages[i].replace(".", "/");
                    ClassLoader classLoader = AppApplication.class.getClassLoader();
                    URL resource = classLoader.getResource(scanBasePackages[i]);
                    File file = new File(resource.getFile());
                    System.out.println(file);
                    if (file.isDirectory()) {
                        // 递归扫描文件夹
                        scanDirectory(file, classLoader, scanBasePackages[i].replace("/", "."));
                    }
                }
            }
        } else {
            System.out.println("No main class found.");
        }
    }

    public static void scanDirectory(File directory, ClassLoader classLoader, String basePackage) {
        if (directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    String relativePath = file.getAbsolutePath().substring(directory.getAbsolutePath().length() + 1);
                    String currentPackage = basePackage + (basePackage.isEmpty() ? "" : ".") + relativePath.replace("\\", "/");
                    // 去除 ".class" 后缀
                    if (currentPackage.endsWith(".class")) {
                        currentPackage = currentPackage.substring(0, currentPackage.length() - 6);
                    }
                    if (file.isDirectory()) {
                        // 递归扫描子文件夹
                        scanDirectory(file, classLoader, currentPackage);
                    } else if (relativePath.endsWith(".class")) {
                        // 处理 .class 文件
                        String className = relativePath.substring(0, relativePath.indexOf(".class"));
                        System.out.println("classNameNow: " + className);
                        try {
                            // 构建完整的类名
                            Class<?> aClass = classLoader.loadClass(currentPackage);
                            // 处理类上的@RequestMapping
                            String classMapping;
                            if (aClass.isAnnotationPresent(RequestMapping.class)) {
                                RequestMapping requestMapping = aClass.getAnnotation(RequestMapping.class);
                                classMapping = requestMapping.value().length > 0 ? requestMapping.value()[0] : "";
                                System.out.println("Class-level RequestMapping: " + classMapping);
                            } else {
                                classMapping = "";
                            }

                            if (aClass.isAnnotationPresent(RestController.class)) {
                                System.out.println("RestController: " + aClass.getName());
                            }

                            if (aClass.isAnnotationPresent(Controller.class)) {
                                System.out.println("Controller: " + aClass.getName());
                            }

                            // 扫描方法
                            Arrays.stream(aClass.getDeclaredMethods()).forEach(method -> {
                                if (method.isAnnotationPresent(GetMapping.class)) {
                                    handleMapping(aClass, method, GetMapping.class, method.getAnnotation(GetMapping.class).value(), classMapping);
                                } else if (method.isAnnotationPresent(PostMapping.class)) {
                                    handleMapping(aClass, method, PostMapping.class, method.getAnnotation(PostMapping.class).value(), classMapping);
                                } else if (method.isAnnotationPresent(DeleteMapping.class)) {
                                    handleMapping(aClass, method, DeleteMapping.class, method.getAnnotation(DeleteMapping.class).value(), classMapping);
                                } else if (method.isAnnotationPresent(PutMapping.class)) {
                                    handleMapping(aClass, method, PutMapping.class, method.getAnnotation(PutMapping.class).value(), classMapping);
                                }
                            });
                        } catch (ClassNotFoundException e) {
                            System.err.println("Class not found: " + currentPackage + "." + className);
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    private static void handleMapping(Class<?> aClass, Method method, Class<?> mappingType, String[] paths, String classMapping) {
        Map<String, Object> parameters = new HashMap<>();
        String methodType = ""; // 请求方法类型 (GET, POST, 等)

        // 根据 mappingType 确定请求类型
        if (mappingType == GetMapping.class) {
            methodType = "GET";
        } else if (mappingType == PostMapping.class) {
            methodType = "POST";
        } else if (mappingType == PutMapping.class) {
            methodType = "PUT";
        } else if (mappingType == DeleteMapping.class) {
            methodType = "DELETE";
        }
        for (String path : paths) {
            // 如果类上有@RequestMapping注解，则将路径前缀添加到方法的映射路径前
            String fullPath = (classMapping.isEmpty() ? "" : classMapping) + path;
            RequestInfo requestInfo = new RequestInfo(
                    aClass.getName(),
                    method.getName(),
                    Arrays.asList(fullPath),
                    parameters,
                    methodType // 添加请求方式
            );
            requestInfos.add(requestInfo);
            System.out.println(mappingType.getSimpleName() + ": " + method.getName() + " - Full Path: " + fullPath);

            // 处理方法参数
            Parameter[] methodParameters = method.getParameters();
            for (Parameter parameter : methodParameters) {
                String paramName = parameter.getName();  // 获取参数的名字
                Class<?> paramType = parameter.getType();  // 获取参数的类型

                // 检查是否存在@RequestParam注解
                if (parameter.isAnnotationPresent(RequestParam.class)) {
                    RequestParam requestParam = parameter.getAnnotation(RequestParam.class);
                    // 如果@RequestParam中定义了值，使用该值作为参数名
                    if (!requestParam.value().isEmpty()) {
                        paramName = requestParam.value();
                    }
                    System.out.println("@RequestParam detected, using param name: " + paramName);
                }

                // 将参数名和参数类型添加到parameters Map中
                parameters.put(paramName, paramType.getSimpleName());
                System.out.println("Parameter: " + paramName + " - Type: " + paramType.getName());
            }
        }
    }


    // 新增处理@RequestParam的方法
    private static void handleRequestParam(String paramName, Class<?> paramType, Map<String, Object> parameters) {
        // 将@RequestParam参数的名称和类型加入parameters Map中
        parameters.put(paramName, paramType.getSimpleName());
        System.out.println("RequestParam: " + paramName + " - Type: " + paramType.getName());
    }

    private static void handleRequestBody(Class<?> paramType, Map<String, Object> parameters) {
        // 获取类中的字段
        Field[] fields = paramType.getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true); // 允许访问私有字段
            String fieldName = field.getName();
            parameters.put(fieldName, field.getType().getSimpleName());
            System.out.println("Field: " + fieldName + " - Type: " + field.getType().getName());
        }
    }

    public  Class<?> findMainClass() {
//        String defaultPackage = "hibiscus.cetide.app";
        String configuredPackage = appConfigProperties.getHibiscus();
//        String scanBasePackages = configuredPackage.isEmpty() ? defaultPackage : configuredPackage;
        String scanBasePackages= configuredPackage;
        Reflections reflections = new Reflections(scanBasePackages);
        Set<Class<?>> classes = reflections.getTypesAnnotatedWith(SpringBootApplication.class);
        for (Class<?> clazz : classes) {
            if (hasMainMethod(clazz)) {
                return clazz;
            }
        }
        return null;
    }

    private static boolean hasMainMethod(Class<?> clazz) {
        try {
            Method method = clazz.getMethod("main", String[].class);
            return method.getReturnType().equals(Void.TYPE);
        } catch (NoSuchMethodException e) {
            return false;
        }
    }
}

