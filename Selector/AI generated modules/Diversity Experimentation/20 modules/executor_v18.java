package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;

// Trusting Pass-through Executor for requestPeDeallocation (3009).
// Dispatches directly without inspecting current PE count, relying on
// ActionSpace's own boolean result as the success signal.
public class executor_v18 implements Executor<int[]> {

    private int successfulActionCount = 0;

    @Override
    public boolean execute(int[] actions, ActionSpace actionSpace) {
        if (actions == null || actions.length < 1) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v18] ", "Malformed payload, expected {vmId}");
            return false;
        }

        int vmId = actions[0];
        GuestEntity vm = actionSpace.getVmById(vmId);
        if (vm == null) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v18] ", "Rejected deallocation, unknown vm=" + vmId);
            return false;
        }

        boolean ok = actionSpace.requestPeDeallocation(vm);
        if (ok) {
            successfulActionCount++;
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v18] ", "Deallocated pe for vm=" + vmId + " now numPes=" + actionSpace.getVmNumberOfPes(vm));
        } else {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v18] ", "Deallocation attempt for vm=" + vmId + " returned false");
        }
        return true;
    }

    @Override
    public String inputSemantic() {
        return "requestPeDeallocation: deallocate a PE from a VM";
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
