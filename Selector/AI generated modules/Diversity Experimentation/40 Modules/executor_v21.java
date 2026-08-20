package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;

public class executor_v21 implements Executor<int[]> {

    private int successfulActionCount = 0;

    @Override
    public boolean execute(int[] payload, ActionSpace actionSpace) {
        if (payload == null || payload.length != 2) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v21] ",
                    "malformed payload, expected {vmId, tierIndex}");
            return false;
        }

        int vmId = payload[0];
        int tierIndex = payload[1];

        GuestEntity vm = actionSpace.getVmById(vmId);
        if (vm == null) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v21] ",
                    "aborting scaling, unresolved VM reference for id " + vmId);
            return false;
        }

        int[] tiers = actionSpace.getRamTiers();
        if (tierIndex < 0 || tierIndex >= tiers.length) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v21] ",
                    "aborting scaling, tierIndex " + tierIndex + " out of range");
            return false;
        }

        double requestedRam = tiers[tierIndex];
        if (requestedRam == actionSpace.getVmRam(vm)) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v21] ",
                    "skipping scaling, VM " + vmId + " already at requested RAM tier " + requestedRam);
            return false;
        }

        boolean succeeded = actionSpace.requestRamScaling(vm, requestedRam);
        if (succeeded) {
            successfulActionCount++;
        }

        Log.printlnConcat(actionSpace.getNow(), ": [executor_v21] ",
                "redundancy check passed, issued requestRamScaling vm=" + vmId + " succeeded=" + succeeded);
        return true;
    }

    @Override
    public String inputSemantic() {
        return "Scale VM RAM to a tier";
    }

    @Override
    public int inputGuid() {
        return 3006;
    }

    @Override
    public int getSuccessfulActionCount() {
        return successfulActionCount;
    }
}
