package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;

// Executor for ActionSpace.requestPeDeallocation - GUID suffix 09.
public class executor_v8 implements Executor<int[]> {

    private static final int GUID = 3009;
    private static final String SEMANTIC =
        "requestPeDeallocation: release one processing element (PE) from a VM. Payload {vmId}.";

    private int successfulActions = 0;

    @Override
    public boolean execute(int[] actions, ActionSpace actionSpace) {
        double now = actionSpace.getNow();

        if (actions == null || actions.length != 1) {
            Log.printlnConcat(now, ": [executor_v8] rejected - payload must have exactly 1 entry {vmId}.");
            return false;
        }

        if (actions[0] == -1) {
            Log.printlnConcat(now, ": [executor_v8] no-op sentinel received, no PE deallocation requested.");
            return false;
        }

        int vmId = actions[0];
        GuestEntity vm = actionSpace.getVmById(vmId);

        if (vm == null) {
            Log.printlnConcat(now, ": [executor_v8] rejected - VM ", vmId, " could not be resolved.");
            return false;
        }

        if (actionSpace.getVmNumberOfPes(vm) <= 1) {
            Log.printlnConcat(now, ": [executor_v8] rejected - VM ", vmId, " only has 1 PE remaining, deallocation would strand its workload.");
            return false;
        }

        boolean succeeded = actionSpace.requestPeDeallocation(vm);
        if (succeeded) {
            successfulActions++;
            Log.printlnConcat(now, ": [executor_v8] deallocated one PE from VM ", vmId, ".");
        } else {
            Log.printlnConcat(now, ": [executor_v8] attempted PE deallocation on VM ", vmId, " but the request was refused.");
        }
        return true;
    }

    @Override
    public String inputSemantic() {
        return SEMANTIC;
    }

    @Override
    public int inputGuid() {
        return GUID;
    }

    @Override
    public int getSuccessfulActionCount() {
        return successfulActions;
    }
}
