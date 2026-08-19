package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;
import org.cloudbus.cloudsim.core.PowerGuestEntity;
import org.cloudbus.cloudsim.core.PowerHostEntity;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.power.PowerDatacenter;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.power.PowerVm;
import java.util.List;

// Reactive Vertical MIPS Scaling Planner.
// Walks the VM-level LoadState[] in order and scales the first OVERLOADED
// VM up to its next MIPS tier, provided that tier exists and can be
// resolved to a valid index in the permitted tier list.
public class planner_v6 implements Planner<LoadState[], int[]> {

    private static final int INPUT_GUID = 2300;
    private static final int OUTPUT_GUID = 3005;

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {
        List<GuestEntity> vms = readSpace.getVmList();
        int limit = Math.min(diagnosis.length, vms.size());
        int[] mipsTiers = readSpace.getMipsTiers();

        for (int i = 0; i < limit; i++) {
            if (diagnosis[i] != LoadState.OVERLOADED) {
                continue;
            }
            GuestEntity vm = vms.get(i);
            double nextTier = readSpace.getNextMipsTier(vm);
            if (nextTier < 0) {
                continue;
            }
            int tierIndex = findTierIndex(mipsTiers, nextTier);
            if (tierIndex < 0) {
                continue;
            }
            int vmId = readSpace.getId(vm);
            Log.printlnConcat(readSpace.getNow(), ": [planner_v6] scaling vm ", vmId, " up to mips tier index ", tierIndex);
            return new int[] { vmId, tierIndex };
        }

        Log.printlnConcat(readSpace.getNow(), ": [planner_v6] no overloaded vm eligible for mips scale-up");
        return new int[0];
    }

    private int findTierIndex(int[] tiers, double value) {
        for (int i = 0; i < tiers.length; i++) {
            if (tiers[i] == Math.round(value)) {
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
        return "requestMipsScaling";
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
