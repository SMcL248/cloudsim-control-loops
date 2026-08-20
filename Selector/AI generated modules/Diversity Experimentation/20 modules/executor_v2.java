package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;

// State-Aware Idempotency Guard for moveCloudlet (3001).
// Rather than deeply validating cloudlet/source-VM membership, this variant
// guards against redundant self-moves and races with an in-flight migration
// on the destination VM, then trusts the underlying substrate for the rest.
public class executor_v2 implements Executor<int[]> {

    @Override
    public boolean execute(int[] actions, ActionSpace actionSpace) {
        if (actions == null || actions.length < 3) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v2] ", "Malformed payload, expected {cloudletId, fromVmId, toVmId}");
            return false;
        }

        int cloudletId = actions[0];
        int fromVmId = actions[1];
        int toVmId = actions[2];

        if (fromVmId == toVmId) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v2] ", "Skipped no-op move, cloudlet=" + cloudletId + " source and destination vm are identical");
            return false;
        }

        GuestEntity toVm = actionSpace.getVmById(toVmId);
        if (toVm == null) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v2] ", "Rejected move, unknown destination vm=" + toVmId);
            return false;
        }

        if (actionSpace.isVmMigrating(toVm)) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v2] ", "Skipped move, destination vm=" + toVmId + " is mid-migration, retry later");
            return false;
        }

        actionSpace.moveCloudlet(cloudletId, fromVmId, toVmId);
        Log.printlnConcat(actionSpace.getNow(), ": [executor_v2] ", "Attempted move of cloudlet=" + cloudletId + " to vm=" + toVmId + " trusting substrate validation");
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
