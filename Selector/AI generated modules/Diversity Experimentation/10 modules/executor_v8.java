package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;

// Variant angle: requestPeDeallocation, guarded by refusing to strip a VM down
// to zero PEs (checks getVmNumberOfPes(vm) > 1 first), which a naive passthrough
// would allow and which would leave the VM unable to process any workload.
public class executor_v8 implements Executor<int[]> {

    private int successfulActionCount = 0;

    @Override
    public boolean execute(int[] actions, ActionSpace actionSpace) {
        double now = actionSpace.getNow();

        if (actions == null || actions.length != 1) {
            Log.printlnConcat(now, ": [executor_v8] REJECTED malformed payload, expected {vmId}");
            return false;
        }

        int vmId = actions[0];
        GuestEntity vm = actionSpace.getVmById(vmId);

        if (vm == null) {
            Log.printlnConcat(now, ": [executor_v8] REJECTED PE deallocation, VM ", vmId, " could not be resolved");
            return false;
        }

        if (actionSpace.getVmNumberOfPes(vm) <= 1) {
            Log.printlnConcat(now, ": [executor_v8] SKIPPED PE deallocation, VM ", vmId, " would be left with zero PEs");
            return false;
        }

        boolean succeeded = actionSpace.requestPeDeallocation(vm);
        if (succeeded) {
            successfulActionCount++;
        }

        Log.printlnConcat(now, ": [executor_v8] ATTEMPTED requestPeDeallocation vm=", vmId, " succeeded=", succeeded);
        return true;
    }

    @Override
    public String inputSemantic() {
        return "deallocate PE from VM";
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
