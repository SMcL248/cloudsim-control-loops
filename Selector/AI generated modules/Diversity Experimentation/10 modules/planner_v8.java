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

// Elastic Core Allocation Planner.
// Among all VMs flagged OVERLOADED in the VM-level LoadState[], selects the
// one with the highest rolling-mean CPU utilisation and grants it an extra
// PE, targeting the most consistently saturated VM rather than simply the
// first one encountered.
public class planner_v8 implements Planner<LoadState[], int[]> {

    private static final int INPUT_GUID = 2300;
    private static final int OUTPUT_GUID = 3008;

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {
        List<GuestEntity> vms = readSpace.getVmList();
        int limit = Math.min(diagnosis.length, vms.size());

        GuestEntity candidate = null;
        double highestUtil = -1.0;
        for (int i = 0; i < limit; i++) {
            if (diagnosis[i] != LoadState.OVERLOADED) {
                continue;
            }
            GuestEntity vm = vms.get(i);
            double util = readSpace.getVmUtilizationMean(vm);
            if (util > highestUtil) {
                highestUtil = util;
                candidate = vm;
            }
        }

        if (candidate == null) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v8] no overloaded vm found for pe allocation");
            return new int[0];
        }

        int vmId = readSpace.getId(candidate);
        Log.printlnConcat(readSpace.getNow(), ": [planner_v8] allocating additional pe to highest-utilisation vm ", vmId);
        return new int[] { vmId };
    }

    @Override
    public String inputSemantic() {
        return "vm-loadstate-classification";
    }

    @Override
    public String outputSemantic() {
        return "requestPeAllocation";
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
