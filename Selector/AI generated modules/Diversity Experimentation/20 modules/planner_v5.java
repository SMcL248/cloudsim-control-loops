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
 * Strategy: volatility-aware PE headroom grant.
 * Among VMs flagged OVERLOADED, targets the one with the highest rolling
 * utilisation MAD (spikiest, most unpredictable demand) rather than the
 * one with the highest mean load. Bursty VMs benefit more from an extra
 * PE of headroom than steadily-loaded ones, which are better served by
 * other scaling actions.
 */
public class planner_v5 implements Planner<LoadState[], int[]> {

    private static final String MODULE_NAME = "planner_v5";

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {
        List<GuestEntity> vms = readSpace.getVmList();

        GuestEntity target = null;
        double highestMad = -1.0;
        for (int i = 0; i < diagnosis.length && i < vms.size(); i++) {
            if (diagnosis[i] != LoadState.OVERLOADED) {
                continue;
            }
            GuestEntity vm = vms.get(i);
            if (readSpace.isVmMigrating(vm) || readSpace.isVmBeingInstantiated(vm)) {
                continue;
            }
            double mad = readSpace.getVmUtilizationMad(vm);
            if (mad > highestMad) {
                highestMad = mad;
                target = vm;
            }
        }

        if (target == null) {
            Log.printlnConcat(readSpace.getNow(), ": [" + MODULE_NAME + "] no overloaded, migratable vm found");
            return new int[0];
        }

        int vmId = readSpace.getId(target);
        Log.printlnConcat(readSpace.getNow(), ": [" + MODULE_NAME + "] allocating extra pe to vm " + vmId + " with utilisation mad " + highestMad);
        return new int[]{vmId};
    }

    @Override
    public String inputSemantic() {
        return "vm-cpu-utilisation-volatility";
    }

    @Override
    public String outputSemantic() {
        return "allocate-pe-highest-volatility-vm";
    }

    @Override
    public int inputGuid() {
        return 2300;
    }

    @Override
    public int outputGuid() {
        return 3008;
    }
}
