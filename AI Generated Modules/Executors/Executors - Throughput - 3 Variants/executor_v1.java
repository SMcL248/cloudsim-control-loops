package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;

/**
 * executor_v1
 *
 * Naive single-cloudlet migration executor.
 * Performs only the mandatory sentinel check, then unconditionally
 * executes the migration described by the input array. Destination
 * datacenter is resolved from the destination VM id.
 */
public class executor_v1 implements Executor<int[]> {

    @Override
    public boolean execute(int[] actions, ActionSpace actionSpace) {

        double now = actionSpace.getNow();

        if (actions == null || actions.length != 3) {
            Log.printlnConcat(now, ": executor_v1 - malformed action array, no migration attempted");
            return false;
        }

        int cloudletId = actions[0];
        int fromVmId = actions[1];
        int toVmId = actions[2];

        if (cloudletId == -1 && fromVmId == -1 && toVmId == -1) {
            Log.printlnConcat(now, ": executor_v1 - sentinel received, no migration needed");
            return false;
        }

        int destDatacenterId = actionSpace.getDatacenterFor(toVmId);

        actionSpace.moveCloudlet(cloudletId, fromVmId, toVmId);

        Log.printlnConcat(now, ": executor_v1 - moved cloudlet ", cloudletId,
                " from vm ", fromVmId, " to vm ", toVmId, " in datacenter ", destDatacenterId);

        return true;
    }

    @Override
    public String inputGuid() {
        return "cloudlet-migration";
    }

}
