package org.cloudbus.cloudsim.examples;// always include

import java.util.List;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;

// GUID 3008 -- requestPeAllocation
// Strategy: saturation-aware grower. Since a host's PEs are a MIPS-rate
// ceiling/divisor rather than a hard-capacity pool, adding a PE to a VM on an
// already-saturated host dilutes throughput per PE instead of adding real
// capacity. This executor locates the VM's host by scanning getAllHosts()/
// getVmListForHost() and flags that risk in the log, but still honours the
// upstream decision by attempting the allocation regardless.
public class executor_v8 implements Executor<int[]> {

    private static final int GUID = 3008;
    private int successCount = 0;

    @Override
    public boolean execute(int[] action, ActionSpace actionSpace) {
        if (action == null || action.length != 1) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v8] Malformed payload for requestPeAllocation, expected 1 int, aborting.");
            return false;
        }
        if (isSentinel(action)) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v8] No prescribed action this cycle.");
            return false;
        }

        int vmId = action[0];
        GuestEntity vm = actionSpace.getVmById(vmId);
        if (vm == null) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v8] Unknown VM reference " + vmId + ", aborting PE allocation.");
            return false;
        }

        HostEntity host = findHost(actionSpace, vm);
        if (host != null && !actionSpace.hostHasFreePe(host)) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v8] Host of VM " + vmId + " is already saturated; an extra PE will dilute per-PE throughput rather than add capacity. Proceeding as instructed but flagging risk.");
        }

        boolean success = actionSpace.requestPeAllocation(vm);
        if (success) {
            successCount++;
        }
        Log.printlnConcat(actionSpace.getNow(), ": [executor_v8] Requested PE allocation for VM " + vmId + ", success=" + success);
        return true;
    }

    private HostEntity findHost(ActionSpace actionSpace, GuestEntity vm) {
        List<HostEntity> hosts = actionSpace.getAllHosts();
        for (HostEntity h : hosts) {
            List<GuestEntity> vms = actionSpace.getVmListForHost(h);
            for (GuestEntity v : vms) {
                if (actionSpace.getId(v) == actionSpace.getId(vm)) {
                    return h;
                }
            }
        }
        return null;
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
        return "Allocate an additional PE to a VM to raise its ceiling on processable MIPS, flagging host saturation risk";
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
