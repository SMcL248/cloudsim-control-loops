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


// Strategy: Direct Scaling. Ram-scaling counterpart of the direct mips variant:
// looks up the tier value and attempts the scale unconditionally within bounds.
public class executor_v10 implements Executor<int[]> {

    private int successfulActionCount = 0;

    @Override
    public boolean execute(int[] actions, ActionSpace actionSpace) {
        String tag = "executor_v10";

        if (actions == null || actions.length != 2) {
            Log.printlnConcat(actionSpace.getNow(), ": [" + tag + "] rejected malformed payload, expected 2 ints, got ",
                    (actions == null ? "null" : actions.length));
            return false;
        }

        int vmId = actions[0];
        int tierIndex = actions[1];

        GuestEntity vm = actionSpace.getVmById(vmId);
        int[] ramTiers = actionSpace.getRamTiers();

        if (vm == null || tierIndex < 0 || tierIndex >= ramTiers.length) {
            Log.printlnConcat(actionSpace.getNow(), ": [" + tag + "] cannot resolve vm ", vmId,
                    " or tier index ", tierIndex, " is out of bounds");
            return false;
        }

        double newRam = ramTiers[tierIndex];
        boolean succeeded = actionSpace.requestRamScaling(vm, newRam);
        if (succeeded) {
            successfulActionCount++;
        }

        Log.printlnConcat(actionSpace.getNow(), ": [" + tag + "] attempted ram scaling of vm ", vmId,
                " to tier ", tierIndex, " value ", newRam, ", succeeded ", succeeded);
        return true;
    }

    @Override
    public String inputSemantic() {
        return "scale vm ram to given tier";
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
