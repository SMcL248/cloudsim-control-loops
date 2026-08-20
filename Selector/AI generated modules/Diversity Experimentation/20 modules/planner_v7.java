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
 * Strategy: capacity-ratio driven MIPS scale-up.
 * Among VMs flagged OVERLOADED, ranks candidates by how large their
 * requested MIPS already is relative to their host's per-PE MIPS rating -
 * a proxy for how close the VM is to saturating what its host can give a
 * single PE. The most constrained candidate is bumped to its next MIPS
 * tier.
 */
public class planner_v7 implements Planner<LoadState[], int[]> {

    private static final String MODULE_NAME = "planner_v7";

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {
        List<GuestEntity> vms = readSpace.getVmList();

        GuestEntity target = null;
        double highestRatio = -1.0;
        for (int i = 0; i < diagnosis.length && i < vms.size(); i++) {
            if (diagnosis[i] != LoadState.OVERLOADED) {
                continue;
            }
            GuestEntity vm = vms.get(i);
            if (readSpace.isVmMigrating(vm) || readSpace.isVmBeingInstantiated(vm)) {
                continue;
            }
            double hostCapacity = readSpace.getHostCapacity(vm);
            if (hostCapacity <= 0.0) {
                continue;
            }
            double ratio = readSpace.getVmRequestedMips(vm) / hostCapacity;
            if (ratio > highestRatio) {
                highestRatio = ratio;
                target = vm;
            }
        }

        if (target == null) {
            Log.printlnConcat(readSpace.getNow(), ": [" + MODULE_NAME + "] no overloaded, migratable vm with valid host capacity found");
            return new int[0];
        }

        double nextTier = readSpace.getNextMipsTier(target);
        if (nextTier < 0.0) {
            Log.printlnConcat(readSpace.getNow(), ": [" + MODULE_NAME + "] vm " + readSpace.getId(target) + " already at max mips tier");
            return new int[0];
        }

        int tierIndex = findTierIndex(readSpace.getMipsTiers(), nextTier);
        if (tierIndex < 0) {
            Log.printlnConcat(readSpace.getNow(), ": [" + MODULE_NAME + "] next mips tier for vm " + readSpace.getId(target) + " did not resolve to a known tier index");
            return new int[0];
        }

        int vmId = readSpace.getId(target);
        Log.printlnConcat(readSpace.getNow(), ": [" + MODULE_NAME + "] scaling vm " + vmId + " up to mips tier index " + tierIndex);
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
        return "vm-mips-capacity-pressure";
    }

    @Override
    public String outputSemantic() {
        return "scale-mips-up-capacity-constrained-vm";
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
