package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;

import java.util.List;

// Strategy: headroom-gated RAM elasticity. Diagnosis is per-VM. Grows RAM for an overloaded VM only
// if its host actually has spare RAM to give (otherwise the request would be futile and wasted).
// If no overloaded VM has headroom, reclaims RAM from the most underloaded VM instead, freeing host
// memory that supports later consolidation and power savings.
public class planner_v6 implements Planner<LoadState[], int[]> {

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {

        List<GuestEntity> vms = readSpace.getVmList();

        if (diagnosis == null || vms == null || diagnosis.length != vms.size()) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v6] diagnosis/VM list mismatch, no-op");
            return new int[0];
        }

        int[] ramTiers = readSpace.getRamTiers();

        for (int i = 0; i < diagnosis.length; i++) {
            if (diagnosis[i] != LoadState.OVERLOADED) continue;
            GuestEntity vm = vms.get(i);
            HostEntity host = findHost(vm, readSpace);
            if (host == null) continue;

            double currentRam = readSpace.getVmRam(vm);
            double nextRam = readSpace.getNextRamTier(vm);
            if (nextRam <= currentRam) continue;

            double headroom = readSpace.getHostAvailableRam(host);
            if (headroom < (nextRam - currentRam)) continue;

            int tierIndex = indexOf(ramTiers, nextRam);
            if (tierIndex == -1) continue;

            int vmId = readSpace.getId(vm);
            Log.printlnConcat(readSpace.getNow(), ": [planner_v6] scaling up RAM for overloaded VM ", vmId, " to tier ", tierIndex);
            return new int[] { vmId, tierIndex };
        }

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
            Log.printlnConcat(readSpace.getNow(), ": [planner_v6] no eligible overloaded or underloaded VM, no-op");
            return new int[0];
        }

        GuestEntity vm = vms.get(underloadedIndex);
        double currentRam = readSpace.getVmRam(vm);
        int currentTierIndex = indexOf(ramTiers, currentRam);

        if (currentTierIndex <= 0) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v6] VM ", readSpace.getId(vm), " already at lowest RAM tier, no-op");
            return new int[0];
        }

        int tierIndex = currentTierIndex - 1;
        int vmId = readSpace.getId(vm);

        Log.printlnConcat(readSpace.getNow(), ": [planner_v6] scaling down RAM for underloaded VM ", vmId, " to tier ", tierIndex);

        return new int[] { vmId, tierIndex };
    }

    private HostEntity findHost(GuestEntity vm, ReadSpace readSpace) {
        int vmId = readSpace.getId(vm);
        for (HostEntity host : readSpace.getAllHosts()) {
            for (GuestEntity hosted : readSpace.getVmListForHost(host)) {
                if (readSpace.getId(hosted) == vmId) {
                    return host;
                }
            }
        }
        return null;
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
        return "requestRamScaling";
    }

    @Override
    public int inputGuid() {
        return 2300;
    }

    @Override
    public int outputGuid() {
        return 3006;
    }
}
