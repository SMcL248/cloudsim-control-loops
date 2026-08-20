package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;

// Defensive Tier-Clamping Executor for requestMipsScaling (3005).
// Clamps an out-of-range tier index into bounds and always attempts the
// scaling call once the VM is found, favouring best-effort execution.
public class executor_v9 implements Executor<int[]> {

    private int successfulActionCount = 0;

    @Override
    public boolean execute(int[] actions, ActionSpace actionSpace) {
        if (actions == null || actions.length < 2) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v9] ", "Malformed payload, expected {vmId, tierIndex}");
            return false;
        }

        int vmId = actions[0];
        int tierIndex = actions[1];

        GuestEntity vm = actionSpace.getVmById(vmId);
        if (vm == null) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v9] ", "Rejected scaling, unknown vm=" + vmId);
            return false;
        }

        int[] tiers = actionSpace.getMipsTiers();
        int clampedIndex = Math.max(0, Math.min(tierIndex, tiers.length - 1));
        if (clampedIndex != tierIndex) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v9] ", "Clamped mips tierIndex " + tierIndex + " -> " + clampedIndex);
        }

        double newValue = tiers[clampedIndex];
        boolean ok = actionSpace.requestMipsScaling(vm, newValue);
        if (ok) {
            successfulActionCount++;
        }
        Log.printlnConcat(actionSpace.getNow(), ": [executor_v9] ", "Requested mips scaling of vm=" + vmId + " to " + newValue + " success=" + ok);
        return true;
    }

    @Override
    public String inputSemantic() {
        return "requestMipsScaling: scale a VM's MIPS to a given tier";
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
