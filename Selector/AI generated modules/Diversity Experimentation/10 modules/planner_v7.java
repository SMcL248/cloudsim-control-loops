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

// Reactive RAM Scaling Planner.
// Unlike a first-match policy, this scans all OVERLOADED VMs in the
// VM-level LoadState[] and selects the one currently provisioned with the
// most RAM, on the assumption that larger VMs under load are running the
// heaviest workloads and benefit most from additional RAM headroom.
public class planner_v7 implements Planner<LoadState[], int[]> {

    private static final int INPUT_GUID = 2300;
    private static final int OUTPUT_GUID = 3006;

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {
        List<GuestEntity> vms = readSpace.getVmList();
        int limit = Math.min(diagnosis.length, vms.size());
        int[] ramTiers = readSpace.getRamTiers();

        GuestEntity candidate = null;
        double largestRam = -1.0;
        for (int i = 0; i < limit; i++) {
            if (diagnosis[i] != LoadState.OVERLOADED) {
                continue;
            }
            GuestEntity vm = vms.get(i);
            double ram = readSpace.getVmRam(vm);
            if (ram > largestRam) {
                largestRam = ram;
                candidate = vm;
            }
        }

        if (candidate == null) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v7] no overloaded vm found for ram scaling");
            return new int[0];
        }

        double nextTier = readSpace.getNextRamTier(candidate);
        if (nextTier < 0) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v7] vm ", readSpace.getId(candidate), " already at max ram tier");
            return new int[0];
        }

        int tierIndex = -1;
        for (int i = 0; i < ramTiers.length; i++) {
            if (ramTiers[i] == Math.round(nextTier)) {
                tierIndex = i;
                break;
            }
        }

        if (tierIndex < 0) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v7] could not resolve ram tier index for vm ", readSpace.getId(candidate));
            return new int[0];
        }

        int vmId = readSpace.getId(candidate);
        Log.printlnConcat(readSpace.getNow(), ": [planner_v7] scaling largest overloaded vm ", vmId, " ram up to tier index ", tierIndex);
        return new int[] { vmId, tierIndex };
    }

    @Override
    public String inputSemantic() {
        return "vm-loadstate-classification";
    }

    @Override
    public String outputSemantic() {
        return "requestRamScaling";
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
