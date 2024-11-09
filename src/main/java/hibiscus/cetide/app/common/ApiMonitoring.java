package hibiscus.cetide.app.common;

import hibiscus.cetide.app.common.model.ApiStatus;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "apiMonitoring")
public class ApiMonitoring {

    private List<ApiStatus> apiList = new ArrayList<>();

    @XmlElement(name = "api")
    public List<ApiStatus> getApiList() {
        return apiList;
    }

    public void setApiList(List<ApiStatus> apiList) {
        this.apiList = apiList;
    }
}


