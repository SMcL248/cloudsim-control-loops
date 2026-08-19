package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import java.util.List;

/*
 * Variant: analyser_v10
 * Level: VM
 * Metric: effective throughput (MIPS)
 * Strategy: personalized baseline - each VM is judged only against its OWN
 * rolling history (readSpace.getVmUtilizationMean/Mad), never against its
 * peers in the current batch. This is the only variant that performs no
 * cross-entity comparison at all: a VM that is simply "busier than average"
 * is not flagged unless it is busier than ITS OWN recent normal. Per the
 * ReadSpace contract, getVmUtilizationMad is expressed in raw (unscaled)
 * utilisation units while getVmUtilizationMean and the incoming throughput
 * metric are MIPS-scaled, so the MAD is scaled by the VM's MIPS rating
 * before it is used to build a comparable band.
 */
public class analyser_v10 implements Analyser<double[], LoadState[]> {

    private static final double K = 1.5;
    private static final double MIN_BAND = 1e-6;

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        List<GuestEntity> vms = readSpace.getVmList();
        int n = metrics.length;
        LoadState[] states = new LoadState[n];
        int overloaded = 0, underloaded = 0, balanced = 0;

        for (int i = 0; i < n; i++) {
            GuestEntity vm = vms.get(i);
            double personalMean = readSpace.getVmUtilizationMean(vm);
            double personalMadRaw = readSpace.getVmUtilizationMad(vm);
            double personalMadScaled = personalMadRaw * readSpace.getVmMips(vm);
            double band = Math.max(K * personalMadScaled, MIN_BAND);

            double value = metrics[i];
            LoadState state;
            if (value > personalMean + band) {
                state = LoadState.OVERLOADED;
            } else if (value < personalMean - band) {
                state = LoadState.UNDERLOADED;
            } else {
                state = LoadState.BALANCED;
            }
            states[i] = state;
            switch (state) {
                case OVERLOADED: overloaded++; break;
                case UNDERLOADED: underloaded++; break;
                default: balanced++; break;
            }
            if (state != LoadState.BALANCED) {
                Log.printlnConcat(readSpace.getNow(), ": [analyser_v10] vm ", readSpace.getId(vm), " deviates from its own baseline (mean=", personalMean, ") -> ", state);
            }
        }

        Log.printlnConcat(readSpace.getNow(), ": [analyser_v10] personalized-baseline classification: ", overloaded, " overloaded, ", underloaded, " underloaded, ", balanced, " balanced");
        return states;
    }

    @Override
    public String inputSemantic() {
        return "vm-effective-throughput-mips";
    }

    @Override
    public String outputSemantic() {
        return "vm-load-state-personalized-baseline";
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
