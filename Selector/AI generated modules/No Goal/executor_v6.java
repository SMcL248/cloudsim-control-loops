package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;

// Strategy: tier-bounds-checked RAM scaling with success accounting.
// Mirrors the MIPS-scaling guard pattern for the RAM dimension: validates
// the tier index against getRamTiers() before calling, and tallies
// boolean success via getSuccessfulActionCount().
public class executor_v6 implements Executor<int[]> {

    private static final int INPUT_GUID = 3006;
    private static final String INPUT_SEMANTIC = "scale a VM's RAM to a given tier";

    private int successfulActionCount = 0;

    @Override
    public boolean execute(int[] actions, ActionSpace actionSpace) {
        if (!hasShape(actions, 2) || isSentinel(actions)) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v6] no-op or malformed payload received, skipping requestRamScaling");
            return false;
        }

        int vmId = actions[0];
        int tierIndex = actions[1];

        GuestEntity vm = actionSpace.getVmById(vmId);
        int[] ramTiers = actionSpace.getRamTiers();

        if (vm == null || tierIndex < 0 || tierIndex >= ramTiers.length) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v6] VM ", vmId, " unresolvable or tier index ", tierIndex, " out of bounds, skipping scaling");
            return false;
        }

        double targetRam = ramTiers[tierIndex];
        boolean succeeded = actionSpace.requestRamScaling(vm, targetRam);

        if (succeeded) {
            successfulActionCount++;
        }

        Log.printlnConcat(actionSpace.getNow(), ": [executor_v6] requested RAM scaling of VM ", vmId, " to tier ", tierIndex, " (", targetRam, "), succeeded=", succeeded);
        return true;
    }

    @Override
    public String inputSemantic() {
        return INPUT_SEMANTIC;
    }

    @Override
    public int inputGuid() {
        return INPUT_GUID;
    }

    @Override
    public int getSuccessfulActionCount() {
        return successfulActionCount;
    }

    private boolean hasShape(int[] a, int len) {
        return a != null && a.length == len;
    }

    private boolean isSentinel(int[] a) {
        for (int v : a) {
            if (v != -1) {
                return false;
            }
        }
        return true;
    }
}
