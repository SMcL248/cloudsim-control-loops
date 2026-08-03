package org.cloudbus.cloudsim.examples;

import java.util.Arrays;
import java.util.List;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;

/**
 * Analyser v6 - VM-level CPU utilisation classifier (median + MAD robust
 * z-score).
 *
 * Level        : VM (level 3)
 * Metric       : Per-VM CPU utilisation ratio, range approx [0, 1].
 * Threshold    : Dynamic - uses the median and median absolute deviation
 *                (MAD) of the current snapshot rather than mean/stddev,
 *                which is less sensitive to a handful of extreme VMs.
 *                A modified z-score beyond +/-1.5 flags OVERLOADED /
 *                UNDERLOADED respectively.
 * Migration    : A VM currently mid-migration is always reported
 * rule           BALANCED, since its utilisation reading is transient
 *                and should not trigger a further control action.
 */
public class analyser_v6 implements Analyser<double[], LoadState[]> {

    private static final double MODIFIED_Z_THRESHOLD = 1.5;
    private static final double MAD_SCALE_FACTOR = 0.6745;

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        double now = readSpace.getNow();
        List<GuestEntity> vms = readSpace.getVmList();
        int n = metrics.length;
        LoadState[] states = new LoadState[n];

        double median = median(metrics);

        double[] absDeviations = new double[n];
        for (int i = 0; i < n; i++) {
            absDeviations[i] = Math.abs(metrics[i] - median);
        }
        double mad = median(absDeviations);

        for (int i = 0; i < n; i++) {
            boolean migrating = (i < vms.size()) && readSpace.isVmMigrating(vms.get(i));

            if (migrating) {
                states[i] = LoadState.BALANCED;
                continue;
            }

            if (mad <= 0.0) {
                states[i] = LoadState.BALANCED;
                continue;
            }

            double modifiedZ = MAD_SCALE_FACTOR * (metrics[i] - median) / mad;

            if (modifiedZ > MODIFIED_Z_THRESHOLD) {
                states[i] = LoadState.OVERLOADED;
            } else if (modifiedZ < -MODIFIED_Z_THRESHOLD) {
                states[i] = LoadState.UNDERLOADED;
            } else {
                states[i] = LoadState.BALANCED;
            }
        }

        Log.printlnConcat(now, ": [analyser_v6] classified ", n,
                " vms, median=", median, " mad=", mad);

        return states;
    }

    private double median(double[] values) {
        int n = values.length;
        if (n == 0) {
            return 0.0;
        }
        double[] sorted = Arrays.copyOf(values, n);
        Arrays.sort(sorted);
        int mid = n / 2;
        if (n % 2 == 0) {
            return (sorted[mid - 1] + sorted[mid]) / 2.0;
        }
        return sorted[mid];
    }

    @Override
    public String inputSemantic() {
        return "vm-cpu-util-ratio";
    }

    @Override
    public String outputSemantic() {
        return "vm-load-state-mad";
    }

    @Override
    public int inputGuid() {
        return 1300;
    }

    @Override
    public int outputGuid() {
        return 2300;
    }
}
