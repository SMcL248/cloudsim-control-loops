package org.cloudbus.cloudsim.examples;// always include

import java.util.List;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.Cloudlet;

// GUID 3004 -- requestVmDestruction
// Strategy: risk-aware but non-blocking. Destruction is a legitimate,
// deliberate strategic call from upstream (e.g. reclaiming a VM on a
// permanently dead host, or consolidating idle capacity), so this executor
// does not second-guess the decision -- it always attempts the destruction
// once the VM reference is valid, but first measures and logs the MI of
// in-flight work that will be stranded, giving downstream tuning a concrete
// throughput-cost signal for this class of action.
public class executor_v4 implements Executor<int[]> {

    private static final int GUID = 3004;

    @Override
    public boolean execute(int[] action, ActionSpace actionSpace) {
        if (action == null || action.length != 1) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v4] Malformed payload for requestVmDestruction, expected 1 int, aborting.");
            return false;
        }
        if (isSentinel(action)) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v4] No prescribed action this cycle.");
            return false;
        }

        int vmId = action[0];
        GuestEntity vm = actionSpace.getVmById(vmId);
        if (vm == null) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v4] Unknown VM reference " + vmId + ", aborting destruction.");
            return false;
        }

        List<Cloudlet> stranded = actionSpace.getVmCloudletList(vm);
        long strandedMi = 0;
        for (Cloudlet cl : stranded) {
            strandedMi += actionSpace.getRemainingLength(cl);
        }
        if (strandedMi > 0) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v4] Destroying VM " + vmId + " will strand " + strandedMi + " MI of unfinished work across " + stranded.size() + " cloudlet(s).");
        }

        actionSpace.requestVmDestruction(vm);
        Log.printlnConcat(actionSpace.getNow(), ": [executor_v4] Destroyed VM " + vmId);
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
        return "Destroy a VM, logging the throughput cost in stranded MI without overriding the upstream decision to destroy it";
    }

    @Override
    public int inputGuid() {
        return GUID;
    }
}
