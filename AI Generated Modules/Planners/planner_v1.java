package org.cloudbus.cloudsim.examples;

import java.util.List;
import java.util.HashSet;
import java.util.Set;

import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;
import org.cloudbus.cloudsim.Log;

/**
 * Planner v1 — Host-level VM migration planner.
 *
 * Strategy:
 *   1. Find the first OVERLOADED host and select its highest-MIPS VM as the migration candidate.
 *   2. Search for a suitable destination host, preferring UNDERLOADED hosts over BALANCED ones.
 *   3. Return [vmId, destHostId], or {-1, -1} if no migration is possible or needed.
 *
 * Input GUID  : host-loadstate
 * Output GUID : host-migration-pair
 */
public class planner_v1 implements Planner<LoadState[], int[]> {

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {
        double now = readSpace.getNow();
        List<HostEntity> hosts = readSpace.getAllHosts();

        if (diagnosis == null || diagnosis.length != hosts.size()) {
            Log.printlnConcat(now, ": [Planner v1] Diagnosis length mismatch (expected ",
                    hosts.size(), ", got ", (diagnosis == null ? "null" : diagnosis.length), "). Aborting.");
            return new int[]{-1, -1};
        }

        // --- Step 1: Identify migration candidate from an overloaded host ---
        HostEntity sourceHost = null;
        GuestEntity vmToMigrate = null;

        for (int i = 0; i < hosts.size(); i++) {
            if (diagnosis[i] != LoadState.OVERLOADED) continue;

            HostEntity host = hosts.get(i);
            List<GuestEntity> vms = readSpace.getVmList();
            if (vms.isEmpty()) continue;

            // Select the VM with the highest current MIPS demand
            GuestEntity bestVm = null;
            double maxMips = -1.0;
            for (GuestEntity vm : vms) {
                double mips = vm.getCurrentRequestedTotalMips();
                if (mips > maxMips) {
                    maxMips = mips;
                    bestVm = vm;
                }
            }

            if (bestVm != null) {
                sourceHost = host;
                vmToMigrate = bestVm;
                Log.printlnConcat(now, ": [Planner v1] Overloaded host ", sourceHost.getId(),
                        " — selected VM ", vmToMigrate.getId(), " (", maxMips, " MIPS) as migration candidate.");
                break;
            }
        }

        if (vmToMigrate == null) {
            Log.printlnConcat(now, ": [Planner v1] No overloaded host with migratable VMs. No migration needed.");
            return new int[]{-1, -1};
        }

        // --- Step 2: Find a suitable destination host ---
        // First preference: UNDERLOADED hosts; fallback: BALANCED hosts.
        HostEntity destHost = findDestination(hosts, diagnosis, sourceHost, vmToMigrate, LoadState.UNDERLOADED);
        if (destHost == null) {
            destHost = findDestination(hosts, diagnosis, sourceHost, vmToMigrate, LoadState.BALANCED);
        }

        if (destHost == null) {
            Log.printlnConcat(now, ": [Planner v1] No suitable destination found for VM ",
                    vmToMigrate.getId(), ". Migration aborted.");
            return new int[]{-1, -1};
        }

        Log.printlnConcat(now, ": [Planner v1] Migration plan: VM ", vmToMigrate.getId(),
                " from Host ", sourceHost.getId(), " -> Host ", destHost.getId());
        return new int[]{vmToMigrate.getId(), destHost.getId()};
    }

    /**
     * Scans the host list for the first host matching the target LoadState that
     * is not the source host and can accommodate the candidate VM.
     */
    private HostEntity findDestination(List<HostEntity> hosts, LoadState[] diagnosis,
                                       HostEntity sourceHost, GuestEntity vm, LoadState targetState) {
        for (int i = 0; i < hosts.size(); i++) {
            if (diagnosis[i] != targetState) continue;
            HostEntity candidate = hosts.get(i);
            if (candidate.getId() == sourceHost.getId()) continue;
            if (candidate.isSuitableForGuest(vm)) {
                return candidate;
            }
        }
        return null;
    }

    @Override
    public String inputGuid() {
        return "host-loadstate";
    }

    @Override
    public String outputGuid() {
        return "host-migration-pair";
    }
}
