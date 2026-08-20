package org.cloudbus.cloudsim.examples;// always include

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

/**
 * Strategy: idle-PE reclamation.
 * Among VMs flagged UNDERLOADED that hold more than one PE, reclaims a PE
 * from the one with the lowest rolling mean utilisation, freeing compute
 * headroom on its host without touching VMs that are merely temporarily
 * quiet.
 */
public class planner_v6 implements Planner<LoadState[], int[]> {

    private static final String MODULE_NAME = "planner_v6";

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {
        List<GuestEntity> vms = readSpace.getVmList();

        GuestEntity target = null;
        double lowestMean = Double.MAX_VALUE;
        for (int i = 0; i < diagnosis.length && i < vms.size(); i++) {
            if (diagnosis[i] != LoadState.UNDERLOADED) {
                continue;
            }
            GuestEntity vm = vms.get(i);
            if (readSpace.isVmMigrating(vm) || readSpace.isVmBeingInstantiated(vm)) {
                continue;
            }
            if (readSpace.getVmNumberOfPes(vm) <= 1) {
                continue;
            }
            double mean = readSpace.getVmUtilizationMean(vm);
            if (mean < lowestMean) {
                lowestMean = mean;
                target = vm;
            }
        }

        if (target == null) {
            Log.printlnConcat(readSpace.getNow(), ": [" + MODULE_NAME + "] no underloaded multi-pe vm eligible for pe reclamation");
            return new int[0];
        }

        int vmId = readSpace.getId(target);
        Log.printlnConcat(readSpace.getNow(), ": [" + MODULE_NAME + "] deallocating pe from vm " + vmId + " with mean utilisation " + lowestMean);
        return new int[]{vmId};
    }

    @Override
    public String inputSemantic() {
        return "vm-cpu-utilisation-mean-idle";
    }

    @Override
    public String outputSemantic() {
        return "deallocate-pe-lowest-util-vm";
    }

    @Override
    public int inputGuid() {
        return 2300;
    }

    @Override
    public int outputGuid() {
        return 3009;
    }
}
