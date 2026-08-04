package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;

// Cloudlet-level analyser. Interprets metrics[i] as a per-cloudlet
// remaining-length fraction (remaining/total, in [0,1]) aligned with
// readSpace.getActiveCloudlets(). Uses mean +/- (k * stddev) of the
// observed batch, but k itself is adaptive: it widens when the batch's
// coefficient of variation is low (little natural spread, so a tighter
// multiplier would over-flag cloudlets) and narrows when the batch is
// already highly dispersed. Cloudlets with unusually high remaining work
// are OVERLOADED (at risk of missing completion), unusually low remaining
// work are UNDERLOADED (nearly finished, low risk).
public class analyser_v9 implements Analyser<double[], LoadState[]> {

    private static final String MODULE_NAME = "analyser_v9";
    private static final int INPUT_GUID = 1400;
    private static final int OUTPUT_GUID = 2400;
    private static final double HIGH_CV_MULTIPLIER = 1.0;
    private static final double LOW_CV_MULTIPLIER = 0.5;
    private static final double CV_SWITCH_POINT = 0.5;

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        int n = metrics.length;
        LoadState[] result = new LoadState[n];

        if (n == 0) {
            return result;
        }

        double mean = 0.0;
        for (double v : metrics) {
            mean += v;
        }
        mean /= n;

        double variance = 0.0;
        for (double v : metrics) {
            variance += (v - mean) * (v - mean);
        }
        variance /= n;
        double stddev = Math.sqrt(variance);

        double coefficientOfVariation = (mean != 0.0) ? (stddev / mean) : 0.0;
        double k = (coefficientOfVariation > CV_SWITCH_POINT) ? HIGH_CV_MULTIPLIER : LOW_CV_MULTIPLIER;

        double upperBound = mean + (k * stddev);
        double lowerBound = mean - (k * stddev);

        for (int i = 0; i < n; i++) {
            double v = metrics[i];
            if (v > upperBound) {
                result[i] = LoadState.OVERLOADED;
            } else if (v < lowerBound) {
                result[i] = LoadState.UNDERLOADED;
            } else {
                result[i] = LoadState.BALANCED;
            }
        }

        Log.printlnConcat(readSpace.getNow(), ": [" + MODULE_NAME
                + "] classified " + n + " cloudlets via remaining-length adaptive stddev (mean="
                + mean + ", stddev=" + stddev + ", k=" + k + ")");

        return result;
    }

    @Override
    public String inputSemantic() {
        return "cloudlet-remaining-length-fraction";
    }

    @Override
    public String outputSemantic() {
        return "cloudlet-load-classification-remaining-adaptive-std";
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
