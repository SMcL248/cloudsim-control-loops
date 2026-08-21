package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;

// GUID 3007 -- requestBwScaling
// Strategy: tier-resolving scaler with no-op suppression, mirroring the MIPS
// and RAM scalers but for bandwidth, which matters most for data-intensive
// or migration-heavy workloads where network headroom gates throughput.
public class executor_v7 implements Executor<int[]> {

    private static final int GUID = 3007;
    private int successCount = 0;

    @Override
    public boolean execute(int[] action, ActionSpace actionSpace) {
        if (action == null || action.length != 2) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v7] Malformed payload for requestBwScaling, expected 2 ints, aborting.");
            return false;
        }
        if (isSentinel(action)) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v7] No prescribed action this cycle.");
            return false;
        }

        int vmId = action[0];
        int tierIndex = action[1];

        GuestEntity vm = actionSpace.getVmById(vmId);
        if (vm == null) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v7] Unknown VM reference " + vmId + ", aborting BW scaling.");
            return false;
        }

        int[] bwTiers = actionSpace.getBwTiers();
        if (tierIndex < 0 || bwTiers == null || tierIndex >= bwTiers.length) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v7] Tier index " + tierIndex + " out of range for VM " + vmId + ", aborting.");
            return false;
        }

        double newBw = bwTiers[tierIndex];
        if (newBw == actionSpace.getVmBw(vm)) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v7] VM " + vmId + " already at requested BW tier, skipping no-op scaling.");
            return false;
        }

        boolean success = actionSpace.requestBwScaling(vm, newBw);
        if (success) {
            successCount++;
        }
        Log.printlnConcat(actionSpace.getNow(), ": [executor_v7] Requested BW scaling of VM " + vmId + " to " + newBw + " (tier " + tierIndex + "), success=" + success);
        return true;
    }

    private boolean isSentinel(int[] a) {
        for (int v : a) {
            if (v != -1) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String inputSemantic() {
        return "Scale a VM's bandwidth to a specified tier to relieve network-bound throughput constraints";
    }

    @Override
    public int inputGuid() {
        return GUID;
    }

    @Override
    public int getSuccessfulActionCount() {
        return successCount;
    }
}
