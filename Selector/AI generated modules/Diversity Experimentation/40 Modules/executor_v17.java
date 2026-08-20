package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;

public class executor_v17 implements Executor<int[]> {

    // Only scale up MIPS if the VM's rolling utilisation suggests it is under
    // sustained pressure; otherwise the action is considered unnecessary.
    private static final double UTIL_THRESHOLD = 0.75;

    private int successfulActionCount = 0;

    @Override
    public boolean execute(int[] payload, ActionSpace actionSpace) {
        if (payload == null || payload.length != 2) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v17] ",
                    "malformed payload, expected {vmId, tierIndex}");
            return false;
        }

        int vmId = payload[0];
        int tierIndex = payload[1];

        GuestEntity vm = actionSpace.getVmById(vmId);
        if (vm == null) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v17] ",
                    "aborting scaling, unresolved VM reference for id " + vmId);
            return false;
        }

        double meanUtil = actionSpace.getVmUtilizationMean(vm);
        if (meanUtil < UTIL_THRESHOLD) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v17] ",
                    "skipping scaling, VM " + vmId + " mean utilisation " + meanUtil
                            + " is below threshold " + UTIL_THRESHOLD);
            return false;
        }

        int[] tiers = actionSpace.getMipsTiers();
        if (tierIndex < 0 || tierIndex >= tiers.length) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v17] ",
                    "aborting scaling, tierIndex " + tierIndex + " out of range");
            return false;
        }

        boolean succeeded = actionSpace.requestMipsScaling(vm, tiers[tierIndex]);
        if (succeeded) {
            successfulActionCount++;
        }

        Log.printlnConcat(actionSpace.getNow(), ": [executor_v17] ",
                "utilisation-triggered scaling for VM " + vmId + " succeeded=" + succeeded);
        return true;
    }

    @Override
    public String inputSemantic() {
        return "Scale VM MIPS to a tier";
    }

    @Override
    public int inputGuid() {
        return 3005;
    }

    @Override
    public int getSuccessfulActionCount() {
        return successfulActionCount;
    }
}
