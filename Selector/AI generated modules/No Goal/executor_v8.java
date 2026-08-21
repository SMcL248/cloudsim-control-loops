package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;

// Strategy: PE-floor-protected deallocation.
// Dropping a VM to zero PEs would silently strand any workload still
// running on it, so this variant refuses to deallocate when the VM is
// already at a single PE, keeping a floor of 1 regardless of what the
// planner requested. Tallies boolean success.
public class executor_v8 implements Executor<int[]> {

    private static final int INPUT_GUID = 3009;
    private static final String INPUT_SEMANTIC = "deallocate a PE from a VM";

    private int successfulActionCount = 0;

    @Override
    public boolean execute(int[] actions, ActionSpace actionSpace) {
        if (!hasShape(actions, 1) || isSentinel(actions)) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v8] no-op or malformed payload received, skipping requestPeDeallocation");
            return false;
        }

        int vmId = actions[0];
        GuestEntity vm = actionSpace.getVmById(vmId);

        if (vm == null) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v8] VM ", vmId, " no longer exists, skipping PE deallocation");
            return false;
        }

        int currentPes = actionSpace.getVmNumberOfPes(vm);

        if (currentPes <= 1) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v8] VM ", vmId, " has only ", currentPes, " PE(s), refusing to deallocate below floor of 1");
            return false;
        }

        boolean succeeded = actionSpace.requestPeDeallocation(vm);

        if (succeeded) {
            successfulActionCount++;
        }

        Log.printlnConcat(actionSpace.getNow(), ": [executor_v8] requested PE deallocation for VM ", vmId, ", succeeded=", succeeded);
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
