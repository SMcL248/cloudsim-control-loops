package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;

// State-Aware Idempotency Executor for requestBwScaling (3007).
// Skips the call entirely when the VM is already at the requested
// bandwidth tier, avoiding a redundant scaling request.
public class executor_v14 implements Executor<int[]> {

    private int successfulActionCount = 0;

    @Override
    public boolean execute(int[] actions, ActionSpace actionSpace) {
        if (actions == null || actions.length < 2) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v14] ", "Malformed payload, expected {vmId, tierIndex}");
            return false;
        }

        int vmId = actions[0];
        int tierIndex = actions[1];

        GuestEntity vm = actionSpace.getVmById(vmId);
        if (vm == null) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v14] ", "Rejected scaling, unknown vm=" + vmId);
            return false;
        }

        int[] tiers = actionSpace.getBwTiers();
        if (tierIndex < 0 || tierIndex >= tiers.length) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v14] ", "Rejected scaling, tierIndex=" + tierIndex + " out of range for vm=" + vmId);
            return false;
        }

        double targetBw = tiers[tierIndex];
        double currentBw = actionSpace.getVmBw(vm);
        if (currentBw == targetBw) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v14] ", "Skipped scaling, vm=" + vmId + " already at bw=" + targetBw);
            return false;
        }

        boolean ok = actionSpace.requestBwScaling(vm, targetBw);
        if (ok) {
            successfulActionCount++;
        }
        Log.printlnConcat(actionSpace.getNow(), ": [executor_v14] ", "Requested bw scaling of vm=" + vmId + " to " + targetBw + " success=" + ok);
        return true;
    }

    @Override
    public String inputSemantic() {
        return "requestBwScaling: scale a VM's bandwidth to a given tier";
    }

    @Override
    public int inputGuid() {
        return 3007;
    }

    @Override
    public int getSuccessfulActionCount() {
        return successfulActionCount;
    }
}
