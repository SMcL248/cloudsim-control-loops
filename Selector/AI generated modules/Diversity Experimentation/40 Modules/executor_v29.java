package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;

public class executor_v29 implements Executor<int[]> {

    private static final double UTIL_THRESHOLD = 0.8;

    private int successfulActionCount = 0;

    @Override
    public boolean execute(int[] payload, ActionSpace actionSpace) {
        if (payload == null || payload.length != 1) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v29] ",
                    "malformed payload, expected {vmId}");
            return false;
        }

        int vmId = payload[0];
        GuestEntity vm = actionSpace.getVmById(vmId);
        if (vm == null) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v29] ",
                    "unresolved VM reference for id " + vmId);
            return false;
        }

        double util = actionSpace.getVmCpuUtil(vm);
        if (util < UTIL_THRESHOLD) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v29] ",
                    "skipping allocation, VM " + vmId + " utilisation " + util
                            + " below threshold " + UTIL_THRESHOLD);
            return false;
        }

        boolean succeeded = actionSpace.requestPeAllocation(vm);
        if (succeeded) {
            successfulActionCount++;
        }

        Log.printlnConcat(actionSpace.getNow(), ": [executor_v29] ",
                "utilisation-triggered allocation for VM " + vmId + " succeeded=" + succeeded);
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
