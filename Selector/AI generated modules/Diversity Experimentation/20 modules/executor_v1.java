package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.Cloudlet;

// Strict Validation Executor for moveCloudlet (3001).
// Verifies the cloudlet actually belongs to the source VM and that the
// destination VM is not mid-migration before dispatching. Rejects (does
// not call the underlying API, returns false) on any failed precondition.
public class executor_v1 implements Executor<int[]> {

    @Override
    public boolean execute(int[] actions, ActionSpace actionSpace) {
        if (actions == null || actions.length < 3) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v1] ", "Malformed payload, expected {cloudletId, fromVmId, toVmId}");
            return false;
        }

        int cloudletId = actions[0];
        int fromVmId = actions[1];
        int toVmId = actions[2];

        Cloudlet cl = actionSpace.getCloudletById(cloudletId);
        if (cl == null) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v1] ", "Rejected move, unknown cloudlet id=" + cloudletId);
            return false;
        }

        GuestEntity fromVm = actionSpace.getVmById(fromVmId);
        GuestEntity toVm = actionSpace.getVmById(toVmId);
        if (fromVm == null || toVm == null) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v1] ", "Rejected move, unknown vm reference fromVm=" + fromVmId + " toVm=" + toVmId);
            return false;
        }

        boolean residesOnFromVm = actionSpace.getVmCloudletList(fromVm).contains(cl);
        if (!residesOnFromVm) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v1] ", "Rejected move, cloudlet=" + cloudletId + " does not currently reside on vm=" + fromVmId);
            return false;
        }

        if (actionSpace.isVmMigrating(toVm)) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v1] ", "Rejected move, destination vm=" + toVmId + " is currently migrating");
            return false;
        }

        actionSpace.moveCloudlet(cloudletId, fromVmId, toVmId);
        Log.printlnConcat(actionSpace.getNow(), ": [executor_v1] ", "Moved cloudlet=" + cloudletId + " from vm=" + fromVmId + " to vm=" + toVmId);
        return true;
    }

    @Override
    public String inputSemantic() {
        return "moveCloudlet: relocate a cloudlet between two VMs";
    }

    @Override
    public int inputGuid() {
        return 3001;
    }
}
