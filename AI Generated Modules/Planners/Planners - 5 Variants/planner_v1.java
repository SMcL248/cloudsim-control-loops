package org.cloudbus.cloudsim.examples;

import java.util.List;
import java.util.HashSet;
import java.util.Set;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;

/**
 * Planner v1: CPU-util based — Heaviest-VM, Least-Loaded Destination.
 *
 * From the first overloaded host found, selects the VM with the highest
 * MIPS demand (the primary contributor to overload) and places it on the
 * underloaded host with the lowest total MIPS load that can accept it.
 *
 * Rationale: removing the heaviest VM from the source gives the largest
 * immediate relief; targeting the least-loaded destination avoids
 * inadvertently overloading it.
 */
public class planner_v1 implements Planner<LoadState[], int[]> {

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {
        double now = readSpace.getNow();
        List<HostEntity> hosts = readSpace.getAllHosts();

        // Identify source: first overloaded host
        HostEntity sourceHost = null;
        for (int i = 0; i < diagnosis.length && i < hosts.size(); i++) {
            if (diagnosis[i] == LoadState.OVERLOADED) {
                sourceHost = hosts.get(i);
                break;
            }
        }

        if (sourceHost == null) {
            Log.printlnConcat(now, ": planner_v1: No overloaded host found. No migration.");
            return new int[]{-1, -1};
        }

        // Pick VM with highest MIPS demand from source
        GuestEntity targetVm = null;
        double maxMips = -1.0;
        for (GuestEntity vm : sourceHost.getGuestList()) {
            double mips = vm.getCurrentRequestedTotalMips();
            if (mips > maxMips) {
                maxMips = mips;
                targetVm = vm;
            }
        }

        if (targetVm == null) {
            Log.printlnConcat(now, ": planner_v1: Overloaded host ", sourceHost.getId(), " has no VMs.");
            return new int[]{-1, -1};
        }

        // Find destination: underloaded host with lowest total MIPS load that is suitable
        HostEntity destHost = null;
        double minLoad = Double.MAX_VALUE;
        for (int i = 0; i < diagnosis.length && i < hosts.size(); i++) {
            if (diagnosis[i] != LoadState.UNDERLOADED) continue;
            HostEntity candidate = hosts.get(i);
            if (candidate.getId() == sourceHost.getId()) continue;
            if (!candidate.isSuitableForGuest(targetVm)) continue;
            double load = totalMips(candidate);
            if (load < minLoad) {
                minLoad = load;
                destHost = candidate;
            }
        }

        if (destHost == null) {
            Log.printlnConcat(now, ": planner_v1: No suitable underloaded destination found.");
            return new int[]{-1, -1};
        }

        Log.printlnConcat(now, ": planner_v1: Migrate VM ", targetVm.getId(),
                " (", maxMips, " MIPS) from Host ", sourceHost.getId(),
                " to Host ", destHost.getId(), " (load ", minLoad, " MIPS)");
        return new int[]{targetVm.getId(), destHost.getId()};
    }

    private double totalMips(HostEntity host) {
        double total = 0.0;
        for (GuestEntity vm : host.getGuestList()) {
            total += vm.getCurrentRequestedTotalMips();
        }
        return total;
    }

    @Override
    public String inputGuid() { return "host-cpu-util-loadstate"; }

    @Override
    public String outputGuid() { return "host-migration-pair"; }
}
