package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;

// GUID 3005 -- requestMipsScaling
// Strategy: tier-resolving scaler with no-op suppression. Resolves the
// requested tier index against getMipsTiers() so a malformed/out-of-range
// index is caught before touching ActionSpace, skips scaling requests that
// would leave the VM at its current MIPS value, and tracks true ActionSpace
// success via getSuccessfulActionCount() as this call returns boolean.
public class executor_v5 implements Executor<int[]> {

    private static final int GUID = 3005;
    private int successCount = 0;

    @Override
    public boolean execute(int[] action, ActionSpace actionSpace) {
        if (action == null || action.length != 2) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v5] Malformed payload for requestMipsScaling, expected 2 ints, aborting.");
            return false;
        }
        if (isSentinel(action)) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v5] No prescribed action this cycle.");
            return false;
        }

        int vmId = action[0];
        int tierIndex = action[1];

        GuestEntity vm = actionSpace.getVmById(vmId);
        if (vm == null) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v5] Unknown VM reference " + vmId + ", aborting MIPS scaling.");
            return false;
        }

        int[] mipsTiers = actionSpace.getMipsTiers();
        if (tierIndex < 0 || mipsTiers == null || tierIndex >= mipsTiers.length) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v5] Tier index " + tierIndex + " out of range for VM " + vmId + ", aborting.");
            return false;
        }

        double newValue = mipsTiers[tierIndex];
        if (newValue == actionSpace.getVmMips(vm)) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v5] VM " + vmId + " already at requested MIPS tier, skipping no-op scaling.");
            return false;
        }

        boolean success = actionSpace.requestMipsScaling(vm, newValue);
        if (success) {
            successCount++;
        }
        Log.printlnConcat(actionSpace.getNow(), ": [executor_v5] Requested MIPS scaling of VM " + vmId + " to " + newValue + " (tier " + tierIndex + "), success=" + success);
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
        return "Scale a VM's MIPS rating to a specified tier to relieve CPU-bound throughput bottlenecks";
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
