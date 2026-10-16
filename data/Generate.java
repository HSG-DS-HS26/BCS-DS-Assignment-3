import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.SplittableRandom;

/**
 * Generates the data of Tundrell's web shop for tasks 2 and 3. A fixed seed makes every
 * run produce the same files.
 *
 *     data/pagerank/               a crawl of the web shop, one HTML file per page
 *     data/join/customers.csv      customer_id,name,canton                 50,000 customers
 *     data/join/orders.csv         order_id,customer_id,order_date,amount  500,000 orders of 2025
 *
 * Customers live in a canton with a probability proportional to its population. Some
 * customers order much more often than others, most orders are small and a few are large,
 * and November and December are busier than the rest of the year.
 *
 *     java data/Generate.java      # run from the repository root, no Spark needed
 */
public class Generate {
    static final long SEED = 42;
    static final int CUSTOMERS = 50_000;

    // Cantons and their population in thousands (Swiss Federal Statistical Office, 2023, rounded).
    static final String[] CANTONS = {"ZH", "BE", "VD", "AG", "SG", "GE", "LU", "TI", "VS", "FR", "BL", "TG", "SO",
        "GR", "BS", "NE", "SZ", "ZG", "SH", "JU", "AR", "NW", "GL", "OW", "UR", "AI"};
    static final double[] POPULATION = {1605, 1063, 840, 726, 530, 516, 425, 357, 356, 334, 296, 289, 283,
        202, 197, 177, 165, 131, 85, 74, 56, 44, 41, 39, 37, 16};

    static final String[] FIRST_NAMES = {"Anna", "Laura", "Sarah", "Lea", "Julia", "Sophie", "Mia", "Elena", "Chiara",
        "Camille", "Marie", "Nina", "Lara", "Emma", "Noemi", "Luca", "David", "Daniel", "Thomas", "Simon", "Lukas",
        "Marco", "Nico", "Jonas", "Michael", "Samuel", "Pascal", "Matteo", "Julien", "Patrick", "Reto", "Urs",
        "Beat", "Ursula", "Ruth", "Peter", "Hans", "Martin", "Andrea", "Stefan"};
    static final String[] LAST_NAMES = {"Müller", "Meier", "Schmid", "Keller", "Weber", "Huber", "Schneider", "Meyer",
        "Steiner", "Fischer", "Gerber", "Brunner", "Baumann", "Frei", "Zimmermann", "Moser", "Widmer", "Wyss",
        "Graf", "Roth", "Suter", "Bühler", "Bachmann", "Kunz", "Lüthi", "Rossi", "Bernasconi", "Ferrari", "Favre",
        "Rochat", "Bonvin", "Dubois", "Morand", "Fontana", "Caflisch", "Arnold", "Bucher", "Kaufmann", "Lehmann", "Egli"};

    public static void main(String[] args) throws IOException {
        SplittableRandom random = new SplittableRandom(SEED);

        // Customers, and how often each of them orders (log-normal: a few order very often).
        String[] customers = new String[CUSTOMERS];
        double[] activity = new double[CUSTOMERS];
        double[] cantonWeights = cumulative(POPULATION);
        for (int id = 0; id < CUSTOMERS; id++) {
            String name = FIRST_NAMES[random.nextInt(FIRST_NAMES.length)] + " " + LAST_NAMES[random.nextInt(LAST_NAMES.length)];
            String canton = CANTONS[pick(cantonWeights, random)];
            customers[id] = id + "," + name + "," + canton;
            activity[id] = Math.exp(random.nextGaussian());
        }
        double[] customerWeights = cumulative(activity);

        writeCustomers(Path.of("data/join/customers.csv"), customers);

        writeOrders(Path.of("data/join/orders.csv"), orders(500_000, customerWeights, random), 0, 1);
        System.out.println("wrote data/join");

        // Own random, so the files above stay the same whatever happens here.
        writePages(Path.of("data/pagerank"), new SplittableRandom(SEED + 1));
        System.out.println("wrote data/pagerank");
    }

    static final String[] CATEGORIES = {"books", "games", "garden", "kitchen", "music", "sports", "toys", "tools"};
    static final int PRODUCTS = 1_000;
    static final int GUIDES = 3_000;
    static final String MENU = "<nav><a href=\"/\">Tundrell</a> <a href=\"/search\">Search</a> <a href=\"/account\">Account</a>"
        + " <a href=\"/cart\">Cart</a> <a href=\"/checkout\">Checkout</a></nav>\n";
    static final String FOOTER = "<footer><a href=\"https://www.instagram.com/tundrell\">Instagram</a>"
        + " <a href=\"https://www.youtube.com/@tundrell\">YouTube</a></footer>\n";

