package hibiscus.cetide.app.control;

import com.google.gson.Gson;
import hibiscus.cetide.app.model.BaseUser;
import hibiscus.cetide.app.model.BaseUserLoginInfo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import hibiscus.cetide.app.model.BaseUserQueryParam;
import hibiscus.cetide.app.model.RequestInfo;
import hibiscus.cetide.app.service.BaseUserService;
import hibiscus.cetide.app.utils.ExpiredLRUCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import static hibiscus.cetide.app.config.StartupEventListener.requestInfos;

@Controller
@RequestMapping(path = "/app")
public class BaseUserControl {

  private static final Logger LOG = LoggerFactory.getLogger(BaseUserControl.class);

  @Autowired
  private BaseUserService userService;

  @Autowired
  private ExpiredLRUCache<String, Object> cache;
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
  public Map login(@RequestParam String name, @RequestParam String password, HttpServletRequest request,
                   HttpServletResponse response) {
    Map returnData = new HashMap();
    // 根据登录名查询用户
    BaseUser regedUser = getUserByLoginName(name);

    // 找不到此登录用户
    if (regedUser == null) {
      returnData.put("result", false);
      returnData.put("message", "userName not correct");
      return returnData;
    }

    if (regedUser.getPassword().equals(password)) {
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

  @GetMapping("/sign")
  public String signPage(Model model) {
    List<Object> allValues = cache.getAllValuesFromList();
    System.out.println(allValues);
    for (Object object : allValues){
      LOG.info("cache: " + object.toString());
      System.out.println(object.toString());
    }
    // 使用增强的 for 循环遍历列表
    for (RequestInfo requestInfo : requestInfos) {
      System.out.println("Class Name: " + requestInfo.getClassName());
      System.out.println("Method Name: " + requestInfo.getMethodName());
      System.out.println("Paths: " + requestInfo.getPaths());
      System.out.println("Parameters: " + requestInfo.getParameters());
      Gson gson = new Gson();
      String jsonString = gson.toJson(requestInfo.getParameters());
      requestInfo.setParams(jsonString);
    }

    model.addAttribute("requestInfos", requestInfos);
    return "sign";
  }

  @PostMapping("/register")
  @ResponseBody
  public Map registerAction(@RequestParam String name, @RequestParam String password, @RequestParam String mobile,
                            HttpServletRequest request, HttpServletResponse response) {
    Map returnData = new HashMap();

//    // 判断登录名是否已存在
//    User regedUser = getUserByLoginName(name);
//    if (regedUser != null) {
//      returnData.put("result", false);
//      returnData.put("message", "login name already exist");
//      return returnData;
//    }

    BaseUser user = new BaseUser();
    user.setLoginName(name);
    user.setPassword(password);
    user.setMobile(mobile);
    BaseUser newUser = userService.add(user);
    if (newUser != null && StringUtils.hasText(newUser.getId())) {
      returnData.put("result", true);
      returnData.put("message", "register successfule");
    } else {
      returnData.put("result", false);
      returnData.put("message", "register failed");
    }
    return returnData;
  }

  private BaseUser getUserByLoginName(String loginName) {
    BaseUser regedUser = null;
    BaseUserQueryParam param = new BaseUserQueryParam();
    param.setLoginName(loginName);
    Page<BaseUser> users = userService.list(param);
//
//    // 如果登录名正确，只取第一个，要保证用户名不能重复
//    if (users != null && users.getContent() != null && users.getContent().size() > 0) {
//      regedUser = users.getContent().get(0);
//    }

    return regedUser;
  }
}
