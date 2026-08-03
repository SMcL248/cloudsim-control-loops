package org.cloudbus.cloudsim.examples;

import java.util.List;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;

/**
 * Planner v1 - Consolidation migration planner (power-oriented).
 *
 * Strategy:
 *   Diagnosis is a per-host LoadState[] (index i corresponds to
 *   readSpace.getAllHosts().get(i)). Looks for an UNDERLOADED host that is
 *   still carrying guests, picks its first VM, and relocates it onto a
 *   BALANCED host with room, so the source host can eventually be drained
 *   and powered down. Reduces the number of lightly-loaded hosts that still
 *   draw power.
 *
 * Input semantic  : host-loadstate-consolidation (GUID 2200)
 * Output semantic : vm-migration                 (GUID 3006, requestVmMigration)
 */
public class planner_v1 implements Planner<LoadState[], int[]> {

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {
        double now = readSpace.getNow();
        List<HostEntity> hosts = readSpace.getAllHosts();

        if (diagnosis == null || diagnosis.length != hosts.size()) {
            Log.printlnConcat(now, ": [planner_v1] Diagnosis/host size mismatch. No-op.");
            return new int[]{-1, -1};
        }

        for (int i = 0; i < hosts.size(); i++) {
            if (diagnosis[i] != LoadState.UNDERLOADED) continue;

            HostEntity source = hosts.get(i);
            if (readSpace.isHostFailed(source) || readSpace.isHostPermanentlyDead(source)) continue;

            List<GuestEntity> guests = readSpace.getVmListForHost(source);
            if (guests.isEmpty()) continue;

            GuestEntity vm = guests.get(0);
            if (readSpace.isVmMigrating(vm) || readSpace.isVmBeingInstantiated(vm)) continue;

            HostEntity dest = findConsolidationTarget(hosts, diagnosis, source, vm, readSpace);
            if (dest == null) continue;

            int vmId = readSpace.getId(vm);
            int destId = readSpace.getId(dest);
            Log.printlnConcat(now, ": [planner_v1] Plan migrate VM ", vmId,
                    " off underloaded host ", readSpace.getId(source), " -> host ", destId);
            return new int[]{vmId, destId};
        }

        Log.printlnConcat(now, ": [planner_v1] No consolidation candidate found. No-op.");
        return new int[]{-1, -1};
    }

    private HostEntity findConsolidationTarget(List<HostEntity> hosts, LoadState[] diagnosis,
            HostEntity source, GuestEntity vm, ReadSpace readSpace) {
        for (int i = 0; i < hosts.size(); i++) {
            if (diagnosis[i] != LoadState.BALANCED) continue;
            HostEntity candidate = hosts.get(i);
            if (readSpace.getId(candidate) == readSpace.getId(source)) continue;
            if (readSpace.isHostFailed(candidate) || readSpace.isHostPermanentlyDead(candidate)) continue;
            if (readSpace.isHostPoweredDown(candidate) || readSpace.isHostPoweringUp(candidate)) continue;
            if (readSpace.isHostSuitableForGuest(candidate, vm)) {
                return candidate;
            }
        }
        return null;
    }

    @Override
    public String inputSemantic() {
        return "host-loadstate-consolidation";
    }

    @Override
    public String outputSemantic() {
        return "vm-migration";
    }

    @Override
    public int inputGuid() {
        return 2200;
    }

    @Override
    public int outputGuid() {
        return 3006;
    }
}
