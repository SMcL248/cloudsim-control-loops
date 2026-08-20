package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;

public class executor_v25 implements Executor<int[]> {

    private int successfulActionCount = 0;

    @Override
    public boolean execute(int[] payload, ActionSpace actionSpace) {
        if (payload == null || payload.length != 2) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v25] ",
                    "malformed payload, expected {vmId, tierIndex}");
            return false;
        }

        int vmId = payload[0];
        int tierIndex = payload[1];

        GuestEntity vm = actionSpace.getVmById(vmId);
        if (vm == null) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v25] ",
                    "aborting scaling, unresolved VM reference for id " + vmId);
            return false;
        }

        int[] tiers = actionSpace.getBwTiers();
        if (tierIndex < 0 || tierIndex >= tiers.length) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v25] ",
                    "aborting scaling, tierIndex " + tierIndex + " out of range");
            return false;
        }

        double requestedBw = tiers[tierIndex];
        if (requestedBw == actionSpace.getVmBw(vm)) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v25] ",
                    "skipping scaling, VM " + vmId + " already at requested bandwidth tier " + requestedBw);
            return false;
        }

        boolean succeeded = actionSpace.requestBwScaling(vm, requestedBw);
        if (succeeded) {
            successfulActionCount++;
        }

        Log.printlnConcat(actionSpace.getNow(), ": [executor_v25] ",
                "redundancy check passed, issued requestBwScaling vm=" + vmId + " succeeded=" + succeeded);
        return true;
    }

    @Override
    public String inputSemantic() {
        return "Scale VM bandwidth to a tier";
    }

    @Override
    public int inputGuid() {
        return 3007;
    }

    @Override
    public int getSuccessfulActionCount() {
        return successfulActionCount;
    }
}
