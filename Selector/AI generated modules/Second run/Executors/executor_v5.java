package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;

// Executes the requestMipsScaling action: scales a VM's MIPS allocation to the value at the given tier index.
public class executor_v5 implements Executor<int[]> {

    private int successfulActionCount = 0;

    @Override
    public boolean execute(int[] actions, ActionSpace actionSpace) {
        if (actions == null || actions.length != 2) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v5] invalid payload, expected {vmId, tierIndex}");
            return false;
        }

        int vmId = actions[0];
        int tierIndex = actions[1];

        GuestEntity vm = actionSpace.getVmById(vmId);
        int[] mipsTiers = actionSpace.getMipsTiers();

        if (vm == null || mipsTiers == null || tierIndex < 0 || tierIndex >= mipsTiers.length) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v5] cannot resolve vm ", vmId, " or tier index ", tierIndex, ", aborting");
            return false;
        }

        double newValue = mipsTiers[tierIndex];
        boolean scaled = actionSpace.requestMipsScaling(vm, newValue);

        if (scaled) {
            successfulActionCount++;
        }

        Log.printlnConcat(actionSpace.getNow(), ": [executor_v5] requested mips scaling of vm ", vmId, " to tier ", tierIndex, " (", newValue, ") -- success: ", scaled);
        return true;
    }

    @Override
    public String inputSemantic() {
        return "requestMipsScaling(vmId, tierIndex)";
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
