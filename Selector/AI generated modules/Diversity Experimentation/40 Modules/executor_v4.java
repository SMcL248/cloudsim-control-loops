package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.core.GuestEntity;

public class executor_v4 implements Executor<int[]> {

    @Override
    public boolean execute(int[] payload, ActionSpace actionSpace) {
        if (payload == null || payload.length != 3) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v4] ",
                    "malformed payload, expected {cloudletId, fromVmId, toVmId}");
            return false;
        }

        int cloudletId = payload[0];
        int fromVmId = payload[1];
        int toVmId = payload[2];

        actionSpace.moveCloudlet(cloudletId, fromVmId, toVmId);

        Cloudlet cl = actionSpace.getCloudletById(cloudletId);
        GuestEntity toVm = actionSpace.getVmById(toVmId);

        boolean verified = false;
        if (cl != null && toVm != null && actionSpace.getVmCloudletList(toVm) != null) {
            verified = actionSpace.getVmCloudletList(toVm).contains(cl);
        }

        if (verified) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v4] ",
                    "moveCloudlet issued and verified, cloudlet " + cloudletId + " now present on VM " + toVmId);
        } else {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v4] ",
                    "moveCloudlet issued but post-action verification could not confirm placement on VM " + toVmId);
        }

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
