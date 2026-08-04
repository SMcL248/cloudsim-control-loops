package org.cloudbus.cloudsim.examples;// always include

import java.util.Arrays;
import org.cloudbus.cloudsim.Log;

// VM-level analyser. Interprets metrics[i] as a per-VM throughput deficit
// fraction: 1 - (effective throughput / requested mips), in [0,1], where
// 0 means the VM is getting all the MIPS it requested and 1 means it is
// fully starved. Aligned with readSpace.getVmList(). Uses linear-
// interpolated percentiles (10th/90th) of the observed batch: VMs in the
// worst decile of deficit are OVERLOADED (significant contention), VMs in
// the best decile are UNDERLOADED (effectively no contention).
public class analyser_v7 implements Analyser<double[], LoadState[]> {

    private static final String MODULE_NAME = "analyser_v7";
    private static final int INPUT_GUID = 1300;
    private static final int OUTPUT_GUID = 2300;
    private static final double UPPER_PERCENTILE = 0.90;
    private static final double LOWER_PERCENTILE = 0.10;

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        int n = metrics.length;
        LoadState[] result = new LoadState[n];

        if (n == 0) {
            return result;
        }

        double[] sorted = Arrays.copyOf(metrics, n);
        Arrays.sort(sorted);

        double p90 = interpolatedPercentile(sorted, UPPER_PERCENTILE);
        double p10 = interpolatedPercentile(sorted, LOWER_PERCENTILE);

        for (int i = 0; i < n; i++) {
            double v = metrics[i];
            if (v >= p90) {
                result[i] = LoadState.OVERLOADED;
            } else if (v <= p10) {
                result[i] = LoadState.UNDERLOADED;
            } else {
                result[i] = LoadState.BALANCED;
            }
        }

        Log.printlnConcat(readSpace.getNow(), ": [" + MODULE_NAME
                + "] classified " + n + " vms via throughput-deficit percentiles (p10="
                + p10 + ", p90=" + p90 + ")");

        return result;
    }

    // Linear-interpolated percentile over an already-sorted array.
    private double interpolatedPercentile(double[] sorted, double p) {
        int n = sorted.length;
        if (n == 1) {
            return sorted[0];
        }
        double rank = p * (n - 1);
        int lowIdx = (int) Math.floor(rank);
        int highIdx = (int) Math.ceil(rank);
        if (lowIdx == highIdx) {
            return sorted[lowIdx];
        }
        double fraction = rank - lowIdx;
        return sorted[lowIdx] + fraction * (sorted[highIdx] - sorted[lowIdx]);
    }

    @Override
    public String inputSemantic() {
        return "vm-throughput-deficit-fraction";
    }

    @Override
    public String outputSemantic() {
        return "vm-load-classification-throughput-percentile";
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
