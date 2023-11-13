import org.apache.lucene.analysis.cn.smart.SmartChineseAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SearchEngine {

    public static void main(String[] args) {
        try {
            String indexDir = "/java/SearchEngine/Index"; // 确保这是你索引存储的路径
            String queryStr = "天际线2，道路"; // 查询字符串

            // 打开索引
            Directory dir = FSDirectory.open(Paths.get(indexDir));
            IndexReader reader = DirectoryReader.open(dir);
            IndexSearcher searcher = new IndexSearcher(reader);

            // 使用SmartChineseAnalyzer进行查询解析
            SmartChineseAnalyzer analyzer = new SmartChineseAnalyzer();
            QueryParser parser = new QueryParser("content", analyzer);
            Query query = parser.parse(queryStr);

            // 执行查询
            TopDocs hits = searcher.search(query, 10); // 获取前10个结果（可以自定前几个结果）

            // 输出结果
            for (ScoreDoc sd : hits.scoreDocs) {
                Document d = searcher.doc(sd.doc);
                String detail = d.get("detail");

                // 解析时间
                Pattern pattern = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
                Matcher matcher = pattern.matcher(detail);
                if (matcher.find()) {
                    String date = matcher.group(0);
                    // 输出结果及其日期
                    System.out.println(date + ": " + d.get("title"));
                }
            }
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
            // 这里可以处理异常
        }
    }
}
