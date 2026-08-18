package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.Cloudlet;
import java.util.List;

// Executor variant implementing requestVmDestruction (GUID suffix 04).
// Payload: {vmId}
// Note: destroying a VM discards any Cloudlet workload still allocated to it.
public class executor_v4 implements Executor<int[]> {

    private static final int EXPECTED_LENGTH = 1;

    @Override
    public boolean execute(int[] actions, ActionSpace actionSpace) {
        if (actions == null || actions.length != EXPECTED_LENGTH) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v4] rejected payload, expected 1 int {vmId} but got length ",
                    actions == null ? "null" : actions.length);
            return false;
        }

        int vmId = actions[0];
        GuestEntity vm = actionSpace.getVmById(vmId);

        if (vm == null) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v4] skipped destruction, unknown vm id ", vmId);
            return false;
        }

        List<Cloudlet> orphanedWork = actionSpace.getVmCloudletList(vm);
        if (!orphanedWork.isEmpty()) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v4] warning, destroying vm ", vmId,
                    " will discard ", orphanedWork.size(), " assigned cloudlet(s)");
        }

        actionSpace.requestVmDestruction(vm);
        Log.printlnConcat(actionSpace.getNow(), ": [executor_v4] requested destruction of vm ", vmId);
        return true;
    }

    @Override
    public String inputSemantic() {
        return "requestVmDestruction action payload {vmId}";
    }

    @Override
    public int inputGuid() {
        return 3004;
    }
}
