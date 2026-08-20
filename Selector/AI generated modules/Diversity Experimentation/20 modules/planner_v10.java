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
 * Strategy: concurrency-driven bandwidth scale-up.
 * Among VMs flagged OVERLOADED, uses the number of cloudlets currently
 * co-resident on the VM as a proxy for network/IO contention (more
 * concurrent workloads sharing one link) and bumps the busiest-by-count
 * VM to its next bandwidth tier.
 */
public class planner_v10 implements Planner<LoadState[], int[]> {

    private static final String MODULE_NAME = "planner_v10";

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {
        List<GuestEntity> vms = readSpace.getVmList();

        GuestEntity target = null;
        int mostCloudlets = -1;
        for (int i = 0; i < diagnosis.length && i < vms.size(); i++) {
            if (diagnosis[i] != LoadState.OVERLOADED) {
                continue;
            }
            GuestEntity vm = vms.get(i);
            if (readSpace.isVmMigrating(vm) || readSpace.isVmBeingInstantiated(vm)) {
                continue;
            }
            int count = readSpace.getVmCloudletList(vm).size();
            if (count > mostCloudlets) {
                mostCloudlets = count;
                target = vm;
            }
        }

        if (target == null) {
            Log.printlnConcat(readSpace.getNow(), ": [" + MODULE_NAME + "] no overloaded, migratable vm found");
            return new int[0];
        }

        double nextTier = readSpace.getNextBwTier(target);
        if (nextTier < 0.0) {
            Log.printlnConcat(readSpace.getNow(), ": [" + MODULE_NAME + "] vm " + readSpace.getId(target) + " already at max bandwidth tier");
            return new int[0];
        }

        int tierIndex = findTierIndex(readSpace.getBwTiers(), nextTier);
        if (tierIndex < 0) {
            Log.printlnConcat(readSpace.getNow(), ": [" + MODULE_NAME + "] next bandwidth tier for vm " + readSpace.getId(target) + " did not resolve to a known tier index");
            return new int[0];
        }

        int vmId = readSpace.getId(target);
        Log.printlnConcat(readSpace.getNow(), ": [" + MODULE_NAME + "] scaling vm " + vmId + " up to bandwidth tier index " + tierIndex);
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
        return "vm-cloudlet-concurrency-io-pressure";
    }

    @Override
    public String outputSemantic() {
        return "scale-bw-up-io-bound-vm";
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
