package org.cloudbus.cloudsim.examples;

import java.util.List;
import java.util.HashSet;
import java.util.Set;

import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;
import org.cloudbus.cloudsim.Log;

/**
 * Planner v1 — First-Fit Greedy (CPU Utilisation)
 *
 * Strategy:
 *   1. Find the first OVERLOADED host (by index order).
 *   2. Select the VM with the highest current MIPS demand on that host.
 *   3. Find the first BALANCED or UNDERLOADED host that can accommodate the VM.
 *
 * GUID pair: host-cpu-util-loadstate -> host-migration-pair
 *
 * Trade-offs:
 *   - Very fast O(H + V) scan; deterministic and easy to reason about.
 *   - Does not globally minimise imbalance; stops as soon as a valid pair is found.
 */
public class planner_v1 implements Planner<LoadState[], int[]> {

    @Override
    public String inputGuid() {
        return "host-cpu-util-loadstate";
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

        // 1. Find first OVERLOADED host
        HostEntity source = null;
        int sourceIndex = -1;
        for (int i = 0; i < n; i++) {
            if (diagnosis[i] == LoadState.OVERLOADED) {
                source = hosts.get(i);
                sourceIndex = i;
                break;
            }
        }

        if (source == null) {
            Log.printlnConcat(now, ": [Planner v1] No OVERLOADED host found. No migration planned.");
            return new int[]{-1, -1};
        }

        Log.printlnConcat(now, ": [Planner v1] Source host selected: ", source.getId());

        // 2. Pick VM with highest MIPS demand from source host
        List<GuestEntity> vms = source.getGuestList();
        if (vms == null || vms.isEmpty()) {
            Log.printlnConcat(now, ": [Planner v1] Source host ", source.getId(), " has no VMs. Aborting.");
            return new int[]{-1, -1};
        }

        GuestEntity selectedVm = null;
        double maxMips = -1.0;
        for (GuestEntity vm : vms) {
            double mips = vm.getCurrentRequestedTotalMips();
            if (mips > maxMips) {
                maxMips = mips;
                selectedVm = vm;
            }
        }

        Log.printlnConcat(now, ": [Planner v1] VM selected for migration: ", selectedVm.getId(),
                " (MIPS=", maxMips, ")");

        // 3. Find first suitable BALANCED or UNDERLOADED host
        HostEntity target = null;
        for (int i = 0; i < n; i++) {
            if (i == sourceIndex) continue;
            if (diagnosis[i] == LoadState.BALANCED || diagnosis[i] == LoadState.UNDERLOADED) {
                HostEntity candidate = hosts.get(i);
                if (candidate.isSuitableForGuest(selectedVm)) {
                    target = candidate;
                    break;
                }
            }
        }

        if (target == null) {
            Log.printlnConcat(now, ": [Planner v1] No suitable target host found for VM ",
                    selectedVm.getId(), ". Migration aborted.");
            return new int[]{-1, -1};
        }

        Log.printlnConcat(now, ": [Planner v1] Plan: migrate VM ", selectedVm.getId(),
                " -> host ", target.getId());
        return new int[]{selectedVm.getId(), target.getId()};
    }
}
