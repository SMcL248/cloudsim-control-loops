package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;

// Strict Validation Executor for requestBwScaling (3007).
// Rejects the instruction outright when the tier index is out of range,
// making no attempt to repair it.
public class executor_v13 implements Executor<int[]> {

    private int successfulActionCount = 0;

    @Override
    public boolean execute(int[] actions, ActionSpace actionSpace) {
        if (actions == null || actions.length < 2) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v13] ", "Malformed payload, expected {vmId, tierIndex}");
            return false;
        }

        int vmId = actions[0];
        int tierIndex = actions[1];

        GuestEntity vm = actionSpace.getVmById(vmId);
        if (vm == null) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v13] ", "Rejected scaling, unknown vm=" + vmId);
            return false;
        }

        int[] tiers = actionSpace.getBwTiers();
        if (tierIndex < 0 || tierIndex >= tiers.length) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v13] ", "Rejected scaling, tierIndex=" + tierIndex + " out of range for vm=" + vmId);
            return false;
        }

        double newBw = tiers[tierIndex];
        boolean ok = actionSpace.requestBwScaling(vm, newBw);
        if (ok) {
            successfulActionCount++;
        }
        Log.printlnConcat(actionSpace.getNow(), ": [executor_v13] ", "Requested bw scaling of vm=" + vmId + " to " + newBw + " success=" + ok);
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
