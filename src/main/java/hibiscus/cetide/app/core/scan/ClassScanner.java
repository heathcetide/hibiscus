package hibiscus.cetide.app.core.scan;

import hibiscus.cetide.app.core.model.AppConfigProperties;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Set;

@Component
public class ClassScanner {

    @Autowired
    private MappingHandler mappingHandler;

    @Autowired
    private AppConfigProperties appConfigProperties;
    
    public Class<?> findMainClass() {
        String scanBasePackages = appConfigProperties.getScanPath();
        if (scanBasePackages == null || scanBasePackages.isEmpty()) {
            scanBasePackages = "com.example"; // 默认包路径
        }
        
        Reflections reflections = new Reflections(new ConfigurationBuilder()
            .setUrls(ClasspathHelper.forPackage(scanBasePackages))
            .setScanners(Scanners.TypesAnnotated));
            
        Set<Class<?>> classes = reflections.getTypesAnnotatedWith(SpringBootApplication.class);
        for (Class<?> clazz : classes) {
            if (hasMainMethod(clazz)) {
                return clazz;
            }
        }
        return null;
    }

    private boolean hasMainMethod(Class<?> clazz) {
        try {
            Method method = clazz.getMethod("main", String[].class);
            return method.getReturnType().equals(Void.TYPE);
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    public void scanApplication(Class<?> mainClass) {
        if (mainClass != null && mainClass.isAnnotationPresent(SpringBootApplication.class)) {
            SpringBootApplication annotation = mainClass.getAnnotation(SpringBootApplication.class);
            String[] scanBasePackages = annotation.scanBasePackages();
            if (scanBasePackages.length == 0) {
                // 如果没有指定扫描包，使用主类所在的包
                scanPackage(mainClass.getPackage().getName());
            } else {
                for (String basePackage : scanBasePackages) {
                    scanPackage(basePackage);
                }
            }
        }
    }

    private void scanPackage(String basePackage) {
        ClassLoader classLoader = getClass().getClassLoader();
        URL resource = classLoader.getResource(basePackage.replace(".", "/"));
        if (resource != null) {
            File directory = new File(resource.getFile());
            if (directory.isDirectory()) {
                scanDirectory(directory, basePackage);
            }
        }
    }

    private void scanDirectory(File directory, String basePackage) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    scanDirectory(file, basePackage + "." + file.getName());
                } else if (file.getName().endsWith(".class")) {
                    String className = basePackage + "." + file.getName().replace(".class", "");
                    try {
                        Class<?> clazz = Class.forName(className);
                        mappingHandler.handleClass(clazz);
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
