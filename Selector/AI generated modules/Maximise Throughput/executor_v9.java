package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;

// GUID 3009 -- requestPeDeallocation
// Strategy: hard floor plus soft warning. Refuses to strip a VM down to zero
// PEs -- that would not shrink throughput, it would eliminate it entirely --
// but otherwise trusts the upstream decision, only flagging (not blocking)
// deallocation from a VM that is currently heavily utilised.
public class executor_v9 implements Executor<int[]> {

    private static final int GUID = 3009;
    private static final double HIGH_UTIL_THRESHOLD = 0.7;
    private int successCount = 0;

    @Override
    public boolean execute(int[] action, ActionSpace actionSpace) {
        if (action == null || action.length != 1) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v9] Malformed payload for requestPeDeallocation, expected 1 int, aborting.");
            return false;
        }
        if (isSentinel(action)) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v9] No prescribed action this cycle.");
            return false;
        }

        int vmId = action[0];
        GuestEntity vm = actionSpace.getVmById(vmId);
        if (vm == null) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v9] Unknown VM reference " + vmId + ", aborting PE deallocation.");
            return false;
        }

        if (actionSpace.getVmNumberOfPes(vm) <= 1) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v9] VM " + vmId + " has only one PE left; deallocating would strand it entirely, aborting.");
            return false;
        }
        if (actionSpace.getVmCpuUtil(vm) > HIGH_UTIL_THRESHOLD) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v9] VM " + vmId + " is heavily utilised (" + actionSpace.getVmCpuUtil(vm) + "); deallocating a PE will likely cut its throughput. Proceeding as instructed but flagging risk.");
        }

        boolean success = actionSpace.requestPeDeallocation(vm);
        if (success) {
            successCount++;
        }
        Log.printlnConcat(actionSpace.getNow(), ": [executor_v9] Requested PE deallocation for VM " + vmId + ", success=" + success);
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
        return "Deallocate a PE from a VM, refusing to strand it at zero PEs and flagging deallocation from heavily utilised VMs";
    }

    @Override
    public int inputGuid() {
        return GUID;
    }

    @Override
    public int getSuccessfulActionCount() {
        return successCount;
    }
}
