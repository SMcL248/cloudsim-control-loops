package org.cloudbus.cloudsim.examples;

import java.util.List;
import java.util.HashSet;
import java.util.Set;

import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;
import org.cloudbus.cloudsim.Log;

/**
 * Planner v2 — VM-Count Aware (VM Count)
 *
 * Strategy:
 *   1. Select the OVERLOADED host carrying the most VMs (densest source).
 *   2. Pick the first VM on that host (count-neutral selection — any removal
 *      reduces count by 1; MIPS is intentionally not used to remain consistent
 *      with the vm-count analysis axis).
 *   3. Target the UNDERLOADED host with the fewest VMs (most room to absorb),
 *      falling back to BALANCED hosts if no UNDERLOADED host is available.
 *
 * GUID pair: host-vm-count-loadstate -> host-migration-pair
 *
 * Trade-offs:
 *   - Directly addresses VM density imbalance.
 *   - No MIPS scoring; all host ranking is done on VM counts only.
 *   - Simple to explain and audit.
 */
public class planner_v2 implements Planner<LoadState[], int[]> {

    @Override
    public String inputGuid() {
        return "host-vm-count-loadstate";
    }

    @Override
    public String outputGuid() {
        return "host-migration-pair";
    }

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {
        double now = readSpace.getNow();
        List<HostEntity> hosts = readSpace.getAllHosts();
        int n = Math.min(diagnosis.length, hosts.size());

        // 1. Select OVERLOADED host with the highest VM count
        HostEntity source = null;
        int sourceIndex = -1;
        int maxVmCount = -1;

        for (int i = 0; i < n; i++) {
            if (diagnosis[i] == LoadState.OVERLOADED) {
                HostEntity candidate = hosts.get(i);
                List<GuestEntity> vms = candidate.getGuestList();
                int count = (vms == null) ? 0 : vms.size();
                if (count > maxVmCount) {
                    maxVmCount = count;
                    source = candidate;
                    sourceIndex = i;
                }
            }
        }

        if (source == null) {
            Log.printlnConcat(now, ": [Planner v2] No OVERLOADED host found. No migration planned.");
            return new int[]{-1, -1};
        }

        Log.printlnConcat(now, ": [Planner v2] Source host selected: ", source.getId(),
                " (VM count=", maxVmCount, ")");

        // 2. Pick the first VM on the source host (any removal reduces count by 1)
        List<GuestEntity> sourceVms = source.getGuestList();
        if (sourceVms == null || sourceVms.isEmpty()) {
            Log.printlnConcat(now, ": [Planner v2] Source host ", source.getId(),
                    " has no VMs. Aborting.");
            return new int[]{-1, -1};
        }

        GuestEntity selectedVm = sourceVms.get(0);
        Log.printlnConcat(now, ": [Planner v2] VM selected for migration: ", selectedVm.getId());

        // 3a. Prefer UNDERLOADED target with fewest VMs
        HostEntity target = null;
        int minVmCount = Integer.MAX_VALUE;

        for (int i = 0; i < n; i++) {
            if (i == sourceIndex) continue;
            if (diagnosis[i] == LoadState.UNDERLOADED) {
                HostEntity candidate = hosts.get(i);
                if (candidate.isSuitableForGuest(selectedVm)) {
                    List<GuestEntity> cvms = candidate.getGuestList();
                    int count = (cvms == null) ? 0 : cvms.size();
                    if (count < minVmCount) {
                        minVmCount = count;
                        target = candidate;
                    }
                }
            }
        }

        // 3b. Fall back to BALANCED host with fewest VMs if no UNDERLOADED found
        if (target == null) {
            minVmCount = Integer.MAX_VALUE;
            for (int i = 0; i < n; i++) {
                if (i == sourceIndex) continue;
                if (diagnosis[i] == LoadState.BALANCED) {
                    HostEntity candidate = hosts.get(i);
                    if (candidate.isSuitableForGuest(selectedVm)) {
                        List<GuestEntity> cvms = candidate.getGuestList();
                        int count = (cvms == null) ? 0 : cvms.size();
                        if (count < minVmCount) {
                            minVmCount = count;
                            target = candidate;
                        }
                    }
                }
            }
        }

        if (target == null) {
            Log.printlnConcat(now, ": [Planner v2] No suitable target host found for VM ",
                    selectedVm.getId(), ". Migration aborted.");
            return new int[]{-1, -1};
        }

        Log.printlnConcat(now, ": [Planner v2] Plan: migrate VM ", selectedVm.getId(),
                " -> host ", target.getId(), " (VM count=", minVmCount, ")");
        return new int[]{selectedVm.getId(), target.getId()};
    }
}
