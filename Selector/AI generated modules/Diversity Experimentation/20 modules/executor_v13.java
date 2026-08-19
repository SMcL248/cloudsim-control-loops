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


// Strategy: Idempotency-Guarded Scaling. Skips firing the action entirely if the
// vm is already at the requested bandwidth tier value, avoiding redundant churn
// on the underlying network reconfiguration.
public class executor_v13 implements Executor<int[]> {

    private int successfulActionCount = 0;

    @Override
    public boolean execute(int[] actions, ActionSpace actionSpace) {
        String tag = "executor_v13";

        if (actions == null || actions.length != 2) {
            Log.printlnConcat(actionSpace.getNow(), ": [" + tag + "] rejected malformed payload, expected 2 ints, got ",
                    (actions == null ? "null" : actions.length));
            return false;
        }

        int vmId = actions[0];
        int tierIndex = actions[1];

        GuestEntity vm = actionSpace.getVmById(vmId);
        int[] bwTiers = actionSpace.getBwTiers();

        if (vm == null || tierIndex < 0 || tierIndex >= bwTiers.length) {
            Log.printlnConcat(actionSpace.getNow(), ": [" + tag + "] cannot resolve vm ", vmId,
                    " or tier index ", tierIndex, " is out of bounds");
            return false;
        }

        double targetBw = bwTiers[tierIndex];
        double currentBw = actionSpace.getVmBw(vm);

        if (targetBw == currentBw) {
            Log.printlnConcat(actionSpace.getNow(), ": [" + tag + "] skipped redundant bw scaling of vm ", vmId,
                    ", already at value ", currentBw);
            return false;
        }

        boolean succeeded = actionSpace.requestBwScaling(vm, targetBw);
        if (succeeded) {
            successfulActionCount++;
        }

        Log.printlnConcat(actionSpace.getNow(), ": [" + tag + "] attempted bw scaling of vm ", vmId,
                " from ", currentBw, " to ", targetBw, ", succeeded ", succeeded);
        return true;
    }

    @Override
    public String inputSemantic() {
        return "scale vm bandwidth to given tier, skipped if already at target value";
    }

    @Override
    public int inputGuid() {
        return 3007;
    }

    @Override
    public int getSuccessfulActionCount() {
        return successfulActionCount;
    }
}
