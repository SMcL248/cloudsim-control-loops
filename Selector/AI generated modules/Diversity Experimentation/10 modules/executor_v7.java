package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;

public class executor_v7 implements Executor<int[]> {

    private int successfulActionCount = 0;

    @Override
    public boolean execute(int[] actions, ActionSpace actionSpace) {
        if (actions == null || actions.length != 2) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v7] rejected malformed payload, expected 2 ints");
            return false;
        }

        int vmId = actions[0];
        int tierIndex = actions[1];

        GuestEntity vm = actionSpace.getVmById(vmId);
        int[] bwTiers = actionSpace.getBwTiers();

        if (vm == null || bwTiers == null || tierIndex < 0 || tierIndex >= bwTiers.length) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v7] rejected, unresolved vmId=", vmId, " or bad tierIndex=", tierIndex);
            return false;
        }

        double newBw = bwTiers[tierIndex];
        boolean succeeded;
        try {
            succeeded = actionSpace.requestBwScaling(vm, newBw);
        } catch (Exception e) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v7] requestBwScaling threw, vmId=", vmId, " newBw=", newBw);
            return false;
        }

        if (succeeded) {
            successfulActionCount++;
        }

        Log.printlnConcat(actionSpace.getNow(), ": [executor_v7] attempted requestBwScaling, vmId=", vmId, " newBw=", newBw, " succeeded=", succeeded);
        return true;
    }

    @Override
    public String inputSemantic() {
        return "scale VM bandwidth to tier value";
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
