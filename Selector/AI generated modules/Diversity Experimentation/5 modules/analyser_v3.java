package org.cloudbus.cloudsim.examples;// always include

// Import whats needed
import java.util.Arrays;
import org.cloudbus.cloudsim.Log;

/**
 * Variant 3 - Cloudlet level, interquartile-range (Tukey fence) thresholds.
 * A cloudlet with disproportionately more work remaining than its peers this
 * tick is falling behind (overloaded); one with disproportionately little
 * remaining is nearly finished (underloaded). Fences are derived from the
 * observed distribution of remaining-length fractions, not fixed constants.
 */
public class analyser_v3 implements Analyser<double[], LoadState[]> {

    private static final int INPUT_GUID = 1400;
    private static final int OUTPUT_GUID = 2400;
    private static final String INPUT_SEMANTIC = "cloudlet-remaining-length-fraction-of-total-length";
    private static final String OUTPUT_SEMANTIC = "cloudlet-load-classification-balanced-under-over";

    private static final double IQR_MULTIPLIER = 1.5;
    private static final int MIN_SAMPLE_FOR_IQR = 4;

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        int n = metrics.length;
        LoadState[] states = new LoadState[n];

        double lowerFence;
        double upperFence;

        if (n >= MIN_SAMPLE_FOR_IQR) {
            double[] sorted = Arrays.copyOf(metrics, n);
            Arrays.sort(sorted);
            double q1 = percentile(sorted, 0.25);
            double q3 = percentile(sorted, 0.75);
            double iqr = q3 - q1;
            lowerFence = q1 - IQR_MULTIPLIER * iqr;
            upperFence = q3 + IQR_MULTIPLIER * iqr;
        } else {
            // Too few active cloudlets this tick for quartiles to be
            // meaningful; fall back to treating everyone as on-pace.
            lowerFence = Double.NEGATIVE_INFINITY;
            upperFence = Double.POSITIVE_INFINITY;
        }

        int overloaded = 0;
        int underloaded = 0;
        int balanced = 0;

        for (int i = 0; i < n; i++) {
            double remainingFraction = metrics[i];
            LoadState state;

            if (remainingFraction > upperFence) {
                state = LoadState.OVERLOADED;
            } else if (remainingFraction < lowerFence) {
                state = LoadState.UNDERLOADED;
            } else {
                state = LoadState.BALANCED;
            }

            states[i] = state;
            if (state == LoadState.OVERLOADED) overloaded++;
            else if (state == LoadState.UNDERLOADED) underloaded++;
            else balanced++;
        }

        Log.printlnConcat(readSpace.getNow(), ": [analyser_v3] IQR cloudlet classification complete -> ",
                n, " cloudlets, overloaded=", overloaded, ", underloaded=", underloaded, ", balanced=", balanced);

        return states;
    }

    // Linear-interpolation percentile over an already-sorted array.
    private double percentile(double[] sorted, double p) {
        if (sorted.length == 1) {
            return sorted[0];
        }
        double rank = p * (sorted.length - 1);
        int lowIndex = (int) Math.floor(rank);
        int highIndex = (int) Math.ceil(rank);
        if (lowIndex == highIndex) {
            return sorted[lowIndex];
        }
        double fraction = rank - lowIndex;
        return sorted[lowIndex] + fraction * (sorted[highIndex] - sorted[lowIndex]);
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
