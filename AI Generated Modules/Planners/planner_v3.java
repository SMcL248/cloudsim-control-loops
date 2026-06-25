package org.cloudbus.cloudsim.examples;

import java.util.List;
import java.util.HashSet;
import java.util.Set;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;

/**
 * Planner v3 — Greedy Best-Pair
 *
 * Strategy: Require at least one OVERLOADED host before acting. Then
 * exhaustively scan all VMs and — for each — test whether a valid
 * destination exists (UNDERLOADED preferred, BALANCED as fallback).
 * Among all feasible (VM, destination) pairs, select the one whose VM
 * has the highest requested MIPS, maximising per-migration load relief.
 *
 * Unlike v1, which commits to the highest-MIPS VM and may find no
 * valid destination, v3 guarantees that the returned pair is always
 * placeable, avoiding wasted migration attempts.
 *
 * Trigger:  OVERLOADED host present
 * VM pick:  highest requested MIPS among VMs with a confirmed destination
 * Dest pick: UNDERLOADED first, then BALANCED
 */
public class planner_v3 implements Planner<LoadState[], int[]> {

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {
        double now = readSpace.getNow();
        List<HostEntity> hosts = readSpace.getAllHosts();
        List<GuestEntity> vms   = readSpace.getVmList();
        int bound = Math.min(diagnosis.length, hosts.size());

        // Step 1: Check that at least one OVERLOADED host exists
        boolean anyOverloaded = false;
        for (int i = 0; i < bound; i++) {
            if (diagnosis[i] == LoadState.OVERLOADED) {
                anyOverloaded = true;
                break;
            }
        }

        if (!anyOverloaded) {
            Log.printlnConcat(now, ": [Planner_v3] No overloaded hosts. No migration needed.");
            return new int[]{-1, -1};
        }

        // Step 2: Exhaustive search — find the feasible pair with highest VM MIPS
        GuestEntity bestVm   = null;
        HostEntity  bestHost = null;
        double bestMips = Double.NEGATIVE_INFINITY;

        for (GuestEntity vm : vms) {
            double mips = vm.getCurrentRequestedTotalMips();
            if (mips <= bestMips) {
                // Cannot improve on current best; skip
                continue;
            }

            // Try to find a valid destination for this VM
            HostEntity dest = findDestination(diagnosis, hosts, vm, LoadState.UNDERLOADED, bound);
            if (dest == null) {
                dest = findDestination(diagnosis, hosts, vm, LoadState.BALANCED, bound);
            }

            if (dest != null) {
                bestVm   = vm;
                bestHost = dest;
                bestMips = mips;
            }
        }

        if (bestVm == null) {
            Log.printlnConcat(now, ": [Planner_v3] No feasible migration pair found across all VMs.");
            return new int[]{-1, -1};
        }

        Log.printlnConcat(now, ": [Planner_v3] Best-pair migration: VM ", bestVm.getId(),
                " (MIPS=", bestMips, ") -> Host ", bestHost.getId(), ".");
        return new int[]{bestVm.getId(), bestHost.getId()};
    }

    /**
     * Returns the first host with the target LoadState that can accommodate the VM,
     * or null if none exists.
     */
    private HostEntity findDestination(LoadState[] diagnosis, List<HostEntity> hosts,
                                        GuestEntity vm, LoadState targetState, int bound) {
        for (int i = 0; i < bound; i++) {
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
