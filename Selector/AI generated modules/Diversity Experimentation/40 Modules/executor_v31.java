package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;

public class executor_v31 implements Executor<int[]> {

    private int successfulActionCount = 0;

    @Override
    public boolean execute(int[] payload, ActionSpace actionSpace) {
        if (payload == null || payload.length != 1) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v31] ",
                    "malformed payload, expected {vmId}");
            return false;
        }

        int vmId = payload[0];
        GuestEntity vm = actionSpace.getVmById(vmId);
        if (vm == null) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v31] ",
                    "unresolved VM reference for id " + vmId);
            return false;
        }

        boolean succeeded = actionSpace.requestPeDeallocation(vm);
        if (succeeded) {
            successfulActionCount++;
        }

        Log.printlnConcat(actionSpace.getNow(), ": [executor_v31] ",
                "issued requestPeDeallocation vm=" + vmId + " succeeded=" + succeeded);
        return true;
    }

    @Override
    public String inputSemantic() {
        return "Deallocate a PE from a VM";
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
