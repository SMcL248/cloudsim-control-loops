package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;

// Variant angle: requestPeAllocation, guarded by resolving the VM and skipping
// VMs currently mid-migration (allocating a PE mid-move is treated as unsafe
// here), and tracking the boolean success signal from ActionSpace.
public class executor_v7 implements Executor<int[]> {

    private int successfulActionCount = 0;

    @Override
    public boolean execute(int[] actions, ActionSpace actionSpace) {
        double now = actionSpace.getNow();

        if (actions == null || actions.length != 1) {
            Log.printlnConcat(now, ": [executor_v7] REJECTED malformed payload, expected {vmId}");
            return false;
        }

        int vmId = actions[0];
        GuestEntity vm = actionSpace.getVmById(vmId);

        if (vm == null) {
            Log.printlnConcat(now, ": [executor_v7] REJECTED PE allocation, VM ", vmId, " could not be resolved");
            return false;
        }

        if (actionSpace.isVmMigrating(vm)) {
            Log.printlnConcat(now, ": [executor_v7] SKIPPED PE allocation, VM ", vmId, " is currently migrating");
            return false;
        }

        boolean succeeded = actionSpace.requestPeAllocation(vm);
        if (succeeded) {
            successfulActionCount++;
        }

        Log.printlnConcat(now, ": [executor_v7] ATTEMPTED requestPeAllocation vm=", vmId, " succeeded=", succeeded);
        return true;
    }

    @Override
    public String inputSemantic() {
        return "allocate additional PE to VM";
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
