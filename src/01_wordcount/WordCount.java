import java.io.Serializable;
import java.util.Arrays;
import java.util.Comparator;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;

import scala.Tuple2;

/**
 * Word count over <base>/wordcount, written to <base>/output/wordcount.
 *
 *     ./run.sh 01_wordcount
 */
public class WordCount {
    // Serializable, because the executors compare the pairs.
    static final Comparator<Tuple2<String, Integer>> BY_COUNT =
        (Comparator<Tuple2<String, Integer>> & Serializable) (a, b) -> Integer.compare(a._2(), b._2());

    public static void main(String[] args) throws Exception {
        String base = args.length > 0 ? args[0].replaceAll("/+$", "") : "/data";

        JavaSparkContext sc = new JavaSparkContext(new SparkConf().setAppName("wordcount " + base));

        // Nothing is read yet: transformations only build the lineage.
        JavaRDD<String> lines = sc.textFile(base + "/wordcount");

        // TODO start: lowercase, split on "[^a-z']+", drop empty words, count per word.
        JavaPairRDD<String, Integer> counts = null;  // word -> number of times it occurs

        // TODO end

        // top is an action: it starts a job and returns the 20 highest counts to the driver.
        for (Tuple2<String, Integer> t : counts.top(20, BY_COUNT)) {
            System.out.println(t._1() + " " + t._2());
        }

        String out = base + "/output/wordcount";
        Output.clear(sc, out);
        // TODO start: write all counts to out as "word,count" lines.

        // TODO end
        System.out.println("wrote " + out);

        sc.stop();
    }
}
