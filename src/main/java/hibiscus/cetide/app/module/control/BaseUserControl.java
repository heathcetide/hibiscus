package hibiscus.cetide.app.module.control;

import hibiscus.cetide.app.common.utils.ApiUrlUtil;
import hibiscus.cetide.app.common.utils.AppConfigProperties;
import hibiscus.cetide.app.common.model.BaseUser;
import hibiscus.cetide.app.common.model.BaseUserLoginInfo;

import java.util.*;
import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import hibiscus.cetide.app.common.model.RequestInfo;
import hibiscus.cetide.app.module.service.BaseUserService;
import hibiscus.cetide.app.common.utils.ExpiredLRUCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import static hibiscus.cetide.app.core.scan.MappingHandler.requestInfos;


@Controller
@RequestMapping("/app")
@SuppressWarnings("unchecked")
public class BaseUserControl {
    @Autowired
    private BaseUserService userService;

    @Autowired
    private AppConfigProperties appConfigProperties;

    @Autowired
    private ExpiredLRUCache<String, Object> cache;

    @Autowired
    private ApiUrlUtil apiUrlUtil;

    @GetMapping("/login")
    public String loginPage(Model model) {
        return "login";
    }

    @PostMapping("/authenticate")
    @ResponseBody
    public Map<String, Object> login(@RequestParam(value = "name") String name,
                                     @RequestParam(value = "password") String password,
                                     HttpServletRequest request) {
        System.out.println("Received login request" + name + " " + password);
        Map returnData = new HashMap();

        if (appConfigProperties.getUsername().equals(name) && appConfigProperties.getPassword().equals(password)) {
            BaseUserLoginInfo userLoginInfo = new BaseUserLoginInfo();
            userLoginInfo.setUserId("123456789abcd");
            userLoginInfo.setUserName(name);
            // 取得 HttpSession 对象
            HttpSession session = request.getSession();
            // 写入登录信息
            session.setAttribute("userLoginInfo", userLoginInfo);
            returnData.put("result", true);
            returnData.put("message", "login successfule");
            return returnData;
        }
        // 在内存中根据登录名查询用户
        BaseUser user = (BaseUser) cache.get(name);
        // 找不到此登录用户
        if (user == null) {
            returnData.put("result", false);
            returnData.put("message", "userName not correct");
            return returnData;
        }
        if (user.getPassword().equals(password)) {
            BaseUserLoginInfo userLoginInfo = new BaseUserLoginInfo();
            userLoginInfo.setUserId("123456789abcd");
            userLoginInfo.setUserName(name);
            // 取得 HttpSession 对象
            HttpSession session = request.getSession();
            // 写入登录信息
            session.setAttribute("userLoginInfo", userLoginInfo);
            returnData.put("result", true);
            returnData.put("message", "login successfule");
        } else {
            returnData.put("result", false);
            returnData.put("message", "userName or password not correct");
        }
        return returnData;
    }

    @GetMapping("/interface")
    public String InterfacePage(Model model) {
        // 使用 Map 来过滤掉相同的 className
        Map<String, RequestInfo> uniqueRequestInfoMap = new HashMap<>();
        for (RequestInfo requestInfo : requestInfos) {
//            System.out.println("Class Name: " + requestInfo.getClassName());
//            System.out.println("Method Name: " + requestInfo.getMethodName());
//            System.out.println("Paths: " + requestInfo.getPaths());
            try {
                uniqueRequestInfoMap.put(requestInfo.getClassName(), requestInfo);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        Set<RequestInfo> requestInfoSet = new HashSet<>(uniqueRequestInfoMap.values());

        model.addAttribute("requestInfoSet", requestInfoSet);
        model.addAttribute("requestInfos", requestInfos);
        model.addAttribute("baseURL", apiUrlUtil.getServerUrl());
        return "interfaceTest";
    }

    @GetMapping(value = {"/sign", "/"})
    public String signPage(Model model) {
        return "sign";
    }

    @GetMapping("/codeGenerator")
    public String codeGeneratorPage(Model model) {
        return "codeGenerator";
    }

    @PostMapping("/register")
    @ResponseBody
    public Map registerAction(@RequestParam String name, @RequestParam String password, @RequestParam String mobile,
                              HttpServletRequest request, HttpServletResponse response) {
        Map returnData = new HashMap();
        BaseUser user = new BaseUser();
        user.setLoginName(name);
        user.setPassword(password);
        user.setMobile(mobile);
        BaseUser newUser = userService.add(user);
        if (newUser != null) {
            returnData.put("result", true);
            returnData.put("message", "register successful");
        } else {
            returnData.put("result", false);
            returnData.put("message", "register failed");
        }
        return returnData;
    }

}
