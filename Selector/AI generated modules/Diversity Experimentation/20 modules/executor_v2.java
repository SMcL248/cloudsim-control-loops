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


// Strategy: Direct Migration. Decodes {vmId, targetHostId} and fires the migration
// request unconditionally once both entities resolve. No pre-checks on host health
// or resource feasibility; trusts the Planner completely.
public class executor_v2 implements Executor<int[]> {

    @Override
    public boolean execute(int[] actions, ActionSpace actionSpace) {
        String tag = "executor_v2";

        if (actions == null || actions.length != 2) {
            Log.printlnConcat(actionSpace.getNow(), ": [" + tag + "] rejected malformed payload, expected 2 ints, got ",
                    (actions == null ? "null" : actions.length));
            return false;
        }

        int vmId = actions[0];
        int targetHostId = actions[1];

        GuestEntity vm = actionSpace.getVmById(vmId);
        HostEntity targetHost = actionSpace.getHostById(targetHostId);

        if (vm == null || targetHost == null) {
            Log.printlnConcat(actionSpace.getNow(), ": [" + tag + "] cannot resolve vm ", vmId,
                    " or target host ", targetHostId);
            return false;
        }

        actionSpace.requestVmMigration(vm, targetHost);
        Log.printlnConcat(actionSpace.getNow(), ": [" + tag + "] attempted migration of vm ", vmId,
                " to host ", targetHostId, " with no feasibility pre-check");
        return true;
    }

    @Override
    public String inputSemantic() {
        return "migrate vm to target host";
    }

    @Override
    public int inputGuid() {
        return 3002;
    }
}
