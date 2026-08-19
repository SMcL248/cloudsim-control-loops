package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;

public class executor_v3 implements Executor<int[]> {

    private int successfulActionCount = 0;

    @Override
    public boolean execute(int[] actions, ActionSpace actionSpace) {
        if (actions == null || actions.length != 2) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v3] Malformed payload for requestMipsScaling, expected 2 ints, action not attempted.");
            return false;
        }

        int vmId = actions[0];
        int tierIndex = actions[1];

        GuestEntity vm = actionSpace.getVmById(vmId);
        if (vm == null) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v3] VM ", vmId, " could not be resolved, MIPS scaling not attempted.");
            return false;
        }

        int[] mipsTiers = actionSpace.getMipsTiers();
        if (tierIndex < 0 || tierIndex >= mipsTiers.length) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v3] Tier index ", tierIndex, " out of range for VM ", vmId, ", MIPS scaling not attempted.");
            return false;
        }

        double newValue = mipsTiers[tierIndex];
        Log.printlnConcat(actionSpace.getNow(), ": [executor_v3] Requesting MIPS scaling of VM ", vmId, " to tier value ", newValue, ".");
        boolean success = actionSpace.requestMipsScaling(vm, newValue);

        if (success) {
            successfulActionCount++;
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v3] MIPS scaling of VM ", vmId, " succeeded.");
        } else {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v3] MIPS scaling of VM ", vmId, " was rejected by ActionSpace.");
        }

        return true;
    }

    @Override
    public String inputSemantic() {
        return "requestMipsScaling";
    }

    @Override
    public int inputGuid() {
        return 3005;
    }

    @Override
    public int getSuccessfulActionCount() {
        return successfulActionCount;
    }
}
