package hibiscus.cetide.app.control;

import hibiscus.cetide.app.listener.ListenerAspect;
import hibiscus.cetide.app.model.CodeGenerationResponse;
import hibiscus.cetide.app.model.MethodMetrics;
import hibiscus.cetide.app.service.impl.CodeGeneratorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/code")
public class CodeGeneratorControl {

    @Autowired
    private CodeGeneratorService codeGeneratorService;


    @GetMapping("/generate-html")
    public String codeGeneratePage(Model model) {
        return "codeGenerator";
    }
    @PostMapping({"/generate"})
    public String generateCode(
            @RequestParam(value = "className", required = false) String className,
            @RequestParam(value = "packageName", required = false) String packageName,
            @RequestParam(value = "fields", required = false) String fields,
            Model model) {
        String generatedCode = codeGeneratorService.generateCode(className, packageName, fields);
        model.addAttribute("generatedCode", generatedCode);
        return "codeGenerator";
    }

    @PostMapping("/generateModule")
    @ResponseBody
    public CodeGenerationResponse generateModule(@RequestParam(value = "module", required = false) String module){
            String generatedCode = codeGeneratorService.generateModel(module);
            System.out.println("generatedCode = " + generatedCode);
            return new CodeGenerationResponse(generatedCode);
    }
    @Autowired
    private ListenerAspect listenerAspect;
    @GetMapping("/interface-analysis")
    public String interfaceAnalysis(Model model) {
        List<MethodMetrics> methodMetrics = listenerAspect.getMethodMetrics();
        model.addAttribute("methodMetrics", methodMetrics);
        return "interfaceAnalysis";
    }
    @GetMapping("/method-metrics")
    @ResponseBody
    public List<MethodMetrics> getMethodMetrics() {
        return listenerAspect.getMethodMetrics();
    }
    @GetMapping("/method-metrics/{methodName}")
    public ResponseEntity<List<Long>> getMethodExecutionHistory(@PathVariable String methodName) {
        MethodMetrics metrics = listenerAspect.getMethodMetrics().stream()
                .filter(m -> m.getMethodName().equals(methodName))
                .findFirst()
                .orElse(null);

        if (metrics != null) {
            List<Long> executionTimes = metrics.getExecutionTimeHistory();
            System.out.println("Execution times for " + methodName + ": " + executionTimes);
            return ResponseEntity.ok(executionTimes);
        }
        return ResponseEntity.notFound().build();
    }
}
