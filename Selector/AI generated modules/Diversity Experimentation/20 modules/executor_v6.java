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


// Strategy: Direct Destruction. Decodes {vmId} and destroys the vm unconditionally
// once it resolves, with no state checks.
public class executor_v6 implements Executor<int[]> {

    @Override
    public boolean execute(int[] actions, ActionSpace actionSpace) {
        String tag = "executor_v6";

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

        actionSpace.requestVmDestruction(vm);
        Log.printlnConcat(actionSpace.getNow(), ": [" + tag + "] attempted destruction of vm ", vmId);
        return true;
    }

    @Override
    public String inputSemantic() {
        return "destroy vm";
    }

    @Override
    public int inputGuid() {
        return 3004;
    }
}
