package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;

/**
 * analyser_v2 - Host level, population z-score classifier.
 * Strategy: computes the mean and standard deviation of the observed power
 * readings across all hosts this cycle, then classifies each host by how
 * many standard deviations it sits from the fleet mean. Both boundaries are
 * derived from the live distribution rather than fixed constants.
 */
public class analyser_v2 implements Analyser<double[], LoadState[]> {

    private static final double Z_THRESHOLD = 1.0;

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        int n = metrics.length;
        LoadState[] result = new LoadState[n];

        double mean = mean(metrics);
        double stdDev = stdDev(metrics, mean);

        int overloadCount = 0;
        int underloadCount = 0;

        for (int i = 0; i < n; i++) {
            if (stdDev <= 0.0) {
                result[i] = LoadState.BALANCED;
                continue;
            }
            double z = (metrics[i] - mean) / stdDev;
            if (z > Z_THRESHOLD) {
                result[i] = LoadState.OVERLOADED;
                overloadCount++;
            } else if (z < -Z_THRESHOLD) {
                result[i] = LoadState.UNDERLOADED;
                underloadCount++;
            } else {
                result[i] = LoadState.BALANCED;
            }
        }

        Log.printlnConcat(readSpace.getNow(), ": [analyser_v2] classified ", n,
                " hosts by z-score (mean=", mean, ", stdDev=", stdDev,
                ", zThreshold=", Z_THRESHOLD, ") -> overloaded=", overloadCount,
                ", underloaded=", underloadCount, ", balanced=", (n - overloadCount - underloadCount));

        return result;
    }

    private double mean(double[] values) {
        if (values.length == 0) {
            return 0.0;
        }
        double sum = 0.0;
        for (double v : values) {
            sum += v;
        }
        return sum / values.length;
    }

    private double stdDev(double[] values, double mean) {
        if (values.length == 0) {
            return 0.0;
        }
        double sumSq = 0.0;
        for (double v : values) {
            double diff = v - mean;
            sumSq += diff * diff;
        }
        return Math.sqrt(sumSq / values.length);
    }

    @Override
    public String inputSemantic() {
        return "host-power-watts";
    }

    @Override
    public String outputSemantic() {
        return "host-loadstate-power";
    }

    @Override
    public int inputGuid() {
        return 1200;
    }

    @Override
    public int outputGuid() {
        return 2200;
    }
}
