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
 * Strategy: idle-empty VM reclamation.
 * Among VMs flagged UNDERLOADED that currently hold zero cloudlets, picks
 * the one with the lowest rolling mean utilisation and destroys it
 * outright. Unlike scale-down strategies this removes the guest entirely,
 * trading elasticity for maximum resource and cost reclamation on VMs
 * doing no useful work.
 */
public class planner_v11 implements Planner<LoadState[], int[]> {

    private static final String MODULE_NAME = "planner_v11";

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
            if (!readSpace.getVmCloudletList(vm).isEmpty()) {
                continue;
            }
            double mean = readSpace.getVmUtilizationMean(vm);
            if (mean < lowestMean) {
                lowestMean = mean;
                target = vm;
            }
        }

        if (target == null) {
            Log.printlnConcat(readSpace.getNow(), ": [" + MODULE_NAME + "] no underloaded, empty, migratable vm found for reclamation");
            return new int[0];
        }

        int vmId = readSpace.getId(target);
        Log.printlnConcat(readSpace.getNow(), ": [" + MODULE_NAME + "] destroying idle empty vm " + vmId);
        return new int[]{vmId};
    }

    @Override
    public String inputSemantic() {
        return "vm-cpu-utilisation-idle-empty";
    }

    @Override
    public String outputSemantic() {
        return "destroy-idle-empty-vm";
    }

    @Override
    public int inputGuid() {
        return 2300;
    }

    @Override
    public int outputGuid() {
        return 3004;
    }
}
