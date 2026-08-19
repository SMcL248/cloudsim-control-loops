package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;

public class executor_v6 implements Executor<int[]> {

    private int successfulActionCount = 0;

    @Override
    public boolean execute(int[] actions, ActionSpace actionSpace) {
        if (actions == null || actions.length != 2) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v6] rejected malformed payload, expected 2 ints");
            return false;
        }

        int vmId = actions[0];
        int tierIndex = actions[1];

        GuestEntity vm = actionSpace.getVmById(vmId);
        int[] ramTiers = actionSpace.getRamTiers();

        if (vm == null || ramTiers == null || tierIndex < 0 || tierIndex >= ramTiers.length) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v6] rejected, unresolved vmId=", vmId, " or bad tierIndex=", tierIndex);
            return false;
        }

        double newRam = ramTiers[tierIndex];
        boolean succeeded;
        try {
            succeeded = actionSpace.requestRamScaling(vm, newRam);
        } catch (Exception e) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v6] requestRamScaling threw, vmId=", vmId, " newRam=", newRam);
            return false;
        }

        if (succeeded) {
            successfulActionCount++;
        }

        Log.printlnConcat(actionSpace.getNow(), ": [executor_v6] attempted requestRamScaling, vmId=", vmId, " newRam=", newRam, " succeeded=", succeeded);
        return true;
    }

    @Override
    public String inputSemantic() {
        return "scale VM RAM to tier value";
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
