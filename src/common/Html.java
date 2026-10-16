import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Reads the pages of the web shop crawl in <base>/pagerank. */
public final class Html {
    private Html() {}

    private static final Pattern HREF = Pattern.compile("href=\"(/(?:products|category|guides)/[^\"]*)\"");

    /** The page of a crawled file: ".../pagerank/products/17.html" is "/products/17". */
    public static String page(String path) {
        return path.substring(path.lastIndexOf("/pagerank/") + "/pagerank".length(), path.length() - ".html".length());
    }

    /** The links to products, categories and guides, without duplicates. Menu and external links are left out. */
    public static List<String> links(String html) {
        List<String> links = new ArrayList<>();
        Matcher m = HREF.matcher(html);
        while (m.find()) {
            if (!links.contains(m.group(1))) {
                links.add(m.group(1));
            }
        }
        return links;
    }
}
