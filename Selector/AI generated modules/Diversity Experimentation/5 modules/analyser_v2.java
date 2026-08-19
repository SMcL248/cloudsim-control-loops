package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;
import java.util.Arrays;

// Strategy: adaptive z-score classifier.
// Instead of fixed cutoffs, thresholds are derived from the observed batch
// itself - mean and standard deviation of the current metrics array. An
// entry is flagged only if it deviates meaningfully from its peers in this
// tick, so the same raw utilisation value can be judged differently
// depending on what the rest of the fleet is doing.
public class analyser_v2 implements Analyser<double[], LoadState[]> {

    private static final String MODULE_NAME = "analyser_v2";

    private static final int INPUT_GUID = 1300;
    private static final int OUTPUT_GUID = 2300;
    private static final String INPUT_SEMANTIC = "vm-cpu-utilization-instant";
    private static final String OUTPUT_SEMANTIC = "vm-load-zscore-classification";

    private static final double Z_THRESHOLD = 1.0;
    private static final double MIN_STD_DEV = 1e-6;

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        int n = metrics.length;
        LoadState[] result = new LoadState[n];

        if (n == 0) {
            return result;
        }

        double sum = 0.0;
        int validCount = 0;
        for (double v : metrics) {
            if (!Double.isNaN(v)) {
                sum += v;
                validCount++;
            }
        }

        if (validCount == 0) {
            Arrays.fill(result, LoadState.BALANCED);
            return result;
        }

        double mean = sum / validCount;

        double sqDiffSum = 0.0;
        for (double v : metrics) {
            if (!Double.isNaN(v)) {
                double diff = v - mean;
                sqDiffSum += diff * diff;
            }
        }
        double stdDev = Math.sqrt(sqDiffSum / validCount);

        if (stdDev < MIN_STD_DEV) {
            Arrays.fill(result, LoadState.BALANCED);
            Log.printlnConcat(readSpace.getNow(), ": [" + MODULE_NAME + "] near-uniform distribution (stdDev~0), all BALANCED");
            return result;
        }

        for (int i = 0; i < n; i++) {
            double v = metrics[i];
            if (Double.isNaN(v)) {
                result[i] = LoadState.BALANCED;
                continue;
            }
            double z = (v - mean) / stdDev;
            if (z >= Z_THRESHOLD) {
                result[i] = LoadState.OVERLOADED;
            } else if (z <= -Z_THRESHOLD) {
                result[i] = LoadState.UNDERLOADED;
            } else {
                result[i] = LoadState.BALANCED;
            }
        }

        Log.printlnConcat(readSpace.getNow(), ": [" + MODULE_NAME + "] classified ", n,
                " VMs; mean=", mean, " stdDev=", stdDev, " zThreshold=", Z_THRESHOLD);

        return result;
    }

    @Override
    public String inputSemantic() {
        return INPUT_SEMANTIC;
    }

    @Override
    public String outputSemantic() {
        return OUTPUT_SEMANTIC;
    }

    @Override
    public int inputGuid() {
        return INPUT_GUID;
    }

    @Override
    public int outputGuid() {
        return OUTPUT_GUID;
    }
}
