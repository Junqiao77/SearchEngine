import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;


import java.net.URL;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.*;


import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class WebCrawler {

    private Queue<String> queue = new LinkedList<>();
    private Set<String> seenUrls = new HashSet<>();
    private static final int MAX_URLS = 10000;
    private static final String DB_URL = "jdbc:mysql://localhost/topic_search?user=root&password=mysql088925";
    private static final String INSERT_SQL = "INSERT INTO crawled_url_rank (url, title, description,keywords, publish_date, content, timestamp) VALUES (?, ?, ?, ?, ?, ?, ?);";
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.3";
    private static final int CRAWL_DELAY_MS = 500;
    private static final String COOKIE = ".ASPXAUTH=B2572711CDD89A033362C27F2C49D1247D87BC4C707DE16F8ECF34738E590BC0097AD3E547AA23BD34416AF68458BE808B16FF038EE99449092A44BE9ECE21139B8D2BB65762573E17640935CB309AAFA93F6B3D5E6A9BD6F86B0BEB43E429653F62E6921575DEA94ABB922853B34D769FF0F3754714B3BDE0C3F8915604701CD5845067A9B79A57D699A31ABFFD7684F8A34BCB39005DA8E9F36F9B8C48DE7AFF4F6DC3BA9C6CF540B66A156D1EB181447B59C2BF036CE3713FA1216DBBD3F5025ED8115CD52C06D555521BC0D6ECD90A8A8E09071BDE67C7DAC72ADB97098D2F06C74B3CE605B93F2487CBD78812132B7563BB4CAD263398EB2B883A5850D86EBDDA53DCA4C8BC0CB00A6F340CBF36BFFF9421811282A6B38C9A5E1BE404186804F318A81F4057407F428A24E09B1D0286D3B47CB2348E386EB3AEDE03949C0D5C34C835738C8BEEFE4C39603E96490897BA913B8A5051A9A6C2C852CA35B69ED46C75B6B78DBFAB8E009E0F223DDC9BA95ADB2612B76F017BF597F485320FF4098018B96540789B15622DA62682E53F4051B61D84D1762216CE4B4FC8257E5CBCEA93A90F6E3C84B63395B4FEBF0CD761A0D48C3CB4FF96E0A84B9EBE9443F30A980067C810BC577890AE1C2C91D2570C0A8F19A8E001C6F3DCC2F8CC1A97B8B492E06B0A1AFF99CB19D0EEB63BB4ED782FC2; UserCookie=%7B%22status%22%3A%22ok%22%2C%22username%22%3A%22Arbour%22%2C%22usergroup%22%3A4%2C%22email%22%3A%22%22%2C%22userid%22%3A8256675%2C%22logintimes%22%3A3%2C%22phonenumber%22%3A%2218623265087%22%2C%22phonenumberconfirmed%22%3Atrue%2C%22emailconfirmed%22%3Afalse%2C%22userface%22%3A%22https%3A%2F%2Fimage.gamersky.com%2Favatar%2Foriginal%2Fgame%2Fgame287.jpg%22%2C%22modifitime%22%3A%222023-11-06T23%3A18%3A58.7467493%2B08%3A00%22%2C%22token%22%3A%22ec736fed728da2bf7b5ef71b6faa17ff%22%2C%22guId%22%3A%222b263cd0-2ac5-42bd-9526-87d9b880d18a%22%2C%22idcard%22%3A0%2C%22qqClass%22%3A%22no%22%2C%22sinaClass%22%3A%22no%22%2C%22weixinClass%22%3A%22no%22%2C%22emailClass%22%3A%22no%22%2C%22phoneClass%22%3A%22ok%22%2C%22articleUrl%22%3A%22http%3A%2F%2Fi.gamersky.com%2Farticle%2F8256675%22%2C%22iscolumn%22%3Afalse%2C%22homeurl%22%3A%22http%3A%2F%2Fi.gamersky.com%2Fu%2F8256675%2F%22%2C%22isUpdateImage%22%3A1%7D; Search=0; ADback20170903=1; Hm_lvt_dcb5060fba0123ff56d253331f28db6a=1699441480,1699454169,1699490031,1699756304; pc_8256675=true; Hm_lpvt_dcb5060fba0123ff56d253331f28db6a=1699772042";

    private PageRankManager pageRankManager = new PageRankManager();

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

    private Connection connect() {
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(DB_URL);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return conn;
    }

    private void insertData(String url, String title, String description,String keywords, String publishDate, String content, long timestamp) {
        try (Connection conn = this.connect();
             PreparedStatement pstmt = conn.prepareStatement(INSERT_SQL)) {
            pstmt.setString(1, url);
            pstmt.setString(2, title);
            pstmt.setString(3, description);
            pstmt.setString(4, keywords);
            pstmt.setString(5, publishDate);
            pstmt.setString(6, content);
            pstmt.setLong(7, timestamp);
            pstmt.executeUpdate();
            System.out.println("已保存到数据库: " + url);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

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

                // 初始化 publish_date
                String publish_date = ""; // 将 publish_date 的声明移动到这里

                Element detailElement = doc.selectFirst(".Mid2L_tit .detail");
                if (detailElement != null) {
                    String detailText = detailElement.text();
                    String[] details = detailText.split(" 来源：");
                    publish_date = details[0].trim(); // 更新 publish_date 的值
                }

                Elements contentElements = doc.select(".Mid2L_con p, .Mid2L_con .GsImageLabel");
                StringBuilder contentBuilder = new StringBuilder();
                for (Element element : contentElements) {
                    contentBuilder.append(element.text()).append("\n");
                }
                String content = contentBuilder.toString().trim();
                long timestamp = System.currentTimeMillis();
                insertData(currentUrl, title, description, keywords, publish_date, content, timestamp);

                Elements links = doc.select("a[href]");
                Set<String> linkSet = new HashSet<>();
                for (Element link : links) {
                    String absHref = link.attr("abs:href");
                    if (!seenUrls.contains(absHref) && isValidUrl(absHref)) {
                        seenUrls.add(absHref);
                        queue.offer(absHref);
                        linkSet.add(absHref);
                    }
                }

                pageRankManager.addLinks(currentUrl, linkSet);
                countUrls++;
                Thread.sleep(CRAWL_DELAY_MS);
            } catch (Exception e) {
                System.err.println("处理URL时出错: " + currentUrl + "; 错误信息: " + e.getMessage());
            }
        }
        pageRankManager.calculatePageRanks(0.85, 20);
        updatePageRankInDB();
    }


    private void updatePageRankInDB() {
        String updateSql = "UPDATE crawled_url_rank SET pagerank = ? WHERE url = ?;";
        try (Connection conn = this.connect();
             PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
            for (Map.Entry<String, Double> entry : pageRankManager.getAllPageRanks().entrySet()) {
                pstmt.setDouble(1, entry.getValue());
                pstmt.setString(2, entry.getKey());
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            System.out.println("更新数据库时发生错误: " + e.getMessage());
        }
    }

    // PageRank管理器类
    class PageRankManager {
        private Map<String, Set<String>> linkGraph = new HashMap<>();
        private Map<String, Double> pageRanks = new HashMap<>();

        public void addLinks(String pageUrl, Set<String> links) {
            linkGraph.putIfAbsent(pageUrl, new HashSet<>());
            linkGraph.get(pageUrl).addAll(links);
            links.forEach(link -> pageRanks.putIfAbsent(link, 1.0));
        }

        public void calculatePageRanks(double dampingFactor, int iterations) {
            final double initialRank = 1.0 / (double) linkGraph.size();
            pageRanks.keySet().forEach(url -> pageRanks.put(url, initialRank));

            for (int i = 0; i < iterations; i++) {
                Map<String, Double> newRanks = new HashMap<>();

                double sinkRankSum = pageRanks.entrySet().stream()
                        .filter(entry -> {
                            Set<String> links = linkGraph.get(entry.getKey());
                            return links == null || links.isEmpty();
                        })
                        .mapToDouble(Map.Entry::getValue)
                        .sum();

                for (String page : pageRanks.keySet()) {
                    double sum = linkGraph.entrySet().stream()
                            .filter(entry -> entry.getValue().contains(page))
                            .mapToDouble(entry -> {
                                // 使用 getOrDefault 来防止 null 返回值
                                Double rank = pageRanks.getOrDefault(entry.getKey(), 0.0);
                                return rank / entry.getValue().size();
                            })
                            .sum();

                    double newRank = (1 - dampingFactor) / linkGraph.size() + dampingFactor * (sum + sinkRankSum / linkGraph.size());
                    newRanks.put(page, newRank);
                }
                pageRanks = newRanks;
            }
        }

        public Map<String, Double> getAllPageRanks() {
            return new HashMap<>(pageRanks);
        }
    }

    public static void main(String[] args) {
        String seedUrl = "https://www.gamersky.com/z/valorant/"; // 用你的起始URL替换
        if (isValidUrl(seedUrl)) {
            WebCrawler crawler = new WebCrawler(seedUrl);
            crawler.startCrawl();
        } else {
            System.err.println("提供的起始URL无效: " + seedUrl);
        }
    }
}
