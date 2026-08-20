package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;

// Guard-Rail Executor for requestPeDeallocation (3009).
// Refuses to deallocate a VM's last remaining PE, since that would leave
// the VM with zero processing capacity.
public class executor_v17 implements Executor<int[]> {

    private int successfulActionCount = 0;

    @Override
    public boolean execute(int[] actions, ActionSpace actionSpace) {
        if (actions == null || actions.length < 1) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v17] ", "Malformed payload, expected {vmId}");
            return false;
        }

        int vmId = actions[0];
        GuestEntity vm = actionSpace.getVmById(vmId);
        if (vm == null) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v17] ", "Rejected deallocation, unknown vm=" + vmId);
            return false;
        }

        if (actionSpace.getVmNumberOfPes(vm) <= 1) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v17] ", "Refused deallocation, vm=" + vmId + " would be left with zero processing elements");
            return false;
        }

        boolean ok = actionSpace.requestPeDeallocation(vm);
        if (ok) {
            successfulActionCount++;
        }
        Log.printlnConcat(actionSpace.getNow(), ": [executor_v17] ", "Requested pe deallocation for vm=" + vmId + " success=" + ok);
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
