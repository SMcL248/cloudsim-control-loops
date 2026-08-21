package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;

// Strategy: tier-bounds-checked MIPS scaling with success accounting.
// Resolves the requested tier index against getMipsTiers() before
// calling, since an out-of-range index is a malformed plan rather than a
// legitimate scaling target. Tracks the boolean success of the underlying
// ActionSpace call separately via getSuccessfulActionCount(), as required
// for boolean-returning actions.
public class executor_v5 implements Executor<int[]> {

    private static final int INPUT_GUID = 3005;
    private static final String INPUT_SEMANTIC = "scale a VM's MIPS to a given tier";

    private int successfulActionCount = 0;

    @Override
    public boolean execute(int[] actions, ActionSpace actionSpace) {
        if (!hasShape(actions, 2) || isSentinel(actions)) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v5] no-op or malformed payload received, skipping requestMipsScaling");
            return false;
        }

        int vmId = actions[0];
        int tierIndex = actions[1];

        GuestEntity vm = actionSpace.getVmById(vmId);
        int[] mipsTiers = actionSpace.getMipsTiers();

        if (vm == null || tierIndex < 0 || tierIndex >= mipsTiers.length) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v5] VM ", vmId, " unresolvable or tier index ", tierIndex, " out of bounds, skipping scaling");
            return false;
        }

        double targetMips = mipsTiers[tierIndex];
        boolean succeeded = actionSpace.requestMipsScaling(vm, targetMips);

        if (succeeded) {
            successfulActionCount++;
        }

        Log.printlnConcat(actionSpace.getNow(), ": [executor_v5] requested MIPS scaling of VM ", vmId, " to tier ", tierIndex, " (", targetMips, "), succeeded=", succeeded);
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
