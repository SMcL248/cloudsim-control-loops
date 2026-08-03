package org.cloudbus.cloudsim.examples;

import java.util.List;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;

/**
 * Planner v4 - Scale-out VM creation planner (throughput-oriented).
 *
 * Strategy:
 *   Diagnosis is a per-VM LoadState[] (index i corresponds to
 *   readSpace.getVmList().get(i)). When at least a third of VMs are
 *   OVERLOADED, the fleet needs more capacity; requests a new, top-tier VM
 *   (highest MIPS/RAM/BW/core tier index) in the datacenter hosting the
 *   first overloaded VM found. ReadSpace exposes no separate storage-size
 *   tier accessor, so the same top tier index is reused for sizeTierIndex.
 *
 * Input semantic  : vm-loadstate-saturation (GUID 2300)
 * Output semantic : vm-creation             (GUID 3007, requestVmCreation)
 */
public class planner_v4 implements Planner<LoadState[], int[]> {

    private static final double SATURATION_RATIO = 1.0 / 3.0;

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {
        double now = readSpace.getNow();
        List<GuestEntity> vms = readSpace.getVmList();

        if (diagnosis == null || diagnosis.length != vms.size()) {
            Log.printlnConcat(now, ": [planner_v4] Diagnosis/VM size mismatch. No-op.");
            return new int[]{-1, -1, -1};
        }

        int overloadedCount = 0;
        int refVmId = -1;
        for (int i = 0; i < vms.size(); i++) {
            if (diagnosis[i] == LoadState.OVERLOADED) {
                overloadedCount++;
                if (refVmId == -1) refVmId = readSpace.getId(vms.get(i));
            }
        }

        if (vms.isEmpty() || (double) overloadedCount / vms.size() < SATURATION_RATIO) {
            Log.printlnConcat(now, ": [planner_v4] Fleet not saturated (", overloadedCount,
                    "/", vms.size(), " VMs overloaded). No-op.");
            return new int[]{-1, -1, -1};
        }

        int[] mipsTiers = readSpace.getMipsTiers();
        int topTier = mipsTiers.length - 1;
        int datacenterId = readSpace.getDatacenterFor(refVmId);

        Log.printlnConcat(now, ": [planner_v4] ", overloadedCount, "/", vms.size(),
                " VMs overloaded. Plan create top-tier VM (tier ", topTier,
                ") in datacenter ", datacenterId);
        return new int[]{topTier, topTier, datacenterId};
    }

    @Override
    public String inputSemantic() {
        return "vm-loadstate-saturation";
    }

    @Override
    public String outputSemantic() {
        return "vm-creation";
    }

    @Override
    public int inputGuid() {
        return 2300;
    }

    @Override
    public int outputGuid() {
        return 3007;
    }
}
