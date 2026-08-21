package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;

// Strategy: existence-checked teardown.
// This is a destructive, irreversible action (destroys the VM's
// allocated workloads), so it re-resolves the VM by id immediately before
// firing and refuses to act on an id that no longer resolves -- guarding
// against a stale plan double-destroying or targeting a VM that was
// already removed by another cycle.
public class executor_v4 implements Executor<int[]> {

    private static final int INPUT_GUID = 3004;
    private static final String INPUT_SEMANTIC = "destroy a VM and its allocated workloads";

    @Override
    public boolean execute(int[] actions, ActionSpace actionSpace) {
        if (!hasShape(actions, 1) || isSentinel(actions)) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v4] no-op or malformed payload received, skipping requestVmDestruction");
            return false;
        }

        int vmId = actions[0];
        GuestEntity vm = actionSpace.getVmById(vmId);

        if (vm == null) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v4] VM ", vmId, " no longer exists, skipping destruction");
            return false;
        }

        int cloudletCount = actionSpace.getVmCloudletList(vm).size();
        actionSpace.requestVmDestruction(vm);
        Log.printlnConcat(actionSpace.getNow(), ": [executor_v4] requested destruction of VM ", vmId, ", carrying ", cloudletCount, " cloudlet(s) at time of destruction");
        return true;
    }

    @Override
    public String inputSemantic() {
        return INPUT_SEMANTIC;
    }

    @Override
    public int inputGuid() {
        return INPUT_GUID;
    }

    private boolean hasShape(int[] a, int len) {
        return a != null && a.length == len;
    }

    private boolean isSentinel(int[] a) {
        for (int v : a) {
            if (v != -1) {
                return false;
            }
        }
        return true;
    }
}
