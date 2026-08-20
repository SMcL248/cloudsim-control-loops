package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;

// Defensive Tier-Clamping Executor for requestRamScaling (3006).
// Mirrors the clamp-and-attempt philosophy applied to MIPS, adapted to the
// RAM tier table, to always make a best-effort scaling attempt.
public class executor_v11 implements Executor<int[]> {

    private int successfulActionCount = 0;

    @Override
    public boolean execute(int[] actions, ActionSpace actionSpace) {
        if (actions == null || actions.length < 2) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v11] ", "Malformed payload, expected {vmId, tierIndex}");
            return false;
        }

        int vmId = actions[0];
        int tierIndex = actions[1];

        GuestEntity vm = actionSpace.getVmById(vmId);
        if (vm == null) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v11] ", "Rejected scaling, unknown vm=" + vmId);
            return false;
        }

        int[] tiers = actionSpace.getRamTiers();
        int clampedIndex = Math.max(0, Math.min(tierIndex, tiers.length - 1));
        if (clampedIndex != tierIndex) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v11] ", "Clamped ram tierIndex " + tierIndex + " -> " + clampedIndex);
        }

        double newRam = tiers[clampedIndex];
        boolean ok = actionSpace.requestRamScaling(vm, newRam);
        if (ok) {
            successfulActionCount++;
        }
        Log.printlnConcat(actionSpace.getNow(), ": [executor_v11] ", "Requested ram scaling of vm=" + vmId + " to " + newRam + " success=" + ok);
        return true;
    }

    @Override
    public String inputSemantic() {
        return "requestRamScaling: scale a VM's RAM to a given tier";
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
