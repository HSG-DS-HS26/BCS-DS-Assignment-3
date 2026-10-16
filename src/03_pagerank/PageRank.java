import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.apache.spark.HashPartitioner;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaSparkContext;

import scala.Tuple2;

/**
 * PageRank over the web shop crawl in <base>/pagerank, written to <base>/output/pagerank.
 *
 *     ./run.sh 03_pagerank       # 10 iterations
 *     ./run.sh 03_pagerank 3     # 3 iterations
 */
public class PageRank {
    static final double DAMPING = 0.85;

    // Serializable, because the executors compare the pairs.
    static final Comparator<Tuple2<String, Double>> BY_RANK =
        (Comparator<Tuple2<String, Double>> & Serializable) (a, b) -> Double.compare(a._2(), b._2());

    public static void main(String[] args) throws Exception {
        String base = args.length > 0 ? args[0].replaceAll("/+$", "") : "/data";
        int iterations = args.length > 1 ? Integer.parseInt(args[1]) : 10;

        JavaSparkContext sc = new JavaSparkContext(new SparkConf().setAppName("pagerank " + base));

        // (file path, HTML) per page.
        JavaPairRDD<String, String> pages = sc.wholeTextFiles(base + "/pagerank/*/*.html", sc.defaultParallelism());

        // TODO start: page -> its links, with Html.page and Html.links (src/common/Html.java). Partition with
        // new HashPartitioner(sc.defaultParallelism()), setName("links") and cache.
        JavaPairRDD<String, List<String>> links = null;  // page -> the pages it links to

        // TODO end

        // TODO start: page -> 1.0 for every page.
        JavaPairRDD<String, Double> ranks = null;  // page -> its current rank

        // TODO end

        for (int i = 0; i < iterations; i++) {
            // TODO start: join links with ranks; each page sends rank / number of links to each
            // link, and 0.0 to itself so it stays in ranks. Sum per page with the partitioner
            // of links. New rank = (1 - DAMPING) + DAMPING * sum.

            // TODO end
        }

        // top is an action: it runs all iterations and returns the 10 highest ranks to the driver.
        System.out.println("rank        page");
        for (Tuple2<String, Double> t : ranks.top(10, BY_RANK)) {
            System.out.printf("%-10.4f  %s%n", t._2(), t._1());
        }

        String out = base + "/output/pagerank";
        Output.clear(sc, out);
        ranks.map(t -> t._1() + "," + t._2()).saveAsTextFile(out);
        System.out.println("wrote " + out);

        sc.stop();
    }
}
