import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.cn.smart.SmartChineseAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.Paths;
import java.sql.*;

public class ImprovedIndexBuilder implements AutoCloseable {

    private IndexWriter writer;
    private Analyzer analyzer;

    public ImprovedIndexBuilder(String indexDir) throws IOException {
        Directory dir = FSDirectory.open(Paths.get(indexDir));
        analyzer = new SmartChineseAnalyzer();
        IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
        iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
        writer = new IndexWriter(dir, iwc);
    }

    public void indexDocument(String id, String url, String title, String description, String keywords, String detail, String content, long timestamp) throws IOException {
        Document doc = new Document();
        Field titleField = new StringField("title", title, Field.Store.YES);

        doc.add(new StringField("id", id, Field.Store.YES));
        doc.add(new StringField("url", url, Field.Store.YES));
        doc.add(titleField);
        doc.add(new StringField("description", description, Field.Store.YES));
        doc.add(new TextField("keywords", keywords, Field.Store.YES));
        doc.add(new TextField("detail", detail, Field.Store.YES));
        doc.add(new TextField("content", content, Field.Store.YES));
        doc.add(new NumericDocValuesField("timestamp", timestamp));
        // 用于范围查询的时间戳
        doc.add(new LongPoint("timestamp_range", timestamp));

        writer.addDocument(doc);
    }

    @Override
    public void close() throws IOException {
        writer.close();
    }

    public static void main(String[] args) {
        String indexDir = "/java/SearchEngine/Index";// 你的索引文件的地址
        String jdbcUrl = "jdbc:mysql://localhost:3306/your_data";// 你的数据库的URL
        String user = "root";// 数据库的用户名
        String password = "";// 数据库的密码
        String sqlQuery = "";// 数据库中存储的表的名称

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.out.println("未找到MySQL JDBC驱动.");
            e.printStackTrace();
            return;
        }

        try (ImprovedIndexBuilder builder = new ImprovedIndexBuilder(indexDir);
             Connection conn = DriverManager.getConnection(jdbcUrl, user, password);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sqlQuery)) {

            while (rs.next()) {
                String id = rs.getString("id");
                String url = rs.getString("url");
                String title = rs.getString("title");
                String content = rs.getString("content");
                String keywords = rs.getString("keywords");
                String detail = rs.getString("detail");
                String description = summarizeContent(content); // 使用内容摘要
                long timestamp = rs.getLong("timestamp");

                builder.indexDocument(id, url, title, description, keywords, detail, content, timestamp);
            }

            System.out.println("索引构建完成.");
        } catch (SQLException | IOException e) {
            System.out.println("发生错误.");
            e.printStackTrace();
        }
    }

    private static String summarizeContent(String content) {
        return content.length() > 100 ? content.substring(0, 100) + "..." : content;
    }
}
