package hibiscus.cetide;


import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpUtil;
import hibiscus.cetide.app.AppApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

@SpringBootTest(classes = AppApplication.class)
public class HutoolTest {


//    @Test
//    public void test() throws IOException {
//        String json = "{\n" +
//                "  \"current\": 1,\n" +
//                "  \"pageSize\": 8,\n" +
//                "  \"sortField\": \"createTime\",\n" +
//                "  \"sortOrder\": \"descend\",\n" +
//                "  \"category\": \"文章\",\n" +
//                "  \"tags\": [],\n" +
//                "  \"reviewStatus\": 1\n" +
//                "}";
//        String url = "https://api.code-nav.cn/api/post/search/page/vo";
//
//
//        String result2 = HttpRequest.post(url)
//                .body(json)
//                .execute().body();
//        String uu = "F:\\j4_8_6_10_cs\\src\\test\\java\\hibiscus\\cetide";
//        File file = new File(uu, "result.json");
//        file.createNewFile();
//        FileWriter fileWriter = new FileWriter(file);
//        fileWriter.write(result2);
//        System.out.println(result2);
//    }
//
//    @Test
//    public void test2() {
//        String result1= HttpUtil.get("localhost:8080/hello");
//        System.out.println(result1);
//        String url = "localhost:8080/hello";
//        String result2 = HttpRequest.get(url)
//                .execute().body();
//        System.out.println(result2);
//    }
}
