package org.cloudbus.cloudsim.examples;

import java.util.List;
import java.util.HashSet;
import java.util.Set;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;

/**
 * Planner v2 — Consolidation
 *
 * Strategy: Locate the first UNDERLOADED host in the diagnosis array.
 * Select the VM with the lowest requested MIPS as the migration candidate
 * (lightest workload, easiest to place elsewhere). Move it to a BALANCED
 * host (preferred) or another UNDERLOADED host to begin vacating the
 * underloaded host and reducing resource fragmentation.
 *
 * Trigger:  UNDERLOADED host present
 * VM pick:  lowest requested MIPS
 * Dest pick: BALANCED first, then another UNDERLOADED
 */
public class planner_v2 implements Planner<LoadState[], int[]> {

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {
        double now = readSpace.getNow();
        List<HostEntity> hosts = readSpace.getAllHosts();
        List<GuestEntity> vms   = readSpace.getVmList();
        int bound = Math.min(diagnosis.length, hosts.size());

        // Step 1: Find the first UNDERLOADED host
        int underloadedIdx = -1;
        for (int i = 0; i < bound; i++) {
            if (diagnosis[i] == LoadState.UNDERLOADED) {
                underloadedIdx = i;
                break;
            }
        }

        if (underloadedIdx == -1) {
            Log.printlnConcat(now, ": [Planner_v2] No underloaded host found. Consolidation not required.");
            return new int[]{-1, -1};
        }

        Log.printlnConcat(now, ": [Planner_v2] Underloaded host at index ", underloadedIdx, ". Starting consolidation.");

        // Step 2: Select VM with lowest requested MIPS (lightest workload to relocate)
        GuestEntity candidate = null;
        double minMips = Double.POSITIVE_INFINITY;
        for (GuestEntity vm : vms) {
            double mips = vm.getCurrentRequestedTotalMips();
            if (mips < minMips) {
                minMips = mips;
                candidate = vm;
            }
        }

        if (candidate == null) {
            Log.printlnConcat(now, ": [Planner_v2] No VMs available for consolidation.");
            return new int[]{-1, -1};
        }

        Log.printlnConcat(now, ": [Planner_v2] Consolidation candidate: VM ", candidate.getId(),
                " (MIPS=", minMips, ").");

        // Step 3: Find destination — prefer BALANCED (consolidate onto active host),
        //         fall back to another UNDERLOADED host
        HostEntity destination = findDestination(diagnosis, hosts, candidate, underloadedIdx, LoadState.BALANCED, bound);
        if (destination == null) {
            destination = findDestination(diagnosis, hosts, candidate, underloadedIdx, LoadState.UNDERLOADED, bound);
        }

        if (destination == null) {
            Log.printlnConcat(now, ": [Planner_v2] No suitable consolidation destination for VM ", candidate.getId(), ".");
            return new int[]{-1, -1};
        }

        Log.printlnConcat(now, ": [Planner_v2] Plan: consolidate VM ", candidate.getId(),
                " -> Host ", destination.getId(), " (vacating underloaded host index ", underloadedIdx, ").");
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
