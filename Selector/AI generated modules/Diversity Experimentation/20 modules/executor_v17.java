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


// Strategy: Minimum-PE-Guarded Deallocation. Refuses to deallocate a pe if doing
// so would leave the vm with zero pes, protecting it from being stripped down to
// an unschedulable state.
public class executor_v17 implements Executor<int[]> {

    private int successfulActionCount = 0;

    @Override
    public boolean execute(int[] actions, ActionSpace actionSpace) {
        String tag = "executor_v17";

        if (actions == null || actions.length != 1) {
            Log.printlnConcat(actionSpace.getNow(), ": [" + tag + "] rejected malformed payload, expected 1 int, got ",
                    (actions == null ? "null" : actions.length));
            return false;
        }

        int vmId = actions[0];
        GuestEntity vm = actionSpace.getVmById(vmId);

        if (vm == null) {
            Log.printlnConcat(actionSpace.getNow(), ": [" + tag + "] cannot resolve vm ", vmId);
            return false;
        }

        if (actionSpace.getVmNumberOfPes(vm) <= 1) {
            Log.printlnConcat(actionSpace.getNow(), ": [" + tag + "] refused pe deallocation for vm ", vmId,
                    " to avoid stripping it below one pe");
            return false;
        }

        boolean succeeded = actionSpace.requestPeDeallocation(vm);
        if (succeeded) {
            successfulActionCount++;
        }

        Log.printlnConcat(actionSpace.getNow(), ": [" + tag + "] attempted pe deallocation for vm ", vmId,
                ", succeeded ", succeeded);
        return true;
    }

    @Override
    public String inputSemantic() {
        return "deallocate pe from vm, refused if it would leave vm with zero pes";
    }

    @Override
    public int inputGuid() {
        return 3009;
    }

    @Override
    public int getSuccessfulActionCount() {
        return successfulActionCount;
    }
}
