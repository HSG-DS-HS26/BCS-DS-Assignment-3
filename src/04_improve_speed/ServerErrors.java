import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaSparkContext;

import scala.Tuple2;

/**
 * The 10 paths with the most server errors (status >= 500) in the access logs in <base>/logs.
 * Field 6 of a line is the path, field 8 the status:
 *
 *     10.0.1.128 - - [01/Mar/2025:00:00:00 +0000] "GET /products/145 HTTP/1.1" 200 6929
 *
 *     ./run.sh 04_improve_speed
 */
public class ServerErrors {
    public static void main(String[] args) {
        String base = args.length > 0 ? args[0].replaceAll("/+$", "") : "/data";

        JavaSparkContext sc = new JavaSparkContext(new SparkConf().setAppName("server-errors " + base));

        long start = System.nanoTime();

        JavaPairRDD<String, Integer> errorsPerPath = sc.textFile(base + "/logs")
            .mapToPair(line -> new Tuple2<>(path(line), line))           
            .groupByKey()                                                
            .mapValues(lines -> {                                        
                int errors = 0;
                for (String line : lines) {
                    if (status(line) >= 500) {
                        errors++;
                    }
                }
                return errors;
            })
            .filter(t -> t._2() > 0);                                     

        System.out.println("errors  path");
        for (Tuple2<Integer, String> t : errorsPerPath.mapToPair(Tuple2::swap).sortByKey(false).take(10)) {
            System.out.printf("%6d  %s%n", t._1(), t._2());
        }
        System.out.printf("took %.1f s%n", (System.nanoTime() - start) / 1e9);

        sc.stop();
    }

    static String path(String line) {
        return line.split(" ")[6];
    }

    static int status(String line) {
        return Integer.parseInt(line.split(" ")[8]);
    }
}
