package org.cloudbus.cloudsim.examples;

import java.util.List;
import java.util.HashSet;
import java.util.Set;

import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;
import org.cloudbus.cloudsim.Log;

/**
 * Planner v3 — Balanced-Pair Optimizer (CPU Utilisation)
 *
 * Strategy:
 *   1. Require at least one OVERLOADED host AND at least one UNDERLOADED host;
 *      if neither exists the loop is already balanced enough — no migration.
 *   2. Select the OVERLOADED host with the highest aggregate MIPS demand
 *      (most urgently needs relief).
 *   3. From that host, pick the VM whose MIPS demand is closest to half the
 *      host's total excess MIPS (target: shrink excess by ~50% per step).
 *   4. Among all suitable UNDERLOADED hosts, prefer the one with the lowest
 *      aggregate MIPS demand (most headroom, least risk of flipping it OVERLOADED).
 *
 * GUID pair: host-cpu-util-loadstate -> host-migration-pair
 *
 * Trade-offs:
 *   - More globally aware than v1/v2; considers both source pressure and
 *     destination headroom simultaneously.
 *   - Requires an OVERLOADED-UNDERLOADED pair; will not migrate to BALANCED
 *     hosts, preventing over-correction.
 *   - The "half-excess" VM selection is a heuristic — actual capacity bounds
 *     are enforced by isSuitableForGuest().
 */
public class planner_v3 implements Planner<LoadState[], int[]> {

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

        // Guard: need at least one OVERLOADED and one UNDERLOADED host
        boolean hasOverloaded = false;
        boolean hasUnderloaded = false;
        for (int i = 0; i < n; i++) {
            if (diagnosis[i] == LoadState.OVERLOADED)  hasOverloaded  = true;
            if (diagnosis[i] == LoadState.UNDERLOADED) hasUnderloaded = true;
        }
        if (!hasOverloaded || !hasUnderloaded) {
            Log.printlnConcat(now, ": [Planner v3] No valid OVERLOADED-UNDERLOADED pair. No migration planned.");
            return new int[]{-1, -1};
        }

        // 1. Find OVERLOADED host with highest aggregate MIPS demand
        HostEntity source = null;
        int sourceIndex = -1;
        double sourceAggregateMips = -1.0;

        for (int i = 0; i < n; i++) {
            if (diagnosis[i] != LoadState.OVERLOADED) continue;
            HostEntity candidate = hosts.get(i);
            List<GuestEntity> vms = candidate.getGuestList();
            if (vms == null || vms.isEmpty()) continue;
            double total = 0.0;
            for (GuestEntity vm : vms) {
                total += vm.getCurrentRequestedTotalMips();
            }
            if (total > sourceAggregateMips) {
                sourceAggregateMips = total;
                source = candidate;
                sourceIndex = i;
            }
        }

        if (source == null) {
            Log.printlnConcat(now, ": [Planner v3] No OVERLOADED host with VMs found. Aborting.");
            return new int[]{-1, -1};
        }

        Log.printlnConcat(now, ": [Planner v3] Source host: ", source.getId(),
                " (aggregate MIPS=", sourceAggregateMips, ")");

        // 2. Pick VM closest to half of the source's aggregate MIPS
        //    This aims to relieve ~50% of the load in a single migration.
        double halfLoad = sourceAggregateMips / 2.0;
        List<GuestEntity> sourceVms = source.getGuestList();
        GuestEntity selectedVm = null;
        double bestDelta = Double.MAX_VALUE;

        for (GuestEntity vm : sourceVms) {
            double mips = vm.getCurrentRequestedTotalMips();
            double delta = Math.abs(mips - halfLoad);
            if (delta < bestDelta) {
                bestDelta = delta;
                selectedVm = vm;
            }
        }

        Log.printlnConcat(now, ": [Planner v3] VM selected: ", selectedVm.getId(),
                " (MIPS=", selectedVm.getCurrentRequestedTotalMips(),
                ", target half-load=", halfLoad, ")");

        // 3. Find suitable UNDERLOADED host with lowest aggregate MIPS (most headroom)
        HostEntity target = null;
        double minTargetMips = Double.MAX_VALUE;

        for (int i = 0; i < n; i++) {
            if (i == sourceIndex) continue;
            if (diagnosis[i] != LoadState.UNDERLOADED) continue;
            HostEntity candidate = hosts.get(i);
            if (!candidate.isSuitableForGuest(selectedVm)) continue;
            List<GuestEntity> cvms = candidate.getGuestList();
            double total = 0.0;
            if (cvms != null) {
                for (GuestEntity vm : cvms) {
                    total += vm.getCurrentRequestedTotalMips();
                }
            }
            if (total < minTargetMips) {
                minTargetMips = total;
                target = candidate;
            }
        }

        if (target == null) {
            Log.printlnConcat(now, ": [Planner v3] No suitable UNDERLOADED target found for VM ",
                    selectedVm.getId(), ". Migration aborted.");
            return new int[]{-1, -1};
        }

        Log.printlnConcat(now, ": [Planner v3] Plan: migrate VM ", selectedVm.getId(),
                " -> host ", target.getId(), " (target aggregate MIPS=", minTargetMips, ")");
        return new int[]{selectedVm.getId(), target.getId()};
    }
}
