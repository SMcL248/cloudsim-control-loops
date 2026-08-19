package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.Cloudlet;

// Variant angle: moveCloudlet, guarded against no-op moves (same source/target VM)
// and against moving a cloudlet or VM reference that cannot be resolved.
public class executor_v1 implements Executor<int[]> {

    @Override
    public boolean execute(int[] actions, ActionSpace actionSpace) {
        double now = actionSpace.getNow();

        if (actions == null || actions.length != 3) {
            Log.printlnConcat(now, ": [executor_v1] REJECTED malformed payload, expected {cloudletId, fromVmId, toVmId}");
            return false;
        }

        int cloudletId = actions[0];
        int fromVmId = actions[1];
        int toVmId = actions[2];

        if (fromVmId == toVmId) {
            Log.printlnConcat(now, ": [executor_v1] SKIPPED no-op move, cloudlet ", cloudletId, " source and target VM are both ", fromVmId);
            return false;
        }

        Cloudlet cl = actionSpace.getCloudletById(cloudletId);
        GuestEntity fromVm = actionSpace.getVmById(fromVmId);
        GuestEntity toVm = actionSpace.getVmById(toVmId);

        if (cl == null || fromVm == null || toVm == null) {
            Log.printlnConcat(now, ": [executor_v1] REJECTED move, cloudlet ", cloudletId, " or VM ", fromVmId, "/", toVmId, " could not be resolved");
            return false;
        }

        actionSpace.moveCloudlet(cloudletId, fromVmId, toVmId);
        Log.printlnConcat(now, ": [executor_v1] ATTEMPTED moveCloudlet cloudlet=", cloudletId, " from=", fromVmId, " to=", toVmId);
        return true;
    }

    @Override
    public String inputSemantic() {
        return "move cloudlet between VMs";
    }

    @Override
    public int inputGuid() {
        return 3001;
    }
}
