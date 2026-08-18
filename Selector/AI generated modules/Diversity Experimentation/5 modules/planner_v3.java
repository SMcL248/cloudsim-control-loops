package org.cloudbus.cloudsim.examples;// always include

import java.util.List;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;

/**
 * planner_v3
 *
 * Strategy: "Vertical scale-up for the worst offender"
 * VM-level diagnosis. Among VMs flagged OVERLOADED (and not already
 * mid-migration or mid-instantiation), selects the one with the highest CPU
 * utilisation whose next MIPS tier is actually available (getNextMipsTier
 * != -1 sentinel, and that value matches a known tier index), and requests
 * a scale to that tier.
 * Emits requestMipsScaling{vmId, tierIndex}, or an empty array if no VM
 * qualifies.
 */
public class planner_v3 implements Planner<LoadState[], int[]> {

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {

        List<GuestEntity> vms = readSpace.getVmList();
        int limit = Math.min(diagnosis.length, vms.size());
        int[] mipsTiers = readSpace.getMipsTiers();

        GuestEntity worstVm = null;
        int worstTierIndex = -1;
        double worstUtil = -1.0;

        for (int i = 0; i < limit; i++) {
            if (diagnosis[i] != LoadState.OVERLOADED) {
                continue;
            }
            GuestEntity vm = vms.get(i);
            if (readSpace.isVmMigrating(vm) || readSpace.isVmBeingInstantiated(vm)) {
                continue;
            }

            double nextTier = readSpace.getNextMipsTier(vm);
            if (nextTier < 0) {
                // Already maxed out, or current MIPS doesn't match a known tier.
                continue;
            }

            int tierIndex = indexOfTier(mipsTiers, nextTier);
            if (tierIndex < 0) {
                continue;
            }

            double util = readSpace.getVmCpuUtil(vm);
            if (util > worstUtil) {
                worstUtil = util;
                worstVm = vm;
                worstTierIndex = tierIndex;
            }
        }

        if (worstVm == null) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v3] ", "no scalable overloaded vm found");
            return new int[0];
        }

        int vmId = readSpace.getId(worstVm);
        Log.printlnConcat(readSpace.getNow(), ": [planner_v3] ",
                "scaling up vm " + vmId + " to mips tier index " + worstTierIndex);
        return new int[] { vmId, worstTierIndex };
    }

    private int indexOfTier(int[] tiers, double value) {
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
        return "vm-loadstate-classification";
    }

    @Override
    public String outputSemantic() {
        return "vm-mips-scale-up";
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
