package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;

// Executes the requestRamScaling action: scales a VM's RAM allocation to the value at the given tier index.
public class executor_v6 implements Executor<int[]> {

    private int successfulActionCount = 0;

    @Override
    public boolean execute(int[] actions, ActionSpace actionSpace) {
        if (actions == null || actions.length != 2) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v6] invalid payload, expected {vmId, tierIndex}");
            return false;
        }

        int vmId = actions[0];
        int tierIndex = actions[1];

        GuestEntity vm = actionSpace.getVmById(vmId);
        int[] ramTiers = actionSpace.getRamTiers();

        if (vm == null || ramTiers == null || tierIndex < 0 || tierIndex >= ramTiers.length) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v6] cannot resolve vm ", vmId, " or tier index ", tierIndex, ", aborting");
            return false;
        }

        double newRam = ramTiers[tierIndex];
        boolean scaled = actionSpace.requestRamScaling(vm, newRam);

        if (scaled) {
            successfulActionCount++;
        }

        Log.printlnConcat(actionSpace.getNow(), ": [executor_v6] requested ram scaling of vm ", vmId, " to tier ", tierIndex, " (", newRam, ") -- success: ", scaled);
        return true;
    }

    @Override
    public String inputSemantic() {
        return "requestRamScaling(vmId, tierIndex)";
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
