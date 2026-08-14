package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.Cloudlet;

// Executes the moveCloudlet action: relocates a single Cloudlet from one VM to another.
public class executor_v1 implements Executor<int[]> {

    @Override
    public boolean execute(int[] actions, ActionSpace actionSpace) {
        if (actions == null || actions.length != 3) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v1] invalid payload, expected {cloudletId, fromVmId, toVmId}");
            return false;
        }

        int cloudletId = actions[0];
        int fromVmId = actions[1];
        int toVmId = actions[2];

        Cloudlet cloudlet = actionSpace.getCloudletById(cloudletId);
        GuestEntity fromVm = actionSpace.getVmById(fromVmId);
        GuestEntity toVm = actionSpace.getVmById(toVmId);

        if (cloudlet == null || fromVm == null || toVm == null) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v1] cannot resolve cloudlet ", cloudletId, " or vms ", fromVmId, "/", toVmId, ", aborting");
            return false;
        }

        actionSpace.moveCloudlet(cloudletId, fromVmId, toVmId);
        Log.printlnConcat(actionSpace.getNow(), ": [executor_v1] moved cloudlet ", cloudletId, " from vm ", fromVmId, " to vm ", toVmId);
        return true;
    }

    @Override
    public String inputSemantic() {
        return "moveCloudlet(cloudletId, fromVmId, toVmId)";
    }

    @Override
    public int inputGuid() {
        return 3001;
    }
}
