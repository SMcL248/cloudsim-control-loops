package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;

/**
 * executor_v2
 *
 * Defensive single-cloudlet migration executor.
 * Rejects malformed or partially-negative id triples (not only the exact
 * sentinel), rejects no-op migrations where source and destination VM are
 * identical, and aborts if either VM's datacenter cannot be resolved before
 * attempting the move.
 */
public class executor_v2 implements Executor<int[]> {

    @Override
    public boolean execute(int[] actions, ActionSpace actionSpace) {

        double now = actionSpace.getNow();

        if (actions == null || actions.length != 3) {
            Log.printlnConcat(now, ": executor_v2 - malformed action array, no migration attempted");
            return false;
        }

        int cloudletId = actions[0];
        int fromVmId = actions[1];
        int toVmId = actions[2];

        if (cloudletId < 0 || fromVmId < 0 || toVmId < 0) {
            Log.printlnConcat(now, ": executor_v2 - sentinel or negative id present, no migration attempted");
            return false;
        }

        if (fromVmId == toVmId) {
            Log.printlnConcat(now, ": executor_v2 - source and destination vm are identical (", fromVmId, "), migration skipped");
            return false;
        }

        Integer originDatacenterId = actionSpace.getDatacenterFor(fromVmId);
        Integer destDatacenterId = actionSpace.getDatacenterFor(toVmId);

        if (originDatacenterId == null || destDatacenterId == null) {
            Log.printlnConcat(now, ": executor_v2 - could not resolve datacenter for source or destination vm, migration aborted");
            return false;
        }

        actionSpace.moveCloudlet(cloudletId, fromVmId, toVmId);

        Log.printlnConcat(now, ": executor_v2 - validated migration of cloudlet ", cloudletId,
                " from vm ", fromVmId, " to vm ", toVmId, " in datacenter ", destDatacenterId);

        return true;
    }

    @Override
    public String inputGuid() {
        return "cloudlet-migration";
    }

}
