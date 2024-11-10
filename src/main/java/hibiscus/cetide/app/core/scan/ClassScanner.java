package hibiscus.cetide.app.core.scan;

import hibiscus.cetide.app.common.utils.AppConfigProperties;
import org.reflections.Reflections;
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
    public  Class<?> findMainClass() {
        String scanBasePackages= appConfigProperties.getHibiscus();
        Reflections reflections = new Reflections(scanBasePackages);
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
        if (mainClass.isAnnotationPresent(SpringBootApplication.class)) {
            SpringBootApplication annotation = mainClass.getAnnotation(SpringBootApplication.class);
            String[] scanBasePackages = annotation.scanBasePackages();
            for (String basePackage : scanBasePackages) {
                scanPackage(basePackage);
            }
        }
    }

    private void scanPackage(String basePackage) {
        // 转换包名并扫描类
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
