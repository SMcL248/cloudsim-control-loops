package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import java.util.List;

public class planner_v18 implements Planner<LoadState[], int[]> {

    private static final double STABLE_VARIATION_THRESHOLD = 0.15;

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {
        List<GuestEntity> vms = readSpace.getVmList();
        int limit = Math.min(diagnosis.length, vms.size());
        int[] tiers = readSpace.getMipsTiers();

        GuestEntity worstVm = null;
        double worstMeanThroughput = -1;
        double worstVariation = 0;

        for (int i = 0; i < limit; i++) {
            if (diagnosis[i] != LoadState.OVERLOADED) {
                continue;
            }
            GuestEntity vm = vms.get(i);
            double vmMips = readSpace.getVmMips(vm);
            if (vmMips <= 0) {
                continue;
            }
            double meanUtil = readSpace.getVmUtilizationMean(vm);
            double madUtil = readSpace.getVmUtilizationMad(vm);
            // MAD is on a fractional-utilisation basis, not MIPS-scaled; multiply by the
            // VM's MIPS rating before combining it with a MIPS-scaled quantity.
            double meanThroughput = meanUtil * vmMips;
            double madThroughput = madUtil * vmMips;

            if (meanThroughput > worstMeanThroughput) {
                worstMeanThroughput = meanThroughput;
                worstVm = vm;
                worstVariation = (meanThroughput > 0) ? madThroughput / meanThroughput : Double.MAX_VALUE;
            }
        }

        if (worstVm == null) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v18] no overloaded VM with usable utilisation statistics found");
            return new int[0];
        }

        int currentIndex = tierIndexOf(tiers, readSpace.getVmMips(worstVm));
        if (currentIndex < 0) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v18] VM ", readSpace.getId(worstVm), " current MIPS does not match a known tier, aborting");
            return new int[0];
        }

        boolean stableOverload = worstVariation < STABLE_VARIATION_THRESHOLD;
        int jump = stableOverload ? 2 : 1;
        int targetIndex = Math.min(tiers.length - 1, currentIndex + jump);

        if (targetIndex == currentIndex) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v18] VM ", readSpace.getId(worstVm), " already at highest MIPS tier");
            return new int[0];
        }

        int vmId = readSpace.getId(worstVm);
        Log.printlnConcat(readSpace.getNow(), ": [planner_v18] VM ", vmId, " sustained throughput=", worstMeanThroughput, ", variation=", worstVariation, ", scaling MIPS from tier ", currentIndex, " to tier ", targetIndex);
        return new int[] { vmId, targetIndex };
    }

    private int tierIndexOf(int[] tiers, double value) {
        int rounded = (int) Math.round(value);
        for (int i = 0; i < tiers.length; i++) {
            if (tiers[i] == rounded) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public String inputSemantic() {
        return "vm-loadstate-cpu-overload-confidence";
    }

    @Override
    public String outputSemantic() {
        return "mips-scale-up";
    }

    @Override
    public int inputGuid() {
        return 2300;
    }

    @Override
    public int outputGuid() {
        return 3005;
    }
}
