package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.Cloudlet;
import java.util.List;

// Executor variant implementing moveCloudlet (GUID suffix 01).
// Payload: {cloudletId, fromVmId, toVmId}
public class executor_v1 implements Executor<int[]> {

    private static final int EXPECTED_LENGTH = 3;

    @Override
    public boolean execute(int[] actions, ActionSpace actionSpace) {
        if (actions == null || actions.length != EXPECTED_LENGTH) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v1] rejected payload, expected 3 ints {cloudletId, fromVmId, toVmId} but got length ",
                    actions == null ? "null" : actions.length);
            return false;
        }

        int cloudletId = actions[0];
        int fromVmId = actions[1];
        int toVmId = actions[2];

        Cloudlet cloudlet = actionSpace.getCloudletById(cloudletId);
        GuestEntity fromVm = actionSpace.getVmById(fromVmId);
        GuestEntity toVm = actionSpace.getVmById(toVmId);

        if (cloudlet == null || fromVm == null || toVm == null) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v1] skipped move, unknown id(s). cloudlet=", cloudletId,
                    " from=", fromVmId, " to=", toVmId);
            return false;
        }

        List<Cloudlet> fromVmCloudlets = actionSpace.getVmCloudletList(fromVm);
        if (!fromVmCloudlets.contains(cloudlet)) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v1] skipped move, cloudlet ", cloudletId,
                    " is not currently assigned to source vm ", fromVmId);
            return false;
        }

        actionSpace.moveCloudlet(cloudletId, fromVmId, toVmId);
        Log.printlnConcat(actionSpace.getNow(), ": [executor_v1] moved cloudlet ", cloudletId,
                " from vm ", fromVmId, " to vm ", toVmId);
        return true;
    }

    @Override
    public String inputSemantic() {
        return "moveCloudlet action payload {cloudletId, fromVmId, toVmId}";
    }

    @Override
    public int inputGuid() {
        return 3001;
    }
}
