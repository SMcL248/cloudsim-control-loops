package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;

public class executor_v16 implements Executor<int[]> {

    private int successfulActionCount = 0;

    @Override
    public boolean execute(int[] payload, ActionSpace actionSpace) {
        if (payload == null || payload.length != 2) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v16] ",
                    "malformed payload, expected {vmId, tierIndex}");
            return false;
        }

        int vmId = payload[0];
        int tierIndex = payload[1];

        GuestEntity vm = actionSpace.getVmById(vmId);
        if (vm == null) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v16] ",
                    "aborting scaling, unresolved VM reference for id " + vmId);
            return false;
        }

        int[] tiers = actionSpace.getMipsTiers();
        if (tierIndex < 0 || tierIndex >= tiers.length) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v16] ",
                    "aborting scaling, tierIndex " + tierIndex + " out of range [0," + tiers.length + ")");
            return false;
        }

        double next = actionSpace.getNextMipsTier(vm);
        if (next == -1) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v16] ",
                    "aborting scaling, VM " + vmId + " has no valid next MIPS tier (maxed or off-tier)");
            return false;
        }

        boolean succeeded = actionSpace.requestMipsScaling(vm, tiers[tierIndex]);
        if (succeeded) {
            successfulActionCount++;
        }

        Log.printlnConcat(actionSpace.getNow(), ": [executor_v16] ",
                "tier check passed, issued requestMipsScaling vm=" + vmId + " succeeded=" + succeeded);
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
