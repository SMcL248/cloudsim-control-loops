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
 * Strategy: majority-overload fleet expansion.
 * Rather than reacting to a single hotspot, this planner counts how many
 * VMs are flagged OVERLOADED. Only once a strict majority of the fleet is
 * struggling does it request a brand-new, mid-tier VM in the datacenter of
 * the most heavily-loaded existing VM, treating isolated overload as
 * noise and systemic overload as a signal to grow capacity.
 */
public class planner_v12 implements Planner<LoadState[], int[]> {

    private static final String MODULE_NAME = "planner_v12";

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {
        List<GuestEntity> vms = readSpace.getVmList();

        if (vms.isEmpty()) {
            Log.printlnConcat(readSpace.getNow(), ": [" + MODULE_NAME + "] no vms exist yet, skipping fleet expansion check");
            return new int[0];
        }

        int overloadedCount = 0;
        GuestEntity worst = null;
        double worstMips = -1.0;
        int limit = Math.min(diagnosis.length, vms.size());
        for (int i = 0; i < limit; i++) {
            if (diagnosis[i] != LoadState.OVERLOADED) {
                continue;
            }
            overloadedCount++;
            GuestEntity vm = vms.get(i);
            double requested = readSpace.getVmRequestedMips(vm);
            if (requested > worstMips) {
                worstMips = requested;
                worst = vm;
            }
        }

        if (overloadedCount * 2 <= limit || worst == null) {
            Log.printlnConcat(readSpace.getNow(), ": [" + MODULE_NAME + "] overload not systemic (" + overloadedCount + "/" + limit + "), no expansion triggered");
            return new int[0];
        }

        Integer datacenterId = readSpace.getDatacenterFor(readSpace.getId(worst));
        if (datacenterId == null) {
            Log.printlnConcat(readSpace.getNow(), ": [" + MODULE_NAME + "] could not resolve datacenter for reference vm " + readSpace.getId(worst));
            return new int[0];
        }

        int[] mipsTiers = readSpace.getMipsTiers();
        int[] ramTiers = readSpace.getRamTiers();
        int tierIndex = mipsTiers.length / 2;
        int sizeTierIndex = ramTiers.length / 2;

        Log.printlnConcat(readSpace.getNow(), ": [" + MODULE_NAME + "] systemic overload (" + overloadedCount + "/" + limit + "), requesting new mid-tier vm in datacenter " + datacenterId);
        return new int[]{tierIndex, sizeTierIndex, datacenterId};
    }

    @Override
    public String inputSemantic() {
        return "vm-fleet-cpu-load-systemic";
    }

    @Override
    public String outputSemantic() {
        return "create-vm-majority-overload-response";
    }

    @Override
    public int inputGuid() {
        return 2300;
    }

    @Override
    public int outputGuid() {
        return 3003;
    }
}
