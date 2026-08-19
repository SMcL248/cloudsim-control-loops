package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;

// Variant angle: requestMipsScaling, guarded by bounds-checking tierIndex against
// getMipsTiers() before dereferencing it, and tracking the boolean success signal
// from ActionSpace via getSuccessfulActionCount().
public class executor_v5 implements Executor<int[]> {

    private int successfulActionCount = 0;

    @Override
    public boolean execute(int[] actions, ActionSpace actionSpace) {
        double now = actionSpace.getNow();

        if (actions == null || actions.length != 2) {
            Log.printlnConcat(now, ": [executor_v5] REJECTED malformed payload, expected {vmId, tierIndex}");
            return false;
        }

        int vmId = actions[0];
        int tierIndex = actions[1];

        GuestEntity vm = actionSpace.getVmById(vmId);
        if (vm == null) {
            Log.printlnConcat(now, ": [executor_v5] REJECTED scaling, VM ", vmId, " could not be resolved");
            return false;
        }

        int[] mipsTiers = actionSpace.getMipsTiers();
        if (mipsTiers == null || tierIndex < 0 || tierIndex >= mipsTiers.length) {
            Log.printlnConcat(now, ": [executor_v5] REJECTED scaling, tierIndex ", tierIndex, " out of bounds for VM ", vmId);
            return false;
        }

        double newValue = mipsTiers[tierIndex];
        boolean succeeded = actionSpace.requestMipsScaling(vm, newValue);
        if (succeeded) {
            successfulActionCount++;
        }

        Log.printlnConcat(now, ": [executor_v5] ATTEMPTED requestMipsScaling vm=", vmId, " newValue=", newValue, " succeeded=", succeeded);
        return true;
    }

    @Override
    public String inputSemantic() {
        return "scale VM MIPS tier";
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
