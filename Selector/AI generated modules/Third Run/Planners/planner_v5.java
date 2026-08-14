package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;

import java.util.List;

// Strategy: dual-objective vertical scaling. Diagnosis is per-VM. If any VM is overloaded, scales the
// worst one up a MIPS tier (throughput). Otherwise, if any VM is underloaded, scales the most idle one
// down a tier to shed unneeded power draw. Overload relief always takes priority over power saving.
public class planner_v5 implements Planner<LoadState[], int[]> {

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {

        List<GuestEntity> vms = readSpace.getVmList();

        if (diagnosis == null || vms == null || diagnosis.length != vms.size()) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v5] diagnosis/VM list mismatch, no-op");
            return new int[0];
        }

        int[] mipsTiers = readSpace.getMipsTiers();

        int overloadedIndex = -1;
        double worstUtil = -1;
        for (int i = 0; i < diagnosis.length; i++) {
            if (diagnosis[i] == LoadState.OVERLOADED) {
                double util = readSpace.getVmCpuUtil(vms.get(i));
                if (util > worstUtil) {
                    worstUtil = util;
                    overloadedIndex = i;
                }
            }
        }

        if (overloadedIndex != -1) {
            GuestEntity vm = vms.get(overloadedIndex);
            double currentMips = readSpace.getVmMips(vm);
            double nextMips = readSpace.getNextMipsTier(vm);

            if (nextMips <= currentMips) {
                Log.printlnConcat(readSpace.getNow(), ": [planner_v5] VM ", readSpace.getId(vm), " already at top MIPS tier, no-op");
                return new int[0];
            }

            int tierIndex = indexOf(mipsTiers, nextMips);
            if (tierIndex == -1) {
                Log.printlnConcat(readSpace.getNow(), ": [planner_v5] could not resolve next MIPS tier for VM ", readSpace.getId(vm), ", no-op");
                return new int[0];
            }

            int vmId = readSpace.getId(vm);
            Log.printlnConcat(readSpace.getNow(), ": [planner_v5] scaling up VM ", vmId, " to MIPS tier ", tierIndex);
            return new int[] { vmId, tierIndex };
        }

        // No overload pending: reclaim MIPS from the most underloaded VM to cut wasted power draw.
        int underloadedIndex = -1;
        double bestUtil = Double.MAX_VALUE;
        for (int i = 0; i < diagnosis.length; i++) {
            if (diagnosis[i] == LoadState.UNDERLOADED) {
                double util = readSpace.getVmCpuUtil(vms.get(i));
                if (util < bestUtil) {
                    bestUtil = util;
                    underloadedIndex = i;
                }
            }
        }

        if (underloadedIndex == -1) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v5] no overloaded or underloaded VM found, no-op");
            return new int[0];
        }

        GuestEntity vm = vms.get(underloadedIndex);
        double currentMips = readSpace.getVmMips(vm);
        int currentTierIndex = indexOf(mipsTiers, currentMips);

        if (currentTierIndex <= 0) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v5] VM ", readSpace.getId(vm), " already at lowest MIPS tier, no-op");
            return new int[0];
        }

        int tierIndex = currentTierIndex - 1;
        int vmId = readSpace.getId(vm);

        Log.printlnConcat(readSpace.getNow(), ": [planner_v5] scaling down VM ", vmId, " to MIPS tier ", tierIndex);

        return new int[] { vmId, tierIndex };
    }

    private int indexOf(int[] tiers, double value) {
        for (int i = 0; i < tiers.length; i++) {
            if (tiers[i] == (int) value) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public String inputSemantic() {
        return "vm-cpu-load-state";
    }

    @Override
    public String outputSemantic() {
        return "requestMipsScaling";
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
