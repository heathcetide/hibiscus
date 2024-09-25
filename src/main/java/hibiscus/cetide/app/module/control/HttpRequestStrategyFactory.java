package hibiscus.cetide.app.module.control;

import hibiscus.cetide.app.module.service.HttpRequestStrategy;
import hibiscus.cetide.app.module.service.impl.GetRequestStrategy;
import hibiscus.cetide.app.module.service.impl.PostRequestStrategy;
import org.springframework.stereotype.Component;

@Component
public class HttpRequestStrategyFactory {
    public HttpRequestStrategy createStrategy(String method) {
        switch (method.toUpperCase()) {
            case "GET":
                return new GetRequestStrategy();
            case "POST":
                return new PostRequestStrategy();
//            case "PUT":
//                return new PutRequestStrategy(httpClient);
//            case "PATCH":
//                return new PatchRequestStrategy(httpClient);
//            case "DELETE":
//                return new DeleteRequestStrategy(httpClient);
            default:
                throw new IllegalArgumentException("Unsupported HTTP method: " + method);
        }
    }
}