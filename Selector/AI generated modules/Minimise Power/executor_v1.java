package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.core.GuestEntity;

// Executor for ActionSpace.moveCloudlet - GUID suffix 01.
public class executor_v1 implements Executor<int[]> {

    private static final int GUID = 3001;
    private static final String SEMANTIC =
        "moveCloudlet: relocate a single Cloudlet from its current VM to a different VM. Payload {cloudletId, fromVmId, toVmId}.";

    @Override
    public boolean execute(int[] actions, ActionSpace actionSpace) {
        double now = actionSpace.getNow();

        if (actions == null || actions.length != 3) {
            Log.printlnConcat(now, ": [executor_v1] rejected - payload must have exactly 3 entries {cloudletId, fromVmId, toVmId}.");
            return false;
        }

        if (actions[0] == -1 && actions[1] == -1 && actions[2] == -1) {
            Log.printlnConcat(now, ": [executor_v1] no-op sentinel received, nothing to move.");
            return false;
        }

        int cloudletId = actions[0];
        int fromVmId = actions[1];
        int toVmId = actions[2];

        if (fromVmId == toVmId) {
            Log.printlnConcat(now, ": [executor_v1] rejected - source and destination VM are identical (vmId=", fromVmId, "), no move needed.");
            return false;
        }

        Cloudlet cl = actionSpace.getCloudletById(cloudletId);
        GuestEntity fromVm = actionSpace.getVmById(fromVmId);
        GuestEntity toVm = actionSpace.getVmById(toVmId);

        if (cl == null || fromVm == null || toVm == null) {
            Log.printlnConcat(now, ": [executor_v1] rejected - cloudlet or VM reference could not be resolved (cloudletId=", cloudletId,
                ", fromVmId=", fromVmId, ", toVmId=", toVmId, ").");
            return false;
        }

        if (!actionSpace.getVmCloudletList(fromVm).contains(cl)) {
            Log.printlnConcat(now, ": [executor_v1] rejected - cloudlet ", cloudletId, " is not currently attached to source VM ", fromVmId, ".");
            return false;
        }

        actionSpace.moveCloudlet(cloudletId, fromVmId, toVmId);
        Log.printlnConcat(now, ": [executor_v1] moved cloudlet ", cloudletId, " from VM ", fromVmId, " to VM ", toVmId, ".");
        return true;
    }

    @Override
    public String inputSemantic() {
        return SEMANTIC;
    }

    @Override
    public int inputGuid() {
        return GUID;
    }
}
