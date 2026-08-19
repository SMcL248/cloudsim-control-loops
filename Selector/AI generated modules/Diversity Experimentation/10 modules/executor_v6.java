package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;

// Variant angle: requestRamScaling, guarded by bounds-checking tierIndex against
// getRamTiers(), and additionally skipping the call entirely if the resolved
// tier value equals the VM's current RAM (avoids a redundant no-op request).
public class executor_v6 implements Executor<int[]> {

    private int successfulActionCount = 0;

    @Override
    public boolean execute(int[] actions, ActionSpace actionSpace) {
        double now = actionSpace.getNow();

        if (actions == null || actions.length != 2) {
            Log.printlnConcat(now, ": [executor_v6] REJECTED malformed payload, expected {vmId, tierIndex}");
            return false;
        }

        int vmId = actions[0];
        int tierIndex = actions[1];

        GuestEntity vm = actionSpace.getVmById(vmId);
        if (vm == null) {
            Log.printlnConcat(now, ": [executor_v6] REJECTED scaling, VM ", vmId, " could not be resolved");
            return false;
        }

        int[] ramTiers = actionSpace.getRamTiers();
        if (ramTiers == null || tierIndex < 0 || tierIndex >= ramTiers.length) {
            Log.printlnConcat(now, ": [executor_v6] REJECTED scaling, tierIndex ", tierIndex, " out of bounds for VM ", vmId);
            return false;
        }

        double newRam = ramTiers[tierIndex];
        if (newRam == actionSpace.getVmRam(vm)) {
            Log.printlnConcat(now, ": [executor_v6] SKIPPED scaling, VM ", vmId, " already at requested RAM ", newRam);
            return false;
        }

        boolean succeeded = actionSpace.requestRamScaling(vm, newRam);
        if (succeeded) {
            successfulActionCount++;
        }

        Log.printlnConcat(now, ": [executor_v6] ATTEMPTED requestRamScaling vm=", vmId, " newRam=", newRam, " succeeded=", succeeded);
        return true;
    }

    @Override
    public String inputSemantic() {
        return "scale VM RAM tier";
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
