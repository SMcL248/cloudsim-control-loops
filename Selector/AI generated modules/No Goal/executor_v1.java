package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.core.GuestEntity;

// Strategy: residency-verified cloudlet move.
// Before issuing the move, confirms the cloudlet is still resident on the
// stated source VM. Plans can go stale between Plan and Execute (the
// cloudlet may already have finished, been moved, or been lost), so this
// guards against acting on out-of-date information rather than trusting
// the payload blindly.
public class executor_v1 implements Executor<int[]> {

    private static final int INPUT_GUID = 3001;
    private static final String INPUT_SEMANTIC = "move a cloudlet from one VM to another VM";

    @Override
    public boolean execute(int[] actions, ActionSpace actionSpace) {
        if (!hasShape(actions, 3) || isSentinel(actions)) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v1] no-op or malformed payload received, skipping moveCloudlet");
            return false;
        }

        int cloudletId = actions[0];
        int fromVmId = actions[1];
        int toVmId = actions[2];

        Cloudlet cl = actionSpace.getCloudletById(cloudletId);
        GuestEntity fromVm = actionSpace.getVmById(fromVmId);

        if (cl == null || fromVm == null) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v1] cloudlet ", cloudletId, " or source VM ", fromVmId, " no longer resolvable, skipping move");
            return false;
        }

        boolean stillResident = false;
        for (Cloudlet resident : actionSpace.getVmCloudletList(fromVm)) {
            if (actionSpace.getId(resident) == cloudletId) {
                stillResident = true;
                break;
            }
        }

        if (!stillResident) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v1] cloudlet ", cloudletId, " is no longer resident on VM ", fromVmId, ", stale plan detected, skipping move");
            return false;
        }

        actionSpace.moveCloudlet(cloudletId, fromVmId, toVmId);
        Log.printlnConcat(actionSpace.getNow(), ": [executor_v1] moved cloudlet ", cloudletId, " from VM ", fromVmId, " to VM ", toVmId);
        return true;
    }

    @Override
    public String inputSemantic() {
        return INPUT_SEMANTIC;
    }

    @Override
    public int inputGuid() {
        return INPUT_GUID;
    }

    private boolean hasShape(int[] a, int len) {
        return a != null && a.length == len;
    }

    private boolean isSentinel(int[] a) {
        for (int v : a) {
            if (v != -1) {
                return false;
            }
        }
        return true;
    }
}
