package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;

// Trusting Pass-through Executor for requestPeAllocation (3008).
// Dispatches directly without inspecting host state, relying on
// ActionSpace's own boolean result as the success signal, then logs the
// resulting PE count for confirmation.
public class executor_v16 implements Executor<int[]> {

    private int successfulActionCount = 0;

    @Override
    public boolean execute(int[] actions, ActionSpace actionSpace) {
        if (actions == null || actions.length < 1) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v16] ", "Malformed payload, expected {vmId}");
            return false;
        }

        int vmId = actions[0];
        GuestEntity vm = actionSpace.getVmById(vmId);
        if (vm == null) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v16] ", "Rejected allocation, unknown vm=" + vmId);
            return false;
        }

        boolean ok = actionSpace.requestPeAllocation(vm);
        if (ok) {
            successfulActionCount++;
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v16] ", "Allocated pe for vm=" + vmId + " now numPes=" + actionSpace.getVmNumberOfPes(vm));
        } else {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v16] ", "Allocation attempt for vm=" + vmId + " returned false, host may be saturated");
        }
        return true;
    }

    @Override
    public String inputSemantic() {
        return "requestPeAllocation: allocate an additional PE to a VM";
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
