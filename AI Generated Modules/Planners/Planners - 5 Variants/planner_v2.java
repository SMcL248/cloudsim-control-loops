package org.cloudbus.cloudsim.examples;

import java.util.List;
import java.util.HashSet;
import java.util.Set;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;

/**
 * Planner v2: CPU-util based — Lightest-VM, First-Fit Destination (minimal disruption).
 *
 * Scans ALL overloaded hosts and selects the VM with the lowest MIPS demand
 * across all of them, then places it on the first suitable underloaded host found.
 *
 * Rationale: migrating the smallest VM minimises live-migration cost and network
 * overhead. Any overloaded host is relieved somewhat; the destination search is
 * fast (first-fit) because the small VM is unlikely to stress it.
 */
public class planner_v2 implements Planner<LoadState[], int[]> {

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {
        double now = readSpace.getNow();
        List<HostEntity> hosts = readSpace.getAllHosts();

        // Find VM with lowest MIPS demand across all overloaded hosts
        GuestEntity targetVm = null;
        HostEntity sourceHost = null;
        double minMips = Double.MAX_VALUE;

        for (int i = 0; i < diagnosis.length && i < hosts.size(); i++) {
            if (diagnosis[i] != LoadState.OVERLOADED) continue;
            HostEntity host = hosts.get(i);
            for (GuestEntity vm : host.getGuestList()) {
                double mips = vm.getCurrentRequestedTotalMips();
                if (mips < minMips) {
                    minMips = mips;
                    targetVm = vm;
                    sourceHost = host;
                }
            }
        }

        if (targetVm == null) {
            Log.printlnConcat(now, ": planner_v2: No overloaded host or VM found. No migration.");
            return new int[]{-1, -1};
        }

        // First-fit: pick the first suitable underloaded host
        HostEntity destHost = null;
        for (int i = 0; i < diagnosis.length && i < hosts.size(); i++) {
            if (diagnosis[i] != LoadState.UNDERLOADED) continue;
            HostEntity candidate = hosts.get(i);
            if (candidate.getId() == sourceHost.getId()) continue;
            if (candidate.isSuitableForGuest(targetVm)) {
                destHost = candidate;
                break;
            }
        }

        if (destHost == null) {
            Log.printlnConcat(now, ": planner_v2: No suitable underloaded destination found.");
            return new int[]{-1, -1};
        }

        Log.printlnConcat(now, ": planner_v2: Minimal-disruption migrate VM ", targetVm.getId(),
                " (", minMips, " MIPS) from Host ", sourceHost.getId(),
                " to Host ", destHost.getId());
        return new int[]{targetVm.getId(), destHost.getId()};
    }

    @Override
    public String inputGuid() { return "host-cpu-util-loadstate"; }

    @Override
    public String outputGuid() { return "host-migration-pair"; }
}
