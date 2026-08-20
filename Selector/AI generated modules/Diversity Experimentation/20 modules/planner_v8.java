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
 * Strategy: idle-driven MIPS scale-down.
 * Among VMs flagged UNDERLOADED, targets the one with the lowest rolling
 * mean utilisation and steps its current MIPS tier down by one, reclaiming
 * provisioned-but-unused capacity for the rest of the datacenter.
 */
public class planner_v8 implements Planner<LoadState[], int[]> {

    private static final String MODULE_NAME = "planner_v8";

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
            double mean = readSpace.getVmUtilizationMean(vm);
            if (mean < lowestMean) {
                lowestMean = mean;
                target = vm;
            }
        }

        if (target == null) {
            Log.printlnConcat(readSpace.getNow(), ": [" + MODULE_NAME + "] no underloaded, migratable vm found");
            return new int[0];
        }

        int[] tiers = readSpace.getMipsTiers();
        int currentIndex = findTierIndex(tiers, readSpace.getVmMips(target));
        if (currentIndex <= 0) {
            Log.printlnConcat(readSpace.getNow(), ": [" + MODULE_NAME + "] vm " + readSpace.getId(target) + " already at lowest known mips tier or tier unresolved");
            return new int[0];
        }

        int targetIndex = currentIndex - 1;
        int vmId = readSpace.getId(target);
        Log.printlnConcat(readSpace.getNow(), ": [" + MODULE_NAME + "] scaling vm " + vmId + " down to mips tier index " + targetIndex);
        return new int[]{vmId, targetIndex};
    }

    private int findTierIndex(int[] tiers, double value) {
        for (int i = 0; i < tiers.length; i++) {
            if (Math.abs((double) tiers[i] - value) < 0.5) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public String inputSemantic() {
        return "vm-cpu-utilisation-mean-idle";
    }

    @Override
    public String outputSemantic() {
        return "scale-mips-down-idle-vm";
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
