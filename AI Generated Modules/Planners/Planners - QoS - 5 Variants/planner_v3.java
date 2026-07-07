package org.cloudbus.cloudsim.examples;

import java.util.List;
import java.util.HashSet;
import java.util.Set;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;

/**
 * Planner v3: VM-count based — Balance VM density across hosts.
 *
 * From the overloaded host hosting the most VMs, picks its first VM and
 * places it on the underloaded host with the fewest VMs that can accept it.
 *
 * All scoring and selection operate on VM counts only (no MIPS values),
 * consistent with a count-based load state diagnosis. This makes sense when
 * the analyser flagged overload based on the number of co-located VMs rather
 * than raw CPU demand.
 */
public class planner_v3 implements Planner<LoadState[], int[]> {

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {
        double now = readSpace.getNow();
        List<HostEntity> hosts = readSpace.getAllHosts();

        // Find overloaded host with the highest VM count
        HostEntity sourceHost = null;
        int maxVmCount = -1;
        for (int i = 0; i < diagnosis.length && i < hosts.size(); i++) {
            if (diagnosis[i] != LoadState.OVERLOADED) continue;
            HostEntity host = hosts.get(i);
            int count = host.getGuestList().size();
            if (count > maxVmCount) {
                maxVmCount = count;
                sourceHost = host;
            }
        }

        if (sourceHost == null) {
            Log.printlnConcat(now, ": planner_v3: No overloaded host found. No migration.");
            return new int[]{-1, -1};
        }

        List<GuestEntity> sourceVms = sourceHost.getGuestList();
        if (sourceVms.isEmpty()) {
            Log.printlnConcat(now, ": planner_v3: Overloaded host ", sourceHost.getId(), " has no VMs.");
            return new int[]{-1, -1};
        }

        // Pick first VM (count-based policy: any VM reduces the count equally)
        GuestEntity targetVm = sourceVms.get(0);

        // Find underloaded host with fewest VMs that is suitable
        HostEntity destHost = null;
        int minVmCount = Integer.MAX_VALUE;
        for (int i = 0; i < diagnosis.length && i < hosts.size(); i++) {
            if (diagnosis[i] != LoadState.UNDERLOADED) continue;
            HostEntity candidate = hosts.get(i);
            if (candidate.getId() == sourceHost.getId()) continue;
            if (!candidate.isSuitableForGuest(targetVm)) continue;
            int count = candidate.getGuestList().size();
            if (count < minVmCount) {
                minVmCount = count;
                destHost = candidate;
            }
        }

        if (destHost == null) {
            Log.printlnConcat(now, ": planner_v3: No suitable underloaded destination found.");
            return new int[]{-1, -1};
        }

        Log.printlnConcat(now, ": planner_v3: VM-count balance migrate VM ", targetVm.getId(),
                " from Host ", sourceHost.getId(), " (", maxVmCount, " VMs)",
                " to Host ", destHost.getId(), " (", minVmCount, " VMs)");
        return new int[]{targetVm.getId(), destHost.getId()};
    }

    @Override
    public String inputGuid() { return "host-vm-count-loadstate"; }

    @Override
    public String outputGuid() { return "host-migration-pair"; }
}
