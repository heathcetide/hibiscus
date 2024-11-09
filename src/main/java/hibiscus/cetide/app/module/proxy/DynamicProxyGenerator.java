package hibiscus.cetide.app.module.proxy;//package hibiscus.cetide.app.module.proxy;
//
//import javax.tools.JavaCompiler;
//import javax.tools.ToolProvider;
//import java.io.File;
//import java.io.FileWriter;
//import java.io.IOException;
//import java.lang.reflect.InvocationHandler;
//import java.lang.reflect.Method;
//import java.lang.reflect.Proxy;
//
//public class DynamicProxyGenerator {
//
//    public static <T> T createProxy(Class<T> targetClass, String moduleName) {
//        try {
//            // 生成临时 Java 文件
//            File tempFile = createTempJavaFile(targetClass.getName(), moduleName);
//
//            // 编译临时文件
//            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
//            compiler.run(null, null, null, tempFile.getPath());
//
//            // 加载编译后的类
//            Class<?> enhancedClass = Class.forName(targetClass.getName() + "Enhanced");
//
//            // 创建代理对象
//            return (T) Proxy.newProxyInstance(targetClass.getClassLoader(), targetClass.getInterfaces(), new InvocationHandler() {
//                @Override
//                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
//                    Object enhancedObject = enhancedClass.newInstance();
//                    Method enhancedMethod = enhancedClass.getMethod(method.getName(), method.getParameterTypes());
//                    return enhancedMethod.invoke(enhancedObject, args);
//                }
//            });
//        } catch (Exception e) {
//            e.printStackTrace();
//            return null;
//        } finally {
//            cleanTempFiles();
//        }
//    }
//
//    private static File createTempJavaFile(String className, String moduleName) throws IOException {
//        File tempDir = new File("temp");
//        if (!tempDir.exists()) {
//            tempDir.mkdirs();
//        }
//        File tempFile = new File(tempDir, className + "Enhanced.java");
//        try (FileWriter writer = new FileWriter(tempFile)) {
//            writer.write("class " + className + "Enhanced implements " + className + " {");
//            writer.write("    @Override");
//            writer.write("    public void performAction() {");
//            writer.write("        " + getLogicForModule(moduleName) + ";");
//            writer.write("        super.performAction();");
//            writer.write("    }");
//            writer.write("}");
//        }
//        return tempFile;
//    }
//
//    private static String getLogicForModule(String moduleName) {
//        // 根据模块名返回相应的增强逻辑
//        if ("MonitorTime".equals(moduleName)) {
//            return "System.out.println(\"Monitoring execution time...\");";
//        }
//        return "";
//    }
//
//    private static void cleanTempFiles() {
//        File tempDir = new File("temp");
//        for (File file : tempDir.listFiles()) {
//            file.delete();
//        }
//    }
//}
///**
// * # 目标类全限定名=增强逻辑描述
// * com.example.TargetClass=System.out.println("Before performing action.");
// *
// *
// * public class FrameworkStarter {
// *     public static void main(String[] args) {
// *         Class<?> targetClass = TargetClass.class;
// *         TargetClass proxy = DynamicProxyGenerator.createProxy(targetClass);
// *         proxy.performAction();
// *     }
// * }
// */