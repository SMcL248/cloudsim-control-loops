package org.cloudbus.cloudsim.examples;// always include

import java.util.List;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;

/**
 * planner_v4
 *
 * Strategy: "Conservative right-sizing of idle VMs"
 * VM-level diagnosis. Among VMs flagged UNDERLOADED that hold more than one
 * PE (so deallocation cannot strand them at zero processing capacity), and
 * that are neither migrating nor mid-instantiation, selects the one with
 * the lowest CPU utilisation and requests one PE be deallocated from it.
 * Emits requestPeDeallocation{vmId}, or an empty array if no VM qualifies.
 */
public class planner_v4 implements Planner<LoadState[], int[]> {

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {

        List<GuestEntity> vms = readSpace.getVmList();
        int limit = Math.min(diagnosis.length, vms.size());

        GuestEntity idlestVm = null;
        double lowestUtil = Double.MAX_VALUE;

        for (int i = 0; i < limit; i++) {
            if (diagnosis[i] != LoadState.UNDERLOADED) {
                continue;
            }
            GuestEntity vm = vms.get(i);
            if (readSpace.isVmMigrating(vm) || readSpace.isVmBeingInstantiated(vm)) {
                continue;
            }
            if (readSpace.getVmNumberOfPes(vm) <= 1) {
                // Keep at least one PE so the VM can still make progress.
                continue;
            }

            double util = readSpace.getVmCpuUtil(vm);
            if (util < lowestUtil) {
                lowestUtil = util;
                idlestVm = vm;
            }
        }

        if (idlestVm == null) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v4] ",
                    "no underloaded vm eligible for pe deallocation");
            return new int[0];
        }

        int vmId = readSpace.getId(idlestVm);
        Log.printlnConcat(readSpace.getNow(), ": [planner_v4] ",
                "deallocating one pe from underloaded vm " + vmId);
        return new int[] { vmId };
    }

    @Override
    public String inputSemantic() {
        return "vm-loadstate-classification";
    }

    @Override
    public String outputSemantic() {
        return "vm-pe-deallocate";
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
