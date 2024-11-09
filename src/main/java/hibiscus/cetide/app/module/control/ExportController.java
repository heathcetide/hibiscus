//package hibiscus.cetide.app.module.control;
//
//import org.springframework.core.io.ByteArrayResource;
//import org.springframework.http.HttpHeaders;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.MediaType;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RequestParam;
//import org.springframework.web.bind.annotation.RestController;
//
//import javax.annotation.Resource;
//import javax.swing.text.Document;
//import java.io.ByteArrayOutputStream;
//import java.io.IOException;
//import java.nio.charset.StandardCharsets;
//
//@RestController
//@RequestMapping("/export")
//public class ExportController {
//
//    @GetMapping
//    public ResponseEntity<Resource> exportFile(@RequestParam String type) {
//        String filename = "export." + type;
//        ByteArrayResource resource = null;
//
//        try {
//            byte[] data = generateFileData(type);
//            resource = new ByteArrayResource(data);
//        } catch (IOException e) {
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
//        }
//
//        MediaType mediaType = getMediaTypeForType(type);
//        return ResponseEntity.ok()
//                .contentType(mediaType)
//                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
//                .body(resource);
//    }
//
//    private byte[] generateFileData(String type) throws IOException {
//        // 这里编写生成对应类型文件的逻辑，可以使用具体的库
//        switch (type) {
//            case "markdown":
//                return "这是一个导出的Markdown文件".getBytes(StandardCharsets.UTF_8);
//            case "pdf":
//                // 使用 iText 或其他库生成 PDF 文件
//                return generatePdfFile();
//            case "html":
//                return "<html><body><h1>导出的HTML文件</h1></body></html>".getBytes(StandardCharsets.UTF_8);
//            case "word":
//                // 使用 Apache POI 生成 Word 文件
//                return generateWordFile();
//            default:
//                throw new IllegalArgumentException("不支持的文件类型: " + type);
//        }
//    }
//
//    private MediaType getMediaTypeForType(String type) {
//        switch (type) {
//            case "markdown":
//                return MediaType.TEXT_MARKDOWN;
//            case "pdf":
//                return MediaType.APPLICATION_PDF;
//            case "html":
//                return MediaType.TEXT_HTML;
//            case "word":
//                return MediaType.valueOf("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
//            default:
//                throw new IllegalArgumentException("不支持的文件类型: " + type);
//        }
//    }
//
//    private byte[] generatePdfFile() {
//        // 使用 iText 或其他库生成 PDF 文件的逻辑
//        // 示例代码，实际需根据需要生成具体内容
//        ByteArrayOutputStream out = new ByteArrayOutputStream();
//        try (PdfWriter writer = new PdfWriter(out);
//             PdfDocument pdfDoc = new PdfDocument(writer)) {
//            Document document = new Document(pdfDoc);
//            document.add(new Paragraph("这是导出的 PDF 文件"));
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        return out.toByteArray();
//    }
//
//    private byte[] generateWordFile() {
//        // 使用 Apache POI 生成 Word 文件的逻辑
//        XWPFDocument document = new XWPFDocument();
//        ByteArrayOutputStream out = new ByteArrayOutputStream();
//        try {
//            XWPFParagraph paragraph = document.createParagraph();
//            XWPFRun run = paragraph.createRun();
//            run.setText("这是一个导出的 Word 文件");
//            document.write(out);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        return out.toByteArray();
//    }
//}
