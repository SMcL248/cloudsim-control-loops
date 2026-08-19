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


// Strategy: Direct Move. Decodes {cloudletId, fromVmId, toVmId} and attempts the
// cloudlet relocation unconditionally once all three referenced entities resolve.
// No feasibility pre-checks are performed; this variant trusts the upstream
// Planner's decision entirely and simply fires the action.
public class executor_v1 implements Executor<int[]> {

    @Override
    public boolean execute(int[] actions, ActionSpace actionSpace) {
        String tag = "executor_v1";

        if (actions == null || actions.length != 3) {
            Log.printlnConcat(actionSpace.getNow(), ": [" + tag + "] rejected malformed payload, expected 3 ints, got ",
                    (actions == null ? "null" : actions.length));
            return false;
        }

        int cloudletId = actions[0];
        int fromVmId = actions[1];
        int toVmId = actions[2];

        Cloudlet cloudlet = actionSpace.getCloudletById(cloudletId);
        GuestEntity fromVm = actionSpace.getVmById(fromVmId);
        GuestEntity toVm = actionSpace.getVmById(toVmId);

        if (cloudlet == null || fromVm == null || toVm == null) {
            Log.printlnConcat(actionSpace.getNow(), ": [" + tag + "] cannot resolve one or more entities for cloudlet ",
                    cloudletId, ", from vm ", fromVmId, ", to vm ", toVmId);
            return false;
        }

        actionSpace.moveCloudlet(cloudletId, fromVmId, toVmId);
        Log.printlnConcat(actionSpace.getNow(), ": [" + tag + "] attempted move of cloudlet ", cloudletId,
                " from vm ", fromVmId, " to vm ", toVmId);
        return true;
    }

    @Override
    public String inputSemantic() {
        return "move cloudlet from source vm to destination vm";
    }

    @Override
    public int inputGuid() {
        return 3001;
    }
}
