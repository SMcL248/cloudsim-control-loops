package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;

// Executor for ActionSpace.requestMipsScaling - GUID suffix 05.
public class executor_v5 implements Executor<int[]> {

    private static final int GUID = 3005;
    private static final String SEMANTIC =
        "requestMipsScaling: rescale a VM's per-PE MIPS rating to a specific entry in getMipsTiers(). Payload {vmId, tierIndex}.";

    private int successfulActions = 0;

    @Override
    public boolean execute(int[] actions, ActionSpace actionSpace) {
        double now = actionSpace.getNow();

        if (actions == null || actions.length != 2) {
            Log.printlnConcat(now, ": [executor_v5] rejected - payload must have exactly 2 entries {vmId, tierIndex}.");
            return false;
        }

        if (actions[0] == -1 && actions[1] == -1) {
            Log.printlnConcat(now, ": [executor_v5] no-op sentinel received, no scaling requested.");
            return false;
        }

        int vmId = actions[0];
        int tierIndex = actions[1];

        GuestEntity vm = actionSpace.getVmById(vmId);
        if (vm == null) {
            Log.printlnConcat(now, ": [executor_v5] rejected - VM ", vmId, " could not be resolved.");
            return false;
        }

        int[] tiers = actionSpace.getMipsTiers();
        if (tierIndex < 0 || tierIndex >= tiers.length) {
            Log.printlnConcat(now, ": [executor_v5] rejected - tierIndex ", tierIndex, " is outside the known tier range [0,", tiers.length, ").");
            return false;
        }

        double targetMips = tiers[tierIndex];
        boolean succeeded = actionSpace.requestMipsScaling(vm, targetMips);

        if (succeeded) {
            successfulActions++;
            Log.printlnConcat(now, ": [executor_v5] scaled VM ", vmId, " MIPS to tier ", tierIndex, " (", targetMips, ").");
        } else {
            Log.printlnConcat(now, ": [executor_v5] attempted MIPS scaling of VM ", vmId, " to tier ", tierIndex, " but the request was refused.");
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
