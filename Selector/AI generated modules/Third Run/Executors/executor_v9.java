package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;

// Executes the requestPeDeallocation action: removes one processing element (PE) from a VM.
public class executor_v9 implements Executor<int[]> {

    private int successfulActionCount = 0;

    @Override
    public boolean execute(int[] actions, ActionSpace actionSpace) {
        if (actions == null || actions.length != 1) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v9] invalid payload, expected {vmId}");
            return false;
        }

        int vmId = actions[0];
        GuestEntity vm = actionSpace.getVmById(vmId);

        if (vm == null) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v9] cannot resolve vm ", vmId, ", aborting");
            return false;
        }

        boolean deallocated = actionSpace.requestPeDeallocation(vm);

        if (deallocated) {
            successfulActionCount++;
        }

        Log.printlnConcat(actionSpace.getNow(), ": [executor_v9] requested pe deallocation for vm ", vmId, " -- success: ", deallocated);
        return true;
    }

    @Override
    public String inputSemantic() {
        return "requestPeDeallocation(vmId)";
    }

    @Override
    public int inputGuid() {
        return 3009;
    }

    @Override
    public int getSuccessfulActionCount() {
        return successfulActionCount;
    }
}
