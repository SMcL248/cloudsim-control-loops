package org.cloudbus.cloudsim.examples;

import java.util.List;
import java.util.HashSet;
import java.util.Set;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;

/**
 * Planner v4: CPU-demand based — Best-Fit Decreasing (global pair search).
 *
 * Evaluates every (overloaded-source VM, underloaded-destination host) pair
 * and selects the one that maximises the destination's post-migration MIPS load.
 * This is a best-fit packing strategy: pack the destination as tightly as
 * possible (within suitability bounds) to leave other underloaded hosts free
 * to consolidate further workloads later.
 *
 * Rationale: greedily maximising destination utilisation reduces the total
 * number of active hosts over time, aiding energy efficiency.
 */
public class planner_v4 implements Planner<LoadState[], int[]> {

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {
        double now = readSpace.getNow();
        List<HostEntity> hosts = readSpace.getAllHosts();

        // Collect indices of overloaded and underloaded hosts
        Set<Integer> overloadedIdx = new HashSet<>();
        Set<Integer> underloadedIdx = new HashSet<>();
        for (int i = 0; i < diagnosis.length && i < hosts.size(); i++) {
            if (diagnosis[i] == LoadState.OVERLOADED)   overloadedIdx.add(i);
            else if (diagnosis[i] == LoadState.UNDERLOADED) underloadedIdx.add(i);
        }

        if (overloadedIdx.isEmpty()) {
            Log.printlnConcat(now, ": planner_v4: No overloaded hosts. No migration.");
            return new int[]{-1, -1};
        }
        if (underloadedIdx.isEmpty()) {
            Log.printlnConcat(now, ": planner_v4: No underloaded hosts. No migration.");
            return new int[]{-1, -1};
        }

        GuestEntity bestVm = null;
        HostEntity bestSource = null;
        HostEntity bestDest = null;
        double bestPostLoad = -1.0;

        for (int si : overloadedIdx) {
            HostEntity source = hosts.get(si);
            for (GuestEntity vm : source.getGuestList()) {
                double vmMips = vm.getCurrentRequestedTotalMips();
                for (int di : underloadedIdx) {
                    if (di == si) continue;
                    HostEntity dest = hosts.get(di);
                    if (!dest.isSuitableForGuest(vm)) continue;
                    double postLoad = totalMips(dest) + vmMips;
                    // Best-fit: maximise post-migration destination load
                    if (postLoad > bestPostLoad) {
                        bestPostLoad = postLoad;
                        bestVm = vm;
                        bestSource = source;
                        bestDest = dest;
                    }
                }
            }
        }

        if (bestVm == null) {
            Log.printlnConcat(now, ": planner_v4: No feasible migration pair found.");
            return new int[]{-1, -1};
        }

        Log.printlnConcat(now, ": planner_v4: Best-fit migrate VM ", bestVm.getId(),
                " (", bestVm.getCurrentRequestedTotalMips(), " MIPS)",
                " from Host ", bestSource.getId(),
                " to Host ", bestDest.getId(),
                " (dest post-load ", bestPostLoad, " MIPS)");
        return new int[]{bestVm.getId(), bestDest.getId()};
    }

    private double totalMips(HostEntity host) {
        double total = 0.0;
        for (GuestEntity vm : host.getGuestList()) {
            total += vm.getCurrentRequestedTotalMips();
        }
        return total;
    }

    @Override
    public String inputGuid() { return "host-cpu-demand-loadstate"; }

    @Override
    public String outputGuid() { return "host-migration-pair"; }
}
