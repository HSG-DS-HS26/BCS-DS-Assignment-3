import java.io.IOException;

import org.apache.hadoop.fs.Path;
import org.apache.spark.api.java.JavaSparkContext;

/** RDD writes (saveAsTextFile) fail if the target exists, so jobs clear it first. */
public final class Output {
    private Output() {}

    /** Deletes dir and everything under it, on whichever storage system its path names. */
    public static void clear(JavaSparkContext sc, String dir) throws IOException {
        Path path = new Path(dir);
        path.getFileSystem(sc.hadoopConfiguration()).delete(path, true);
    }
}
