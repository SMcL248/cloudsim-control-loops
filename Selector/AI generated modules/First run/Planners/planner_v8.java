package org.cloudbus.cloudsim.examples;

import java.util.List;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;

/**
 * Planner v8 - Extra-core allocation planner (throughput-oriented).
 *
 * Strategy:
 *   Diagnosis is a per-VM LoadState[] (index i corresponds to
 *   readSpace.getVmList().get(i)). Finds the first OVERLOADED VM whose host
 *   still has a free PE, and requests that PE be allocated to the VM,
 *   raising its processing capacity without a migration.
 *
 * Input semantic  : vm-loadstate-pe-bound (GUID 2300)
 * Output semantic : pe-allocation         (GUID 3012, requestPeAllocation)
 */
public class planner_v8 implements Planner<LoadState[], int[]> {

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {
        double now = readSpace.getNow();
        List<GuestEntity> vms = readSpace.getVmList();

        if (diagnosis == null || diagnosis.length != vms.size()) {
            Log.printlnConcat(now, ": [planner_v8] Diagnosis/VM size mismatch. No-op.");
            return new int[]{-1};
        }

        List<HostEntity> hosts = readSpace.getAllHosts();

        for (int i = 0; i < vms.size(); i++) {
            if (diagnosis[i] != LoadState.OVERLOADED) continue;

            GuestEntity vm = vms.get(i);
            HostEntity host = findHostOf(vm, hosts, readSpace);
            if (host == null) continue;
            if (!readSpace.hostHasFreePe(host)) continue;

            int vmId = readSpace.getId(vm);
            Log.printlnConcat(now, ": [planner_v8] Plan allocate free PE on host ",
                    readSpace.getId(host), " to overloaded VM ", vmId);
            return new int[]{vmId};
        }

        Log.printlnConcat(now, ": [planner_v8] No overloaded VM with a free host PE. No-op.");
        return new int[]{-1};
    }

    private HostEntity findHostOf(GuestEntity vm, List<HostEntity> hosts, ReadSpace readSpace) {
        int vmId = readSpace.getId(vm);
        for (HostEntity host : hosts) {
            for (GuestEntity guest : readSpace.getVmListForHost(host)) {
                if (readSpace.getId(guest) == vmId) return host;
            }
        }
        return null;
    }

    @Override
    public String inputSemantic() {
        return "vm-loadstate-pe-bound";
    }

    @Override
    public String outputSemantic() {
        return "pe-allocation";
    }

    @Override
    public int inputGuid() {
        return 2300;
    }

    @Override
    public int outputGuid() {
        return 3012;
    }
}
