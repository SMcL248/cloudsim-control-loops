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
 * analyser_v3
 *
 * STRATEGY: Median +/- k * Median Absolute Deviation (MAD), a robust-statistics approach.
 * Level: HOST (2). Metric: host power draw in Watts, as reported by the monitor.
 *
 * Rationale: unlike mean/std-dev (analyser_v2), median/MAD is not dragged around by a
 * handful of extreme outliers (e.g. one host mid power-spike after boot-up). This makes
 * it a steadier signal for power-oriented decisions when the fleet has a few hosts in
 * unusual transient states. An absolute idle floor additionally guarantees that a host
 * drawing near-zero power is always recognised as a power-down/consolidation candidate,
 * even in a fleet where the whole cohort happens to be busy.
 */
public class analyser_v3 implements Analyser<double[], LoadState[]> {

    private static final int INPUT_GUID = 1200;
    private static final int OUTPUT_GUID = 2200;
    private static final String INPUT_SEMANTIC = "host-powerDrawWatts";
    private static final String OUTPUT_SEMANTIC = "host-loadState-medianMad";

    private static final double MAD_MULTIPLIER = 1.5;
    private static final double IDLE_POWER_FLOOR_WATTS = 5.0;

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        int n = metrics.length;
        LoadState[] states = new LoadState[n];
        if (n == 0) {
            return states;
        }

        double median = median(metrics);

        double[] absDeviations = new double[n];
        for (int i = 0; i < n; i++) {
            absDeviations[i] = Math.abs(metrics[i] - median);
        }
        double mad = median(absDeviations);

        int overloaded = 0;
        int underloaded = 0;
        int balanced = 0;

        double upperBand = median + MAD_MULTIPLIER * mad;
        double lowerBand = median - MAD_MULTIPLIER * mad;

        for (int i = 0; i < n; i++) {
            double power = metrics[i];
            if (power <= IDLE_POWER_FLOOR_WATTS) {
                states[i] = LoadState.UNDERLOADED;
                underloaded++;
            } else if (mad > 0.0 && power > upperBand) {
                states[i] = LoadState.OVERLOADED;
                overloaded++;
            } else if (mad > 0.0 && power < lowerBand) {
                states[i] = LoadState.UNDERLOADED;
                underloaded++;
            } else {
                states[i] = LoadState.BALANCED;
                balanced++;
            }
        }

        Log.printlnConcat(readSpace.getNow(), ": [analyser_v3] median=", median, " mad=", mad, " overloaded=", overloaded, " underloaded=", underloaded, " balanced=", balanced);
        return states;
    }

    private static double median(double[] values) {
        double[] sorted = Arrays.copyOf(values, values.length);
        Arrays.sort(sorted);
        int mid = sorted.length / 2;
        if (sorted.length % 2 == 0) {
            return (sorted[mid - 1] + sorted[mid]) / 2.0;
        }
        return sorted[mid];
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
