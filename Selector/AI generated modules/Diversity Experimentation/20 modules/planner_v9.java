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
 * Strategy: priority RAM scaling for the heaviest overloaded workload.
 * Among VMs flagged OVERLOADED, trusts the diagnosis at face value and
 * rewards the VM with the largest existing RAM footprint - the heaviest
 * workload already on the system - with the next RAM tier, on the
 * assumption that the biggest workload is the one whose failure would be
 * most costly.
 */
public class planner_v9 implements Planner<LoadState[], int[]> {

    private static final String MODULE_NAME = "planner_v9";

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {
        List<GuestEntity> vms = readSpace.getVmList();

        GuestEntity target = null;
        double largestRam = -1.0;
        for (int i = 0; i < diagnosis.length && i < vms.size(); i++) {
            if (diagnosis[i] != LoadState.OVERLOADED) {
                continue;
            }
            GuestEntity vm = vms.get(i);
            if (readSpace.isVmMigrating(vm) || readSpace.isVmBeingInstantiated(vm)) {
                continue;
            }
            double ram = readSpace.getVmRam(vm);
            if (ram > largestRam) {
                largestRam = ram;
                target = vm;
            }
        }

        if (target == null) {
            Log.printlnConcat(readSpace.getNow(), ": [" + MODULE_NAME + "] no overloaded, migratable vm found");
            return new int[0];
        }

        double nextTier = readSpace.getNextRamTier(target);
        if (nextTier < 0.0) {
            Log.printlnConcat(readSpace.getNow(), ": [" + MODULE_NAME + "] vm " + readSpace.getId(target) + " already at max ram tier");
            return new int[0];
        }

        int tierIndex = findTierIndex(readSpace.getRamTiers(), nextTier);
        if (tierIndex < 0) {
            Log.printlnConcat(readSpace.getNow(), ": [" + MODULE_NAME + "] next ram tier for vm " + readSpace.getId(target) + " did not resolve to a known tier index");
            return new int[0];
        }

        int vmId = readSpace.getId(target);
        Log.printlnConcat(readSpace.getNow(), ": [" + MODULE_NAME + "] scaling vm " + vmId + " up to ram tier index " + tierIndex);
        return new int[]{vmId, tierIndex};
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
        return "vm-ram-footprint-priority";
    }

    @Override
    public String outputSemantic() {
        return "scale-ram-up-pressured-vm";
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
