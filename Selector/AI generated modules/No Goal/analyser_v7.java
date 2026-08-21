package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.HostEntity;
import java.util.List;

/**
 * analyser_v7 - Host level, capacity-normalised classifier.
 * Strategy: unlike v1 (which thresholds an already-normalised fraction),
 * this variant receives each host's raw absolute used MIPS and must look up
 * that host's total MIPS capacity via ReadSpace to compute a per-host
 * utilisation ratio before thresholding. This makes the classification
 * sensitive to heterogeneous host capacity (old vs current vs modern
 * hardware) rather than assuming the monitor has already normalised the
 * value.
 */
public class analyser_v7 implements Analyser<double[], LoadState[]> {

    private static final double OVERLOAD_THRESHOLD = 0.85;
    private static final double UNDERLOAD_THRESHOLD = 0.15;

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        int n = metrics.length;
        LoadState[] result = new LoadState[n];
        List<HostEntity> hosts = readSpace.getAllHosts();

        int overloadCount = 0;
        int underloadCount = 0;

        for (int i = 0; i < n; i++) {
            HostEntity host = hosts.get(i);
            double capacity = readSpace.getHostTotalMips(host);

            if (capacity <= 0.0) {
                result[i] = LoadState.BALANCED;
                continue;
            }

            double utilisation = metrics[i] / capacity;

            if (utilisation > OVERLOAD_THRESHOLD) {
                result[i] = LoadState.OVERLOADED;
                overloadCount++;
            } else if (utilisation < UNDERLOAD_THRESHOLD) {
                result[i] = LoadState.UNDERLOADED;
                underloadCount++;
            } else {
                result[i] = LoadState.BALANCED;
            }
        }

        Log.printlnConcat(readSpace.getNow(), ": [analyser_v7] classified ", n,
                " hosts by capacity-normalised MIPS (over=", OVERLOAD_THRESHOLD,
                ", under=", UNDERLOAD_THRESHOLD, ") -> overloaded=", overloadCount,
                ", underloaded=", underloadCount, ", balanced=", (n - overloadCount - underloadCount));

        return result;
    }

    @Override
    public String inputSemantic() {
        return "host-usedmips-absolute";
    }

    @Override
    public String outputSemantic() {
        return "host-loadstate-usedmips";
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
