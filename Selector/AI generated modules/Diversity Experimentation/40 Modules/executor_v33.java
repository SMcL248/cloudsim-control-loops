package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;

public class executor_v33 implements Executor<int[]> {

    private static final double UTIL_THRESHOLD = 0.2;

    private int successfulActionCount = 0;

    @Override
    public boolean execute(int[] payload, ActionSpace actionSpace) {
        if (payload == null || payload.length != 1) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v33] ",
                    "malformed payload, expected {vmId}");
            return false;
        }

        int vmId = payload[0];
        GuestEntity vm = actionSpace.getVmById(vmId);
        if (vm == null) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v33] ",
                    "unresolved VM reference for id " + vmId);
            return false;
        }

        double meanUtil = actionSpace.getVmUtilizationMean(vm);
        if (meanUtil > UTIL_THRESHOLD) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v33] ",
                    "skipping deallocation, VM " + vmId + " mean utilisation " + meanUtil
                            + " above underuse threshold " + UTIL_THRESHOLD);
            return false;
        }

        boolean succeeded = actionSpace.requestPeDeallocation(vm);
        if (succeeded) {
            successfulActionCount++;
        }

        Log.printlnConcat(actionSpace.getNow(), ": [executor_v33] ",
                "low-utilisation deallocation for VM " + vmId + " succeeded=" + succeeded);
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
