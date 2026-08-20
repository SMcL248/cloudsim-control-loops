package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;

public class executor_v30 implements Executor<int[]> {

    private static final int MAX_ATTEMPTS = 3;

    private int successfulActionCount = 0;

    @Override
    public boolean execute(int[] payload, ActionSpace actionSpace) {
        if (payload == null || payload.length != 1) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v30] ",
                    "malformed payload, expected {vmId}");
            return false;
        }

        int vmId = payload[0];
        GuestEntity vm = actionSpace.getVmById(vmId);
        if (vm == null) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v30] ",
                    "unresolved VM reference for id " + vmId);
            return false;
        }

        boolean succeeded = false;
        int attempt = 0;
        while (attempt < MAX_ATTEMPTS && !succeeded) {
            attempt++;
            succeeded = actionSpace.requestPeAllocation(vm);
            if (!succeeded) {
                Log.printlnConcat(actionSpace.getNow(), ": [executor_v30] ",
                        "requestPeAllocation attempt " + attempt + " of " + MAX_ATTEMPTS + " failed for VM " + vmId);
            }
        }

        if (succeeded) {
            successfulActionCount++;
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v30] ",
                    "requestPeAllocation succeeded on attempt " + attempt + " for VM " + vmId);
        } else {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v30] ",
                    "requestPeAllocation exhausted " + MAX_ATTEMPTS + " attempts for VM " + vmId);
        }

        return true;
    }

    @Override
    public String inputSemantic() {
        return "Allocate a PE to a VM";
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
