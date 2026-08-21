package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;

// Strategy: instantiation-aware PE allocation.
// A VM that is still being instantiated has not yet settled onto a host,
// so changing its PE topology now is meaningless (and may race the
// placement logic). This variant checks isVmBeingInstantiated() and
// declines to act until the VM is placed, then tallies boolean success.
public class executor_v7 implements Executor<int[]> {

    private static final int INPUT_GUID = 3008;
    private static final String INPUT_SEMANTIC = "allocate an additional PE to a VM";

    private int successfulActionCount = 0;

    @Override
    public boolean execute(int[] actions, ActionSpace actionSpace) {
        if (!hasShape(actions, 1) || isSentinel(actions)) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v7] no-op or malformed payload received, skipping requestPeAllocation");
            return false;
        }

        int vmId = actions[0];
        GuestEntity vm = actionSpace.getVmById(vmId);

        if (vm == null) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v7] VM ", vmId, " no longer exists, skipping PE allocation");
            return false;
        }

        if (actionSpace.isVmBeingInstantiated(vm)) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v7] VM ", vmId, " is still being instantiated, PE topology change is meaningless, skipping");
            return false;
        }

        boolean succeeded = actionSpace.requestPeAllocation(vm);

        if (succeeded) {
            successfulActionCount++;
        }

        Log.printlnConcat(actionSpace.getNow(), ": [executor_v7] requested PE allocation for VM ", vmId, ", succeeded=", succeeded);
        return true;
    }

    @Override
    public String inputSemantic() {
        return INPUT_SEMANTIC;
    }

    @Override
    public int inputGuid() {
        return INPUT_GUID;
    }

    @Override
    public int getSuccessfulActionCount() {
        return successfulActionCount;
    }

    private boolean hasShape(int[] a, int len) {
        return a != null && a.length == len;
    }

    private boolean isSentinel(int[] a) {
        for (int v : a) {
            if (v != -1) {
                return false;
            }
        }
        return true;
    }
}
