package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import java.util.List;

/**
 * analyser_v8 - VM level, per-entity historical baseline classifier.
 * Strategy: every other variant in this set compares each entity against
 * its peers this cycle (cross-sectional). This variant instead compares
 * each VM's current CPU load against its OWN 30-reading rolling history
 * (temporal baselining), using getVmUtilizationMean/getVmUtilizationMad
 * from ReadSpace. Per the API contract the rolling mean is MIPS-scaled
 * while the rolling MAD is not, so the MAD is rescaled by the VM's MIPS
 * rating before being used to build a bound comparable to the mean.
 */
public class analyser_v8 implements Analyser<double[], LoadState[]> {

    private static final double OVERLOAD_MULTIPLIER = 1.5;
    private static final double UNDERLOAD_MULTIPLIER = 1.5;

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        int n = metrics.length;
        LoadState[] result = new LoadState[n];
        List<GuestEntity> vms = readSpace.getVmList();

        int overloadCount = 0;
        int underloadCount = 0;

        for (int i = 0; i < n; i++) {
            GuestEntity vm = vms.get(i);

            double historicMeanMips = readSpace.getVmUtilizationMean(vm);
            double historicMadFraction = readSpace.getVmUtilizationMad(vm);
            double vmMips = readSpace.getVmMips(vm);
            double historicMadMips = historicMadFraction * vmMips;

            if (historicMadMips <= 0.0) {
                result[i] = LoadState.BALANCED;
                continue;
            }

            double upperBound = historicMeanMips + OVERLOAD_MULTIPLIER * historicMadMips;
            double lowerBound = historicMeanMips - UNDERLOAD_MULTIPLIER * historicMadMips;

            double currentLoadMips = metrics[i];

            if (currentLoadMips > upperBound) {
                result[i] = LoadState.OVERLOADED;
                overloadCount++;
            } else if (currentLoadMips < lowerBound) {
                result[i] = LoadState.UNDERLOADED;
                underloadCount++;
            } else {
                result[i] = LoadState.BALANCED;
            }
        }

        Log.printlnConcat(readSpace.getNow(), ": [analyser_v8] classified ", n,
                " vms against their own historical baseline (over=", OVERLOAD_MULTIPLIER,
                "*MAD, under=", UNDERLOAD_MULTIPLIER, "*MAD) -> overloaded=", overloadCount,
                ", underloaded=", underloadCount, ", balanced=", (n - overloadCount - underloadCount));

        return result;
    }

    @Override
    public String inputSemantic() {
        return "vm-cpuload-mipsscaled";
    }

    @Override
    public String outputSemantic() {
        return "vm-loadstate-cpuload-baseline";
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
