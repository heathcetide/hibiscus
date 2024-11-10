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

  private static final Logger LOG = LoggerFactory.getLogger(BaseUserControl.class);

  @Autowired
  private BaseUserService userService;

  @Autowired
  private AppConfigProperties appConfigProperties;

  @Autowired
  private ExpiredLRUCache<String, Object> cache;

  @Autowired
  private ApiUrlUtil apiUrlUtil;

  @PostConstruct
  public void init() {



    LOG.info("UserControl 启动啦");
    LOG.info("userService 注入啦");
  }
  @GetMapping("/login")
  public String loginPage(Model model) {
    return "login";
  }

  @PostMapping("/authenticate")
  @ResponseBody
  public Map<String, Object> login(@RequestParam(value = "name") String name,
                                   @RequestParam(value = "password") String password,
                                   HttpServletRequest request) {
    System.out.println("Received login request"+ name+" "+password);
   Map returnData = new HashMap();

    if(appConfigProperties.getUsername().equals(name)&&appConfigProperties.getPassword().equals(password)){
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
// 假设 RequestInfo 类和 uniqueRequestInfoMap 已经定义好
    for (RequestInfo requestInfo : requestInfos) {
      System.out.println("Class Name: " + requestInfo.getClassName());
      System.out.println("Method Name: " + requestInfo.getMethodName());
      System.out.println("Paths: " + requestInfo.getPaths());
//      System.out.println("Parameters: " + requestInfo.getParameters());

      // 创建 ObjectMapper 实例
//      ObjectMapper objectMapper = new ObjectMapper();

      try {
//        // 将参数转换为 JSON 字符串
//        String jsonString = objectMapper.writeValueAsString(requestInfo.getParameters());
//        requestInfo.setParams(jsonString);

        // 如果 className 不在 Map 中，就加入，已经存在的 className 会被后来的覆盖
        uniqueRequestInfoMap.put(requestInfo.getClassName(), requestInfo);
      } catch (Exception e) {
        // 处理转换异常
        e.printStackTrace();
      }
    }
// 遍历 requestInfos 列表
//    for (RequestInfo requestInfo : requestInfos) {
//      System.out.println("Class Name: " + requestInfo.getClassName());
//      System.out.println("Method Name: " + requestInfo.getMethodName());
//      System.out.println("Paths: " + requestInfo.getPaths());
//      System.out.println("Parameters: " + requestInfo.getParameters());
//
//
//      // 将参数转换为 JSON 字符串
//      Gson gson = new Gson();
//      String jsonString = gson.toJson(requestInfo.getParameters());
//      requestInfo.setParams(jsonString);
//
//      // 如果 className 不在 Map 中，就加入，已经存在的 className 会被后来的覆盖
//      uniqueRequestInfoMap.put(requestInfo.getClassName(), requestInfo);
//    }

// 将 Map 的值转换为 Set，确保没有重复的 className
    Set<RequestInfo> requestInfoSet = new HashSet<>(uniqueRequestInfoMap.values());

    model.addAttribute("requestInfoSet", requestInfoSet);
    model.addAttribute("requestInfos", requestInfos);
    model.addAttribute("baseURL", apiUrlUtil.getServerUrl());
// 返回模板名称
    return "interfaceTest";
  }
  @GetMapping(value={"/sign","/"})
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

//  private BaseUser getUserByLoginName(String loginName) {
//    BaseUser regedUser = null;
//    BaseUserQueryParam param = new BaseUserQueryParam();
//    param.setLoginName(loginName);
//    Page<BaseUser> users = userService.list(param);
////
////    // 如果登录名正确，只取第一个，要保证用户名不能重复
////    if (users != null && users.getContent() != null && users.getContent().size() > 0) {
////      regedUser = users.getContent().get(0);
////    }
//
//    return regedUser;
//  }
}
