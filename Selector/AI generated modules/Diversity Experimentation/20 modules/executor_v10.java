package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;

// No-op Detection Executor for requestMipsScaling (3005).
// Rejects out-of-range tier indices outright (no clamping) and skips the
// call entirely when the VM is already at the requested tier.
public class executor_v10 implements Executor<int[]> {

    private int successfulActionCount = 0;

    @Override
    public boolean execute(int[] actions, ActionSpace actionSpace) {
        if (actions == null || actions.length < 2) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v10] ", "Malformed payload, expected {vmId, tierIndex}");
            return false;
        }

        int vmId = actions[0];
        int tierIndex = actions[1];

        GuestEntity vm = actionSpace.getVmById(vmId);
        if (vm == null) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v10] ", "Rejected scaling, unknown vm=" + vmId);
            return false;
        }

        int[] tiers = actionSpace.getMipsTiers();
        if (tierIndex < 0 || tierIndex >= tiers.length) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v10] ", "Rejected scaling, tierIndex=" + tierIndex + " out of range for vm=" + vmId);
            return false;
        }

        double targetValue = tiers[tierIndex];
        double currentValue = actionSpace.getVmMips(vm);
        if (currentValue == targetValue) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v10] ", "Skipped scaling, vm=" + vmId + " already at mips=" + targetValue);
            return false;
        }

        boolean ok = actionSpace.requestMipsScaling(vm, targetValue);
        if (ok) {
            successfulActionCount++;
        }
        Log.printlnConcat(actionSpace.getNow(), ": [executor_v10] ", "Requested mips scaling of vm=" + vmId + " to " + targetValue + " success=" + ok);
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
