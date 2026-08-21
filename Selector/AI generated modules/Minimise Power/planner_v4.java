package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import java.util.List;

/**
 * Variant 4 - Consistently-Idle PE Reclamation.
 *
 * Strategy: a snapshot-low utilisation reading can just be a momentary lull
 * in a bursty VM that will need its PEs back shortly. This variant instead
 * uses the 30-reading rolling mean *and* mean-absolute-deviation of CPU
 * utilisation to find VMs that are reliably, stably idle - low average and
 * low volatility - and only strips a PE from those. This avoids
 * deallocating from VMs that merely look idle right now but are actually
 * spiky, which would just cause a PE to be re-requested moments later.
 */
public class planner_v4 implements Planner<LoadState[], int[]> {

    private static final int INPUT_GUID = 2300;
    private static final int OUTPUT_GUID = 3009;

    private static final double MEAN_UTIL_THRESHOLD = 0.2;
    private static final double VOLATILITY_FRACTION_THRESHOLD = 0.05;

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {
        List<GuestEntity> vms = readSpace.getVmList();
        int limit = Math.min(diagnosis.length, vms.size());

        GuestEntity steadiestIdleVm = null;
        double bestScore = Double.MAX_VALUE;

        for (int i = 0; i < limit; i++) {
            if (diagnosis[i] != LoadState.UNDERLOADED) {
                continue;
            }
            GuestEntity vm = vms.get(i);
            if (readSpace.getVmNumberOfPes(vm) <= 1) {
                continue;
            }
            if (readSpace.isVmMigrating(vm) || readSpace.isVmBeingInstantiated(vm)) {
                continue;
            }

            double meanUtil = readSpace.getVmUtilizationMean(vm);
            if (meanUtil > MEAN_UTIL_THRESHOLD) {
                continue;
            }

            double madAbsoluteMips = readSpace.getVmUtilizationMad(vm) * readSpace.getVmMips(vm);
            double totalMips = readSpace.getVmMips(vm);
            double volatilityFraction = totalMips > 0 ? madAbsoluteMips / totalMips : 1.0;
            if (volatilityFraction > VOLATILITY_FRACTION_THRESHOLD) {
                continue;
            }

            double score = meanUtil + volatilityFraction;
            if (score < bestScore) {
                bestScore = score;
                steadiestIdleVm = vm;
            }
        }

        int[] noOp = new int[]{-1};
        if (steadiestIdleVm == null) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v4] no consistently-idle vm found, emitting no-op");
            return noOp;
        }

        int vmId = readSpace.getId(steadiestIdleVm);
        Log.printlnConcat(readSpace.getNow(), ": [planner_v4] deallocating a pe from consistently-idle vm ", vmId);
        return new int[]{vmId};
    }

    @Override
    public String inputSemantic() {
        return "vm-cpu-utilisation-loadstate";
    }

    @Override
    public String outputSemantic() {
        return "requestPeDeallocation";
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
