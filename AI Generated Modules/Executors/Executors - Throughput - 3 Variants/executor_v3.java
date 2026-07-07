package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;

/**
 * executor_v3
 *
 * Locality-constrained single-cloudlet migration executor.
 * Resolves the datacenter of both the source and destination VM and only
 * proceeds when they match, refusing cross-datacenter cloudlet migrations
 * outright rather than attempting them.
 */
public class executor_v3 implements Executor<int[]> {

    @Override
    public boolean execute(int[] actions, ActionSpace actionSpace) {

        double now = actionSpace.getNow();

        if (actions == null || actions.length != 3) {
            Log.printlnConcat(now, ": executor_v3 - malformed action array, no migration attempted");
            return false;
        }

        int cloudletId = actions[0];
        int fromVmId = actions[1];
        int toVmId = actions[2];

        if (cloudletId == -1 && fromVmId == -1 && toVmId == -1) {
            Log.printlnConcat(now, ": executor_v3 - sentinel received, no migration needed");
            return false;
        }

        Integer originDatacenterId = actionSpace.getDatacenterFor(fromVmId);
        Integer destDatacenterId = actionSpace.getDatacenterFor(toVmId);

        if (originDatacenterId == null || destDatacenterId == null) {
            Log.printlnConcat(now, ": executor_v3 - unresolved datacenter mapping, migration aborted");
            return false;
        }

        if (!originDatacenterId.equals(destDatacenterId)) {
            Log.printlnConcat(now, ": executor_v3 - cross-datacenter migration not supported (origin ",
                    originDatacenterId, ", destination ", destDatacenterId, "), migration aborted");
            return false;
        }

        actionSpace.moveCloudlet(cloudletId, fromVmId, toVmId);

        Log.printlnConcat(now, ": executor_v3 - same-datacenter migration of cloudlet ", cloudletId,
                " from vm ", fromVmId, " to vm ", toVmId, " within datacenter ", destDatacenterId);

        return true;
    }

    @Override
    public String inputGuid() {
        return "cloudlet-migration";
    }

}
