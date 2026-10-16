import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaSparkContext;

import scala.Tuple2;

/**
 * The canton report: orders and revenue per canton, from <base>/join.
 *
 *     ./run.sh 02_join
 *     ./run.sh 02_join hdfs
 */
public class Join {
    public static void main(String[] args) {
        String base = args.length > 0 ? args[0].replaceAll("/+$", "") : "/data";

        JavaSparkContext sc = new JavaSparkContext(new SparkConf().setAppName("join " + base));

        // TODO start: read orders.csv and customers.csv, parse with Order::fromCsv and
        // Customer::fromCsv (src/common/Order.java, src/common/Customer.java), key both by customer_id.
        JavaPairRDD<Integer, Double> orders = null;     // customer_id -> amount
        JavaPairRDD<Integer, String> customers = null;  // customer_id -> canton

        // TODO end

        // TODO start: join, re-key by canton with (1, amount), add up per canton, sort by canton.
        JavaPairRDD<String, Tuple2<Integer, Double>> revenue = null;  // canton -> (number of orders, revenue in CHF)

        // TODO end

        // The lineage: every RDD revenue depends on; indentation marks a shuffle.
        System.out.println(revenue.toDebugString());

        System.out.println("canton     orders        revenue");
        for (Tuple2<String, Tuple2<Integer, Double>> r : revenue.collect()) {
            System.out.printf("%-6s %10d %14.2f%n", r._1(), r._2()._1(), r._2()._2());
        }

        sc.stop();
    }
}
