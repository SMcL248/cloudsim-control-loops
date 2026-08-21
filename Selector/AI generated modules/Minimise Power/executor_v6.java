package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;

// Executor for ActionSpace.requestRamScaling - GUID suffix 06.
public class executor_v6 implements Executor<int[]> {

    private static final int GUID = 3006;
    private static final String SEMANTIC =
        "requestRamScaling: rescale a VM's RAM allocation to a specific entry in getRamTiers(). Payload {vmId, tierIndex}.";

    private int successfulActions = 0;

    @Override
    public boolean execute(int[] actions, ActionSpace actionSpace) {
        double now = actionSpace.getNow();

        if (actions == null || actions.length != 2) {
            Log.printlnConcat(now, ": [executor_v6] rejected - payload must have exactly 2 entries {vmId, tierIndex}.");
            return false;
        }

        if (actions[0] == -1 && actions[1] == -1) {
            Log.printlnConcat(now, ": [executor_v6] no-op sentinel received, no scaling requested.");
            return false;
        }

        int vmId = actions[0];
        int tierIndex = actions[1];

        GuestEntity vm = actionSpace.getVmById(vmId);
        if (vm == null) {
            Log.printlnConcat(now, ": [executor_v6] rejected - VM ", vmId, " could not be resolved.");
            return false;
        }

        int[] tiers = actionSpace.getRamTiers();
        if (tierIndex < 0 || tierIndex >= tiers.length) {
            Log.printlnConcat(now, ": [executor_v6] rejected - tierIndex ", tierIndex, " is outside the known tier range [0,", tiers.length, ").");
            return false;
        }

        double targetRam = tiers[tierIndex];
        boolean succeeded = actionSpace.requestRamScaling(vm, targetRam);

        if (succeeded) {
            successfulActions++;
            Log.printlnConcat(now, ": [executor_v6] scaled VM ", vmId, " RAM to tier ", tierIndex, " (", targetRam, ").");
        } else {
            Log.printlnConcat(now, ": [executor_v6] attempted RAM scaling of VM ", vmId, " to tier ", tierIndex, " but the request was refused.");
        }
        return true;
    }

    @Override
    public String inputSemantic() {
        return SEMANTIC;
    }

    @Override
    public int inputGuid() {
        return GUID;
    }

    @Override
    public int getSuccessfulActionCount() {
        return successfulActions;
    }
}
