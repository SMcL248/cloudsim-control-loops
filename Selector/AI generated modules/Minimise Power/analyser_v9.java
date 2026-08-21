package org.cloudbus.cloudsim.examples;// always include

// Import whats needed
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;
import org.cloudbus.cloudsim.core.PowerGuestEntity;
import org.cloudbus.cloudsim.core.PowerHostEntity;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.power.PowerDatacenter;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.power.PowerVm;

/**
 * analyser_v9
 *
 * STRATEGY: Coefficient-of-variation adaptive z-score. Level: VM (3). Metric: VM CPU
 * utilisation ratio (0..1).
 *
 * Rationale: a fixed-multiplier z-score (v2 at host level) uses the same band width no
 * matter how volatile the cohort is. This variant instead derives the multiplier itself
 * from the cohort's coefficient of variation (std dev / mean): when the VM fleet is
 * naturally volatile (high CV), the band widens so ordinary spread is not mistaken for
 * an outlier and the planner is not thrashed with reclassification; when the fleet is
 * unusually uniform (low CV), the band narrows so genuine deviation is still caught early.
 * The multiplier is clamped to a sane range to avoid degenerate bands at the extremes.
 * A cohort with zero mean utilisation (everything idle) is a meaningful signal on its own
 * and is classified UNDERLOADED outright.
 */
public class analyser_v9 implements Analyser<double[], LoadState[]> {

    private static final int INPUT_GUID = 1300;
    private static final int OUTPUT_GUID = 2300;
    private static final String INPUT_SEMANTIC = "vm-cpuUtilRatio-0to1";
    private static final String OUTPUT_SEMANTIC = "vm-loadState-cvAdaptiveZScore";

    private static final double MIN_MULTIPLIER = 0.3;
    private static final double MAX_MULTIPLIER = 1.5;

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        int n = metrics.length;
        LoadState[] states = new LoadState[n];
        if (n == 0) {
            return states;
        }

        double sum = 0.0;
        for (double v : metrics) {
            sum += v;
        }
        double mean = sum / n;

        int overloaded = 0;
        int underloaded = 0;
        int balanced = 0;

        if (mean == 0.0) {
            // Entire cohort idle - unambiguous power-saving signal, no need for a band.
            for (int i = 0; i < n; i++) {
                states[i] = LoadState.UNDERLOADED;
            }
            underloaded = n;
            Log.printlnConcat(readSpace.getNow(), ": [analyser_v9] cohort mean=0, all VMs marked UNDERLOADED");
            return states;
        }

        double sqDiffSum = 0.0;
        for (double v : metrics) {
            sqDiffSum += (v - mean) * (v - mean);
        }
        double stdDev = Math.sqrt(sqDiffSum / n);
        double coefficientOfVariation = stdDev / mean;

        double multiplier = Math.max(MIN_MULTIPLIER, Math.min(MAX_MULTIPLIER, coefficientOfVariation));

        double upperBand = mean + multiplier * stdDev;
        double lowerBand = mean - multiplier * stdDev;

        for (int i = 0; i < n; i++) {
            double util = metrics[i];
            if (util > upperBand) {
                states[i] = LoadState.OVERLOADED;
                overloaded++;
            } else if (util < lowerBand) {
                states[i] = LoadState.UNDERLOADED;
                underloaded++;
            } else {
                states[i] = LoadState.BALANCED;
                balanced++;
            }
        }

        Log.printlnConcat(readSpace.getNow(), ": [analyser_v9] mean=", mean, " stdDev=", stdDev, " cv=", coefficientOfVariation, " multiplier=", multiplier, " overloaded=", overloaded, " underloaded=", underloaded, " balanced=", balanced);
        return states;
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
