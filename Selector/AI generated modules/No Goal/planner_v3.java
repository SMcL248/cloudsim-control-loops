package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.core.GuestEntity;
import java.util.List;

/**
 * Variant 3: Opportunistic cloudlet-level offload.
 * Strategy: read cloudlet-level diagnosis. For an OVERLOADED cloudlet,
 * locate its owning VM, then move the cloudlet directly onto whichever VM
 * in the fleet currently reports the lowest CPU utilisation - a fine-grained,
 * workload-centric rebalancing that bypasses host/VM migration machinery
 * entirely.
 */
public class planner_v3 implements Planner<LoadState[], int[]> {

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {
        List<Cloudlet> cloudlets = readSpace.getActiveCloudlets();
        List<GuestEntity> vms = readSpace.getVmList();
        int considered = Math.min(diagnosis.length, cloudlets.size());

        for (int i = 0; i < considered; i++) {
            if (diagnosis[i] != LoadState.OVERLOADED) {
                continue;
            }
            Cloudlet cl = cloudlets.get(i);

            GuestEntity source = null;
            for (GuestEntity vm : vms) {
                if (readSpace.getVmCloudletList(vm).contains(cl)) {
                    source = vm;
                    break;
                }
            }
            if (source == null) {
                continue;
            }

            GuestEntity lightest = null;
            double lightestUtil = Double.MAX_VALUE;
            for (GuestEntity vm : vms) {
                if (readSpace.getId(vm) == readSpace.getId(source)) {
                    continue;
                }
                if (readSpace.isVmMigrating(vm) || readSpace.isVmBeingInstantiated(vm)) {
                    continue;
                }
                double util = readSpace.getVmCpuUtil(vm);
                if (util < lightestUtil) {
                    lightestUtil = util;
                    lightest = vm;
                }
            }

            if (lightest != null) {
                int clId = readSpace.getId(cl);
                int fromId = readSpace.getId(source);
                int toId = readSpace.getId(lightest);
                Log.printlnConcat(readSpace.getNow(), ": [planner_v3] ",
                        "Offloading overloaded cloudlet " + clId + " from VM " + fromId
                                + " to lowest-utilization VM " + toId);
                return new int[]{clId, fromId, toId};
            }
        }

        Log.printlnConcat(readSpace.getNow(), ": [planner_v3] ",
                "No opportunistic offload target found.");
        return new int[]{-1, -1, -1};
    }

    @Override
    public String inputSemantic() {
        return "cloudlet-loadstate-overload-offload";
    }

    @Override
    public String outputSemantic() {
        return "cloudlet-move";
    }

    @Override
    public int inputGuid() {
        return 2400;
    }

    @Override
    public int outputGuid() {
        return 3001;
    }
}
