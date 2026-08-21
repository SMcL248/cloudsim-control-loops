package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;

import java.util.Arrays;
import java.util.List;

/**
 * Variant 2 - VM level - Rank/quantile classification.
 * Assumes the input metric is per-VM instantaneous CPU utilisation
 * as a fraction in [0,1]. Classifies each VM by its rank position
 * within the observed snapshot rather than by distance from a
 * statistical centre - the top third of ranks are OVERLOADED, the
 * bottom third are UNDERLOADED, the middle third is BALANCED.
 */
public class analyser_v2 implements Analyser<double[], LoadState[]> {

    private static final int INPUT_GUID = 1300;
    private static final int OUTPUT_GUID = 2300;

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        int n = metrics.length;
        LoadState[] result = new LoadState[n];
        if (n == 0) {
            return result;
        }
        if (n == 1) {
            result[0] = LoadState.BALANCED;
            return result;
        }

        Integer[] order = new Integer[n];
        for (int i = 0; i < n; i++) {
            order[i] = i;
        }
        Arrays.sort(order, (a, b) -> Double.compare(metrics[a], metrics[b]));

        int overloaded = 0;
        int underloaded = 0;

        for (int rank = 0; rank < n; rank++) {
            int idx = order[rank];
            double normalisedRank = (double) rank / (n - 1);
            if (normalisedRank >= (2.0 / 3.0)) {
                result[idx] = LoadState.OVERLOADED;
                overloaded++;
            } else if (normalisedRank <= (1.0 / 3.0)) {
                result[idx] = LoadState.UNDERLOADED;
                underloaded++;
            } else {
                result[idx] = LoadState.BALANCED;
            }
        }

        List<GuestEntity> vms = readSpace.getVmList();
        Log.printlnConcat(readSpace.getNow(), ": [analyser_v2] ranked ", n,
            " of ", vms.size(), " VMs by utilisation -> overloaded=", overloaded,
            ", underloaded=", underloaded, ", balanced=", (n - overloaded - underloaded));

        return result;
    }

    @Override
    public String inputSemantic() {
        return "vm-cpuUtilFraction-instantaneous";
    }

    @Override
    public String outputSemantic() {
        return "vm-loadState-rankThirds";
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
