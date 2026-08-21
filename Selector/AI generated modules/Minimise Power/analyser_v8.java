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
import java.util.Arrays;

/**
 * analyser_v8
 *
 * STRATEGY: Tukey IQR outlier fences. Level: VM (3). Metric: VM CPU utilisation ratio
 * (0..1).
 *
 * Rationale: computes the interquartile range (Q3 - Q1) of the observed VM utilisations
 * and flags only genuine statistical outliers - values beyond 1.5x the IQR past the
 * quartiles - as OVERLOADED/UNDERLOADED. This differs from the mean/std-dev approach
 * (v2/v9) by being quartile-based rather than moment-based, so it is not skewed by a few
 * extreme values the way a mean can be, and differs from MAD (v3) in using a fixed
 * 1.5x-IQR convention rather than a scaled deviation constant. A VM sitting exactly at
 * zero utilisation is always treated as UNDERLOADED (a true idle guest, regardless of
 * fence placement), since idle VMs are a direct and unambiguous power-saving opportunity.
 */
public class analyser_v8 implements Analyser<double[], LoadState[]> {

    private static final int INPUT_GUID = 1300;
    private static final int OUTPUT_GUID = 2300;
    private static final String INPUT_SEMANTIC = "vm-cpuUtilRatio-0to1";
    private static final String OUTPUT_SEMANTIC = "vm-loadState-tukeyIqrFence";

    private static final double FENCE_MULTIPLIER = 1.5;

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        int n = metrics.length;
        LoadState[] states = new LoadState[n];
        if (n == 0) {
            return states;
        }

        double[] sorted = Arrays.copyOf(metrics, n);
        Arrays.sort(sorted);

        double q1 = percentile(sorted, 0.25);
        double q3 = percentile(sorted, 0.75);
        double iqr = q3 - q1;

        double lowerFence = q1 - FENCE_MULTIPLIER * iqr;
        double upperFence = q3 + FENCE_MULTIPLIER * iqr;

        int overloaded = 0;
        int underloaded = 0;
        int balanced = 0;

        for (int i = 0; i < n; i++) {
            double util = metrics[i];
            if (util <= 0.0) {
                states[i] = LoadState.UNDERLOADED;
                underloaded++;
            } else if (util > upperFence) {
                states[i] = LoadState.OVERLOADED;
                overloaded++;
            } else if (util < lowerFence) {
                states[i] = LoadState.UNDERLOADED;
                underloaded++;
            } else {
                states[i] = LoadState.BALANCED;
                balanced++;
            }
        }

        Log.printlnConcat(readSpace.getNow(), ": [analyser_v8] q1=", q1, " q3=", q3, " iqr=", iqr, " overloaded=", overloaded, " underloaded=", underloaded, " balanced=", balanced);
        return states;
    }

    private static double percentile(double[] sortedValues, double fraction) {
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
        return sortedValues[lowerIndex] * (1.0 - weight) + sortedValues[upperIndex] * weight;
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
