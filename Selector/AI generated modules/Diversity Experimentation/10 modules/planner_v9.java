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

// Elastic Core Reclamation Planner.
// Among VMs flagged UNDERLOADED in the VM-level LoadState[] that have more
// than one PE (so reclamation cannot strand the VM with zero PEs), picks
// the one with the lowest rolling-mean CPU utilisation and deallocates a
// PE from it to free capacity for other guests.
public class planner_v9 implements Planner<LoadState[], int[]> {

    private static final int INPUT_GUID = 2300;
    private static final int OUTPUT_GUID = 3009;

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {
        List<GuestEntity> vms = readSpace.getVmList();
        int limit = Math.min(diagnosis.length, vms.size());

        GuestEntity candidate = null;
        double lowestUtil = Double.MAX_VALUE;
        for (int i = 0; i < limit; i++) {
            if (diagnosis[i] != LoadState.UNDERLOADED) {
                continue;
            }
            GuestEntity vm = vms.get(i);
            if (readSpace.getVmNumberOfPes(vm) <= 1) {
                continue;
            }
            double util = readSpace.getVmUtilizationMean(vm);
            if (util < lowestUtil) {
                lowestUtil = util;
                candidate = vm;
            }
        }

        if (candidate == null) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v9] no underloaded multi-pe vm found for pe reclamation");
            return new int[0];
        }

        int vmId = readSpace.getId(candidate);
        Log.printlnConcat(readSpace.getNow(), ": [planner_v9] deallocating pe from lowest-utilisation vm ", vmId);
        return new int[] { vmId };
    }

    @Override
    public String inputSemantic() {
        return "vm-loadstate-classification";
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
