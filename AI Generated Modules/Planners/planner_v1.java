package org.cloudbus.cloudsim.examples;

import java.util.List;
import java.util.HashSet;
import java.util.Set;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;

/**
 * Planner v1 — Overload Relief
 *
 * Strategy: Locate the first OVERLOADED host in the diagnosis array.
 * Select the VM with the highest requested MIPS as the migration candidate
 * (maximum load-shedding potential). Find a destination host that is
 * UNDERLOADED (preferred) or BALANCED and can accommodate the VM.
 *
 * Trigger:  OVERLOADED host present
 * VM pick:  highest requested MIPS
 * Dest pick: UNDERLOADED first, then BALANCED
 */
public class planner_v1 implements Planner<LoadState[], int[]> {

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {
        double now = readSpace.getNow();
        List<HostEntity> hosts = readSpace.getAllHosts();
        List<GuestEntity> vms   = readSpace.getVmList();
        int bound = Math.min(diagnosis.length, hosts.size());

        // Step 1: Find the first OVERLOADED host
        int overloadedIdx = -1;
        for (int i = 0; i < bound; i++) {
            if (diagnosis[i] == LoadState.OVERLOADED) {
                overloadedIdx = i;
                break;
            }
        }

        if (overloadedIdx == -1) {
            Log.printlnConcat(now, ": [Planner_v1] No overloaded host detected. Skipping migration.");
            return new int[]{-1, -1};
        }

        Log.printlnConcat(now, ": [Planner_v1] Overloaded host at index ", overloadedIdx, ".");

        // Step 2: Select VM with highest requested MIPS
        GuestEntity candidate = null;
        double maxMips = Double.NEGATIVE_INFINITY;
        for (GuestEntity vm : vms) {
            double mips = vm.getCurrentRequestedTotalMips();
            if (mips > maxMips) {
                maxMips = mips;
                candidate = vm;
            }
        }

        if (candidate == null) {
            Log.printlnConcat(now, ": [Planner_v1] No VMs available for migration.");
            return new int[]{-1, -1};
        }

        Log.printlnConcat(now, ": [Planner_v1] Migration candidate: VM ", candidate.getId(),
                " (MIPS=", maxMips, ").");

        // Step 3: Find destination — prefer UNDERLOADED, fall back to BALANCED
        HostEntity destination = findDestination(diagnosis, hosts, candidate, overloadedIdx, LoadState.UNDERLOADED, bound);
        if (destination == null) {
            destination = findDestination(diagnosis, hosts, candidate, overloadedIdx, LoadState.BALANCED, bound);
        }

        if (destination == null) {
            Log.printlnConcat(now, ": [Planner_v1] No suitable destination found for VM ", candidate.getId(), ".");
            return new int[]{-1, -1};
        }

        Log.printlnConcat(now, ": [Planner_v1] Plan: migrate VM ", candidate.getId(),
                " -> Host ", destination.getId(), " (relieving overloaded host index ", overloadedIdx, ").");
        return new int[]{candidate.getId(), destination.getId()};
    }

    private HostEntity findDestination(LoadState[] diagnosis, List<HostEntity> hosts,
                                        GuestEntity vm, int excludeIdx,
                                        LoadState targetState, int bound) {
        for (int i = 0; i < bound; i++) {
            if (i == excludeIdx) continue;
            if (diagnosis[i] == targetState && hosts.get(i).isSuitableForGuest(vm)) {
                return hosts.get(i);
            }
        }
        return null;
    }

    @Override
    public String inputGuid() { return "host-loadstate"; }

    @Override
    public String outputGuid() { return "host-migration-pair"; }
}
