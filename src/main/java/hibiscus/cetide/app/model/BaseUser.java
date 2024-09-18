package hibiscus.cetide.app.model;

public class BaseUser {
  @Override
  public String toString() {
    return "User{" +
            "id='" + id + '\'' +
            ", loginName='" + loginName + '\'' +
            ", password='" + password + '\'' +
            ", mobile='" + mobile + '\'' +
            '}';
  }

  private String id;

  // 登录名
  private String loginName;

  // 登录密码
  private String password;

  // 手机号
  private String mobile;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getLoginName() {
    return loginName;
  }

  public void setLoginName(String loginName) {
    this.loginName = loginName;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getMobile() {
    return mobile;
  }

  public void setMobile(String mobile) {
    this.mobile = mobile;
  }

  public BaseUser() {}
  public BaseUser(String id, String loginName, String password, String mobile) {
    this.id = id;
    this.loginName = loginName;
    this.password = password;
    this.mobile = mobile;
  }
}
