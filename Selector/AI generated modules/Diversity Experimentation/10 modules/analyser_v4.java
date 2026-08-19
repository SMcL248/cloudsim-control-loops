package org.cloudbus.cloudsim.examples;

import java.util.Arrays;
import org.cloudbus.cloudsim.Log;

public class analyser_v4 implements Analyser<double[], LoadState[]> {

    private static final int INPUT_GUID = 1300;
    private static final int OUTPUT_GUID = 2300;
    private static final String INPUT_SEMANTIC = "vm-cpuUtilFraction-instantaneous";
    private static final String OUTPUT_SEMANTIC = "vm-loadState-quartileIqr";

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        int n = metrics.length;
        LoadState[] result = new LoadState[n];

        if (n == 0) {
            Log.printlnConcat(readSpace.getNow(), ": [analyser_v4] no VMs to classify.");
            return result;
        }

        double[] sorted = Arrays.copyOf(metrics, n);
        Arrays.sort(sorted);

        double q1 = percentile(sorted, 0.25);
        double q3 = percentile(sorted, 0.75);

        int overloadedCount = 0;
        int underloadedCount = 0;

        for (int i = 0; i < n; i++) {
            if (metrics[i] > q3) {
                result[i] = LoadState.OVERLOADED;
                overloadedCount++;
            } else if (metrics[i] < q1) {
                result[i] = LoadState.UNDERLOADED;
                underloadedCount++;
            } else {
                result[i] = LoadState.BALANCED;
            }
        }

        Log.printlnConcat(readSpace.getNow(), ": [analyser_v4] classified ", n,
                " VMs against interquartile range (q1=", q1, ", q3=", q3, "): ",
                overloadedCount, " overloaded, ", underloadedCount, " underloaded, ",
                (n - overloadedCount - underloadedCount), " balanced.");

        return result;
    }

    private double percentile(double[] sortedValues, double fraction) {
        int n = sortedValues.length;
        if (n == 1) {
            return sortedValues[0];
        }
        double position = fraction * (n - 1);
        int lowerIndex = (int) Math.floor(position);
        int upperIndex = (int) Math.ceil(position);
        if (lowerIndex == upperIndex) {
            return sortedValues[lowerIndex];
        }
        double weight = position - lowerIndex;
        return sortedValues[lowerIndex] * (1 - weight) + sortedValues[upperIndex] * weight;
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
