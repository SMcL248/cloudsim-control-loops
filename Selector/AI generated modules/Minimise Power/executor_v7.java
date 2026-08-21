package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;

// Executor for ActionSpace.requestPeAllocation - GUID suffix 08.
public class executor_v7 implements Executor<int[]> {

    private static final int GUID = 3008;
    private static final String SEMANTIC =
        "requestPeAllocation: grant one additional processing element (PE) to a VM. Payload {vmId}.";

    private int successfulActions = 0;

    @Override
    public boolean execute(int[] actions, ActionSpace actionSpace) {
        double now = actionSpace.getNow();

        if (actions == null || actions.length != 1) {
            Log.printlnConcat(now, ": [executor_v7] rejected - payload must have exactly 1 entry {vmId}.");
            return false;
        }

        if (actions[0] == -1) {
            Log.printlnConcat(now, ": [executor_v7] no-op sentinel received, no PE allocation requested.");
            return false;
        }

        int vmId = actions[0];
        GuestEntity vm = actionSpace.getVmById(vmId);

        if (vm == null) {
            Log.printlnConcat(now, ": [executor_v7] rejected - VM ", vmId, " could not be resolved.");
            return false;
        }

        if (actionSpace.isVmBeingInstantiated(vm)) {
            Log.printlnConcat(now, ": [executor_v7] rejected - VM ", vmId, " has not yet been placed on a host.");
            return false;
        }

        boolean succeeded = actionSpace.requestPeAllocation(vm);
        if (succeeded) {
            successfulActions++;
            Log.printlnConcat(now, ": [executor_v7] allocated an additional PE to VM ", vmId, ".");
        } else {
            Log.printlnConcat(now, ": [executor_v7] attempted PE allocation on VM ", vmId, " but the request was refused.");
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
