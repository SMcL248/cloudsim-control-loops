package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;

// Executes the requestBwScaling action: scales a VM's bandwidth allocation to the value at the given tier index.
public class executor_v7 implements Executor<int[]> {

    private int successfulActionCount = 0;

    @Override
    public boolean execute(int[] actions, ActionSpace actionSpace) {
        if (actions == null || actions.length != 2) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v7] invalid payload, expected {vmId, tierIndex}");
            return false;
        }

        int vmId = actions[0];
        int tierIndex = actions[1];

        GuestEntity vm = actionSpace.getVmById(vmId);
        int[] bwTiers = actionSpace.getBwTiers();

        if (vm == null || bwTiers == null || tierIndex < 0 || tierIndex >= bwTiers.length) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v7] cannot resolve vm ", vmId, " or tier index ", tierIndex, ", aborting");
            return false;
        }

        double newBw = bwTiers[tierIndex];
        boolean scaled = actionSpace.requestBwScaling(vm, newBw);

        if (scaled) {
            successfulActionCount++;
        }

        Log.printlnConcat(actionSpace.getNow(), ": [executor_v7] requested bw scaling of vm ", vmId, " to tier ", tierIndex, " (", newBw, ") -- success: ", scaled);
        return true;
    }

    @Override
    public String inputSemantic() {
        return "requestBwScaling(vmId, tierIndex)";
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
