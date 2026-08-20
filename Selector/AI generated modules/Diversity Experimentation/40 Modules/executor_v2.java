package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.core.GuestEntity;

public class executor_v2 implements Executor<int[]> {

    @Override
    public boolean execute(int[] payload, ActionSpace actionSpace) {
        if (payload == null || payload.length != 3) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v2] ",
                    "malformed payload, expected {cloudletId, fromVmId, toVmId}");
            return false;
        }

        int cloudletId = payload[0];
        int fromVmId = payload[1];
        int toVmId = payload[2];

        Cloudlet cl = actionSpace.getCloudletById(cloudletId);
        GuestEntity fromVm = actionSpace.getVmById(fromVmId);
        GuestEntity toVm = actionSpace.getVmById(toVmId);

        if (cl == null || fromVm == null || toVm == null) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v2] ",
                    "aborting move, unresolved cloudlet or VM reference");
            return false;
        }

        if (!actionSpace.getVmCloudletList(fromVm).contains(cl)) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v2] ",
                    "aborting move, cloudlet " + cloudletId + " is not currently assigned to source VM " + fromVmId);
            return false;
        }

        if (actionSpace.isVmMigrating(toVm) || actionSpace.isVmMigrating(fromVm)) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v2] ",
                    "aborting move, source or destination VM is mid-migration");
            return false;
        }

        actionSpace.moveCloudlet(cloudletId, fromVmId, toVmId);

        Log.printlnConcat(actionSpace.getNow(), ": [executor_v2] ",
                "validated and issued moveCloudlet cloudlet=" + cloudletId + " from=" + fromVmId + " to=" + toVmId);
        return true;
    }

    @Override
    public String inputSemantic() {
        return "Move a cloudlet from one VM to another";
    }

    @Override
    public int inputGuid() {
        return 3001;
    }
}
