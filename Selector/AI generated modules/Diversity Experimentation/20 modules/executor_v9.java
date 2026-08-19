package org.cloudbus.cloudsim.examples;// always include

// Import whats needed
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;
import org.cloudbus.cloudsim.core.PowerGuestEntity;
import org.cloudbus.cloudsim.core.PowerHostEntity;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.power.PowerDatacenter;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.power.PowerVm;


// Strategy: Clamped Scaling. Rather than rejecting an out-of-range tier index,
// clamps it into the nearest valid tier and proceeds, treating out-of-range
// requests as a saturation signal rather than a hard failure.
public class executor_v9 implements Executor<int[]> {

    private int successfulActionCount = 0;

    @Override
    public boolean execute(int[] actions, ActionSpace actionSpace) {
        String tag = "executor_v9";

        if (actions == null || actions.length != 2) {
            Log.printlnConcat(actionSpace.getNow(), ": [" + tag + "] rejected malformed payload, expected 2 ints, got ",
                    (actions == null ? "null" : actions.length));
            return false;
        }

        int vmId = actions[0];
        int tierIndex = actions[1];

        GuestEntity vm = actionSpace.getVmById(vmId);
        if (vm == null) {
            Log.printlnConcat(actionSpace.getNow(), ": [" + tag + "] cannot resolve vm ", vmId);
            return false;
        }

        int[] mipsTiers = actionSpace.getMipsTiers();
        int clampedIndex = tierIndex;
        if (clampedIndex < 0) {
            clampedIndex = 0;
        } else if (clampedIndex >= mipsTiers.length) {
            clampedIndex = mipsTiers.length - 1;
        }

        if (clampedIndex != tierIndex) {
            Log.printlnConcat(actionSpace.getNow(), ": [" + tag + "] clamped requested tier ", tierIndex,
                    " to nearest valid tier ", clampedIndex, " for vm ", vmId);
        }

        double newValue = mipsTiers[clampedIndex];
        boolean succeeded = actionSpace.requestMipsScaling(vm, newValue);
        if (succeeded) {
            successfulActionCount++;
        }

        Log.printlnConcat(actionSpace.getNow(), ": [" + tag + "] attempted mips scaling of vm ", vmId,
                " to clamped tier ", clampedIndex, " value ", newValue, ", succeeded ", succeeded);
        return true;
    }

    @Override
    public String inputSemantic() {
        return "scale vm mips to given tier, clamped into valid tier range";
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
