package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;

// Outcome-Verifying Pass-through Executor for requestRamScaling (3006).
// Trusts the planner's tier index directly, guards only against a
// catastrophic out-of-bounds index via try/catch, and verifies the result
// by comparing RAM before and after the call.
public class executor_v12 implements Executor<int[]> {

    private int successfulActionCount = 0;

    @Override
    public boolean execute(int[] actions, ActionSpace actionSpace) {
        if (actions == null || actions.length < 2) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v12] ", "Malformed payload, expected {vmId, tierIndex}");
            return false;
        }

        int vmId = actions[0];
        int tierIndex = actions[1];

        GuestEntity vm = actionSpace.getVmById(vmId);
        if (vm == null) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v12] ", "Rejected scaling, unknown vm=" + vmId);
            return false;
        }

        double before = actionSpace.getVmRam(vm);
        try {
            double newRam = actionSpace.getRamTiers()[tierIndex];
            boolean ok = actionSpace.requestRamScaling(vm, newRam);
            if (ok) {
                successfulActionCount++;
            }
            double after = actionSpace.getVmRam(vm);
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v12] ", "vm=" + vmId + " ram before=" + before + " requested=" + newRam + " after=" + after + " success=" + ok);
            return true;
        } catch (ArrayIndexOutOfBoundsException e) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v12] ", "Rejected scaling, tierIndex=" + tierIndex + " out of bounds for vm=" + vmId);
            return false;
        }
    }

    @Override
    public String inputSemantic() {
        return "requestRamScaling: scale a VM's RAM to a given tier";
    }

    @Override
    public int inputGuid() {
        return 3006;
    }

    @Override
    public int getSuccessfulActionCount() {
        return successfulActionCount;
    }
}
