import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.spark.SparkContext;
import org.apache.spark.scheduler.SparkListener;
import org.apache.spark.scheduler.SparkListenerExecutorAdded;
import org.apache.spark.scheduler.SparkListenerStageCompleted;
import org.apache.spark.scheduler.SparkListenerTaskEnd;
import org.apache.spark.scheduler.StageInfo;

/**
 * ./run.sh ... --problematic-system: after the first stage that writes shuffle output, crashes
 * the worker holding the most shuffle output: shuts down its DataNode and kills its executor,
 * losing its shuffle files. The DataNode restarts after about 2 minutes
 * (.devcontainer/start-worker.sh); the executor does not.
 */
public class ProblematicSystem extends SparkListener {
    private final Map<String, String> hostOfExecutor = new ConcurrentHashMap<>();
    private final Map<String, Long> shuffleBytesOfHost = new ConcurrentHashMap<>();
    private volatile boolean crashed = false;

    @Override
    public void onExecutorAdded(SparkListenerExecutorAdded e) {
        hostOfExecutor.put(e.executorId(), e.executorInfo().executorHost());
    }

    @Override
    public void onTaskEnd(SparkListenerTaskEnd e) {
        if (e.taskMetrics() != null) {
            shuffleBytesOfHost.merge(e.taskInfo().host(), e.taskMetrics().shuffleWriteMetrics().bytesWritten(), Long::sum);
        }
    }

    @Override
    public void onStageCompleted(SparkListenerStageCompleted e) {
        StageInfo stage = e.stageInfo();
        if (crashed || stage.failureReason().isDefined()
                || stage.taskMetrics().shuffleWriteMetrics().bytesWritten() == 0) {
            return;
        }
        crashed = true;
        String host = shuffleBytesOfHost.entrySet().stream()
            .max(Map.Entry.comparingByValue()).orElseThrow().getKey();
        // Not on the listener thread: killing an executor waits for the scheduler.
        new Thread(() -> crash(host, stage.stageId()), "problematic-system").start();
    }

    private void crash(String host, int afterStage) {
        System.err.printf("%n*** problematic system: %s crashes after stage %d ***%n%n", host, afterStage);
        try {
            new ProcessBuilder("hdfs", "dfsadmin", "-shutdownDatanode", host + ":9867")   // the DataNode's IPC port
                .redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .start().waitFor();
        } catch (Exception ex) {
            System.err.println("could not shut down the DataNode of " + host + ": " + ex);
        }
        SparkContext sc = SparkContext.getOrCreate();
        hostOfExecutor.forEach((id, h) -> {
            if (h.equals(host)) {
                sc.killExecutor(id);
            }
        });
    }
}
