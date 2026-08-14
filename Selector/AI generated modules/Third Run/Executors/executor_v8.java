package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;

// Executes the requestPeAllocation action: allocates one additional processing element (PE) to a VM.
public class executor_v8 implements Executor<int[]> {

    private int successfulActionCount = 0;

    @Override
    public boolean execute(int[] actions, ActionSpace actionSpace) {
        if (actions == null || actions.length != 1) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v8] invalid payload, expected {vmId}");
            return false;
        }

        int vmId = actions[0];
        GuestEntity vm = actionSpace.getVmById(vmId);

        if (vm == null) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v8] cannot resolve vm ", vmId, ", aborting");
            return false;
        }

        boolean allocated = actionSpace.requestPeAllocation(vm);

        if (allocated) {
            successfulActionCount++;
        }

        Log.printlnConcat(actionSpace.getNow(), ": [executor_v8] requested pe allocation for vm ", vmId, " -- success: ", allocated);
        return true;
    }

    @Override
    public String inputSemantic() {
        return "requestPeAllocation(vmId)";
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
