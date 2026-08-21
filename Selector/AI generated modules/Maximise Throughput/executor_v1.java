package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.Cloudlet;

// GUID 3001 -- moveCloudlet
// Strategy: guard against no-op / wasted migrations. Aborts (does not attempt)
// when the referenced cloudlet or VMs cannot be resolved, when source and
// destination are identical, or when the cloudlet has already finished --
// firing the action in these cases would either error or burn a control
// cycle without moving any throughput needle.
public class executor_v1 implements Executor<int[]> {

    private static final int GUID = 3001;

    @Override
    public boolean execute(int[] action, ActionSpace actionSpace) {
        if (action == null || action.length != 3) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v1] Malformed payload for moveCloudlet, expected 3 ints, aborting.");
            return false;
        }
        if (isSentinel(action)) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v1] No prescribed action this cycle.");
            return false;
        }

        int cloudletId = action[0];
        int fromVmId = action[1];
        int toVmId = action[2];

        Cloudlet cl = actionSpace.getCloudletById(cloudletId);
        GuestEntity fromVm = actionSpace.getVmById(fromVmId);
        GuestEntity toVm = actionSpace.getVmById(toVmId);

        if (cl == null || fromVm == null || toVm == null) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v1] Unknown cloudlet/VM reference, aborting move for cloudlet " + cloudletId);
            return false;
        }
        if (fromVmId == toVmId) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v1] Source and destination VM identical, skipping no-op move for cloudlet " + cloudletId);
            return false;
        }
        if (actionSpace.getRemainingLength(cl) <= 0) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v1] Cloudlet " + cloudletId + " has no remaining work, skipping move.");
            return false;
        }

        actionSpace.moveCloudlet(cloudletId, fromVmId, toVmId);
        Log.printlnConcat(actionSpace.getNow(), ": [executor_v1] Moved cloudlet " + cloudletId + " from VM " + fromVmId + " to VM " + toVmId);
        return true;
    }

    private boolean isSentinel(int[] a) {
        for (int v : a) {
            if (v != -1) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String inputSemantic() {
        return "Migrate a Cloudlet from one VM to another to relieve congestion, skipping moves that cannot possibly help throughput";
    }

    @Override
    public int inputGuid() {
        return GUID;
    }
}
