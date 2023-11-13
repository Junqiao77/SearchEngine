import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.URL;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class WebCrawler {

    private Queue<String> queue = new LinkedList<>();
    private Set<String> seenUrls = new HashSet<>();
    private static final int MAX_URLS = 10000;//设置最大的爬取数量
    private static final String DB_URL = "jdbc:mysql://localhost/数据库名?user=用户名&password=密码";//设置你的数据库的相关信息
    private static final String INSERT_SQL = "INSERT INTO crawled_url_rank (url, title, description, keywords, detail, content, timestamp) VALUES (?, ?, ?, ?, ?, ?, ?);";//设置你数据库中相应的字段，有多少字段就有多少个问号
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.3";
    private static final int CRAWL_DELAY_MS = 500;//设置爬取延迟单位毫秒
    private static final String COOKIE = "Your Cookie String Here";// 如果需要cookie，填写你的cookie

    public WebCrawler(String seedUrl) {
        if (isValidUrl(seedUrl)) {
            queue.offer(seedUrl);
            seenUrls.add(seedUrl);
        }
    }

    private static boolean isValidUrl(String url) {
        try {
            new URL(url).toURI();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean shouldCrawlUrl(String url) {
        return url.matches("https://www.gamersky.com/z/.*/") ||
                url.matches("https://www.gamersky.com/z/.*/news/") ||
                url.matches("https://www.gamersky.com/z/.*/handbook/") ||
                url.matches("https://www.gamersky.com/news/.*") ||
                url.matches("https://www.gamersky.com/handbook/.*");
    }  // 根据我自己的需求，做了选择特定的爬取对应的URL网址，可以自己修改


    private Connection connect() {
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(DB_URL);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return conn;
    }

    private void insertData(String url, String title, String description, String keywords, String detail, String content, long timestamp) {
        try (Connection conn = this.connect();
             PreparedStatement pstmt = conn.prepareStatement(INSERT_SQL)) {
            pstmt.setString(1, url);
            pstmt.setString(2, title);
            pstmt.setString(3, description);
            pstmt.setString(4, keywords);
            pstmt.setString(5, detail); 
            pstmt.setString(6, content);
            pstmt.setLong(7, timestamp);
            pstmt.executeUpdate();
            System.out.println("已保存到数据库: " + url);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    } // 写入数据库的操作

    public void startCrawl() {
        int countUrls = 0;
        while (!queue.isEmpty() && countUrls < MAX_URLS) {
            String currentUrl = queue.poll();
            System.out.println("正在爬取URL: " + currentUrl);
            try {
                Document doc = Jsoup.connect(currentUrl).userAgent(USER_AGENT).cookie("Cookie", COOKIE).get();

                String title = doc.title();
                String description = doc.select("meta[name=description]").attr("content");
                String keywords = doc.select("meta[name=keywords]").attr("content");

                Element detailElement = doc.selectFirst(".Mid2L_tit .detail");
                String detail = detailElement != null ? detailElement.text() : "";

                Elements contentElements = doc.select(".Mid2L_con p, .Mid2L_con .GsImageLabel");
                StringBuilder contentBuilder = new StringBuilder();
                for (Element element : contentElements) {
                    contentBuilder.append(element.text()).append("\n");
                }
                String content = contentBuilder.toString().trim();
                long timestamp = System.currentTimeMillis();

                insertData(currentUrl, title, description, keywords, detail, content, timestamp);

                Elements links = doc.select("a[href]");
                for (Element link : links) {
                    String absHref = link.attr("abs:href");
                    if (!seenUrls.contains(absHref) && isValidUrl(absHref) && shouldCrawlUrl(absHref)) {
                        seenUrls.add(absHref);
                        queue.offer(absHref);
                    }
                }

                countUrls++;
                Thread.sleep(CRAWL_DELAY_MS);
            } catch (Exception e) {
                System.err.println("处理URL时出错: " + currentUrl + "; 错误信息: " + e.getMessage());
            }
        }
    } // 爬取对应的网站的对应的字段，可能网站会改变，也可以爬取你需要的字段

    public static void main(String[] args) {
        WebCrawler crawler = new WebCrawler("https://www.gamersky.com/");
        crawler.startCrawl();
    }
}
