package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;

public class executor_v4 implements Executor<int[]> {

    private int successfulActionCount = 0;

    @Override
    public boolean execute(int[] actions, ActionSpace actionSpace) {
        if (actions == null || actions.length != 1) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v4] Malformed payload for requestPeAllocation, expected 1 int, action not attempted.");
            return false;
        }

        int vmId = actions[0];
        GuestEntity vm = actionSpace.getVmById(vmId);

        if (vm == null) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v4] VM ", vmId, " could not be resolved, PE allocation not attempted.");
            return false;
        }

        Log.printlnConcat(actionSpace.getNow(), ": [executor_v4] Requesting PE allocation for VM ", vmId, ".");
        boolean success = actionSpace.requestPeAllocation(vm);

        if (success) {
            successfulActionCount++;
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v4] PE allocation for VM ", vmId, " succeeded.");
        } else {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v4] PE allocation for VM ", vmId, " was rejected by ActionSpace.");
        }

        return true;
    }

    @Override
    public String inputSemantic() {
        return "requestPeAllocation";
    }

    @Override
    public int inputGuid() {
        return 3008;
    }

    @Override
    public int getSuccessfulActionCount() {
        return successfulActionCount;
    }
}