    /**
     * A crawl of the web shop: one HTML file per category, product and guide page, at the page's
     * path (data/pagerank/products/17.html is /products/17). Some products and guides are much
     * more popular than others and get most of the links.
     */
    static void writePages(Path dir, SplittableRandom random) throws IOException {
        if (Files.exists(dir)) {
            try (var paths = Files.walk(dir)) {
                for (Path p : paths.sorted(Comparator.reverseOrder()).toList()) Files.delete(p);
            }
        }
        int[] category = new int[PRODUCTS];
        double[] productWeights = new double[PRODUCTS];
        for (int p = 0; p < PRODUCTS; p++) {
            category[p] = random.nextInt(CATEGORIES.length);
            productWeights[p] = Math.exp(1.5 * random.nextGaussian());
        }
        productWeights = cumulative(productWeights);
        double[] guideWeights = new double[GUIDES];
        for (int g = 0; g < GUIDES; g++) guideWeights[g] = Math.exp(1.5 * random.nextGaussian());
        guideWeights = cumulative(guideWeights);

        for (int c = 0; c < CATEGORIES.length; c++) {
            List<String> links = new ArrayList<>();
            for (int p = 0; p < PRODUCTS; p++) if (category[p] == c) links.add("/products/" + p);
            writePage(dir, "/category/" + c, CATEGORIES[c], "All " + CATEGORIES[c] + " in our shop.", links);
        }
        for (int p = 0; p < PRODUCTS; p++) {
            List<String> links = new ArrayList<>();
            links.add("/category/" + category[p]);
            for (int n = 5 + random.nextInt(26); n > 0; n--) {
                int other = pick(productWeights, random);
                if (other != p) links.add("/products/" + other);   // the same product can come twice
            }
            for (int n = random.nextInt(4); n > 0; n--) links.add("/guides/" + pick(guideWeights, random));
            writePage(dir, "/products/" + p, CATEGORIES[category[p]] + " product " + p,
                "Product " + p + ". Customers who looked at it also looked at:", links);
        }
        for (int g = 0; g < GUIDES; g++) {
            List<String> links = new ArrayList<>();
            // About 1 in 50 guides is a short note without links to products or other guides.
            int products = random.nextInt(50) == 0 ? 0 : 5 + random.nextInt(41);
            for (int n = products; n > 0; n--) links.add("/products/" + pick(productWeights, random));
            for (int n = products == 0 ? 0 : random.nextInt(4); n > 0; n--) {
                int other = pick(guideWeights, random);
                if (other != g) links.add("/guides/" + other);
            }
            writePage(dir, "/guides/" + g, "Guide " + g, "Buying guide " + g + ". The products it recommends:", links);
        }
    }

    static void writePage(Path dir, String page, String title, String text, List<String> links) throws IOException {
        StringBuilder html = new StringBuilder();
        html.append("<!doctype html>\n<html>\n<head><title>").append(title).append(" | Tundrell</title></head>\n<body>\n");
        html.append(MENU).append("<h1>").append(title).append("</h1>\n<p>").append(text).append("</p>\n<ul>\n");
        for (String link : links) html.append("<li><a href=\"").append(link).append("\">").append(link).append("</a></li>\n");
        html.append("</ul>\n").append(FOOTER).append("</body>\n</html>\n");
        Path file = dir.resolve(page.substring(1) + ".html");
        Files.createDirectories(file.getParent());
        Files.writeString(file, html);
    }

    /** n orders as {customer, day of 2025, amount in cents}, sorted by day like a real export. */
    static long[][] orders(int n, double[] customerWeights, SplittableRandom random) {
        double[] dayWeights = new double[365];
        for (int day = 0; day < 365; day++) {
            int month = LocalDate.of(2025, 1, 1).plusDays(day).getMonthValue();
            dayWeights[day] = month == 12 ? 1.6 : month == 11 ? 1.3 : 1.0;
        }
        dayWeights = cumulative(dayWeights);

        long[][] orders = new long[n][];
        for (int i = 0; i < n; i++) {
            random.nextDouble();   // keeps the random sequence, and so the files, as before
            long customer = pick(customerWeights, random);
            long day = pick(dayWeights, random);
            long cents = Math.max(100, Math.round(4000 * Math.exp(random.nextGaussian())));   // median 40 CHF
            orders[i] = new long[] {customer, day, cents};
        }
        Arrays.sort(orders, (a, b) -> Long.compare(a[1], b[1]));
        return orders;
    }

    static void writeCustomers(Path path, String[] customers) throws IOException {
        Files.createDirectories(path.getParent());
        Files.write(path, Arrays.asList(customers));
    }

    /** Writes every parts-th order, starting at part, so the parts cover the whole year each. */
    static void writeOrders(Path path, long[][] orders, int part, int parts) throws IOException {
        LocalDate first = LocalDate.of(2025, 1, 1);
        try (BufferedWriter out = Files.newBufferedWriter(path)) {
            for (int id = part; id < orders.length; id += parts) {
                long[] o = orders[id];
                out.write(String.format(Locale.ROOT, "%d,%d,%s,%d.%02d%n", id, o[0], first.plusDays(o[1]), o[2] / 100, o[2] % 100));
            }
        }
    }

    static double[] cumulative(double[] weights) {
        double[] sums = new double[weights.length];
        double sum = 0;
        for (int i = 0; i < weights.length; i++) {
            sum += weights[i];
            sums[i] = sum;
        }
        return sums;
    }

    /** An index i with probability weights[i] / sum, given the cumulative weights. */
    static int pick(double[] cumulative, SplittableRandom random) {
        int i = Arrays.binarySearch(cumulative, random.nextDouble() * cumulative[cumulative.length - 1]);
        return Math.min(i >= 0 ? i : -i - 1, cumulative.length - 1);
    }
}
