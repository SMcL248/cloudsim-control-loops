package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;

/**
 * analyser_v5 - Cloudlet level, min-max range normaliser.
 * Strategy: rescales the observed progress rate of each cloudlet into a
 * 0..1 range using this cycle's own minimum and maximum, then applies fixed
 * cut points to the normalised value. This keeps the classification
 * relative to whatever spread of progress rates is currently observed,
 * without assuming a particular distribution shape (unlike the mean/stddev
 * or median/MAD approaches used elsewhere in this set).
 */
public class analyser_v5 implements Analyser<double[], LoadState[]> {

    private static final double LOW_CUT = 0.25;
    private static final double HIGH_CUT = 0.75;

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        int n = metrics.length;
        LoadState[] result = new LoadState[n];

        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (double v : metrics) {
            if (v < min) {
                min = v;
            }
            if (v > max) {
                max = v;
            }
        }
        double range = max - min;

        int overloadCount = 0;
        int underloadCount = 0;

        for (int i = 0; i < n; i++) {
            if (range <= 0.0) {
                result[i] = LoadState.BALANCED;
                continue;
            }
            double normalised = (metrics[i] - min) / range;
            // low progress rate relative to peers this cycle == contended/overloaded
            if (normalised <= LOW_CUT) {
                result[i] = LoadState.OVERLOADED;
                overloadCount++;
            } else if (normalised >= HIGH_CUT) {
                result[i] = LoadState.UNDERLOADED;
                underloadCount++;
            } else {
                result[i] = LoadState.BALANCED;
            }
        }

        Log.printlnConcat(readSpace.getNow(), ": [analyser_v5] classified ", n,
                " cloudlets by min-max range (min=", min, ", max=", max,
                ", lowCut=", LOW_CUT, ", highCut=", HIGH_CUT, ") -> overloaded=", overloadCount,
                ", underloaded=", underloadCount, ", balanced=", (n - overloadCount - underloadCount));

        return result;
    }

    @Override
    public String inputSemantic() {
        return "cloudlet-progressrate-mips";
    }

    @Override
    public String outputSemantic() {
        return "cloudlet-loadstate-progressrate";
    }

    @Override
    public int inputGuid() {
        return 1400;
    }

    @Override
    public int outputGuid() {
        return 2400;
    }
}
