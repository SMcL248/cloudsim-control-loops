package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;

public class executor_v5 implements Executor<int[]> {

    private int successfulActionCount = 0;

    @Override
    public boolean execute(int[] actions, ActionSpace actionSpace) {
        if (actions == null || actions.length != 2) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v5] rejected malformed payload, expected 2 ints");
            return false;
        }

        int vmId = actions[0];
        int tierIndex = actions[1];

        GuestEntity vm = actionSpace.getVmById(vmId);
        int[] mipsTiers = actionSpace.getMipsTiers();

        if (vm == null || mipsTiers == null || tierIndex < 0 || tierIndex >= mipsTiers.length) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v5] rejected, unresolved vmId=", vmId, " or bad tierIndex=", tierIndex);
            return false;
        }

        double newValue = mipsTiers[tierIndex];
        boolean succeeded;
        try {
            succeeded = actionSpace.requestMipsScaling(vm, newValue);
        } catch (Exception e) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v5] requestMipsScaling threw, vmId=", vmId, " newValue=", newValue);
            return false;
        }

        if (succeeded) {
            successfulActionCount++;
        }

        Log.printlnConcat(actionSpace.getNow(), ": [executor_v5] attempted requestMipsScaling, vmId=", vmId, " newValue=", newValue, " succeeded=", succeeded);
        return true;
    }

    @Override
    public String inputSemantic() {
        return "scale VM MIPS to tier value";
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
