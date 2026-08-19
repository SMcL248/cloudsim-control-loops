package org.cloudbus.cloudsim.examples;// always include

// Import whats needed
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;
import java.util.List;

// Strategy: migration-based host consolidation.
// Finds the most heavily loaded OVERLOADED host (by summed VM CPU utilisation), takes its
// busiest VM, and relocates it to the least-utilised UNDERLOADED host that can actually
// accept it. Classic reactive load-balancing via live migration.
public class planner_v1 implements Planner<LoadState[], int[]> {

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {
        List<HostEntity> hosts = readSpace.getAllHosts();

        int worstHostIdx = -1;
        double worstUtil = -1.0;

        // Find the most heavily loaded OVERLOADED host by summed VM CPU utilisation.
        for (int i = 0; i < diagnosis.length && i < hosts.size(); i++) {
            if (diagnosis[i] != LoadState.OVERLOADED) continue;
            HostEntity host = hosts.get(i);
            if (readSpace.isHostFailed(host) || readSpace.isHostPermanentlyDead(host) || readSpace.isHostPoweredDown(host)) continue;

            double totalUtil = 0.0;
            for (GuestEntity vm : readSpace.getVmListForHost(host)) {
                totalUtil += readSpace.getVmCpuUtil(vm);
            }
            if (totalUtil > worstUtil) {
                worstUtil = totalUtil;
                worstHostIdx = i;
            }
        }

        if (worstHostIdx == -1) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v1] ", "no overloaded host found, no migration planned");
            return new int[0];
        }

        HostEntity sourceHost = hosts.get(worstHostIdx);
        List<GuestEntity> sourceVms = readSpace.getVmListForHost(sourceHost);

        GuestEntity busiestVm = null;
        double busiestUtil = -1.0;
        for (GuestEntity vm : sourceVms) {
            if (readSpace.isVmMigrating(vm)) continue;
            double util = readSpace.getVmCpuUtil(vm);
            if (util > busiestUtil) {
                busiestUtil = util;
                busiestVm = vm;
            }
        }

        if (busiestVm == null) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v1] ", "overloaded host has no movable vm, no migration planned");
            return new int[0];
        }

        // Find least-loaded UNDERLOADED host that can actually accept this VM.
        HostEntity targetHost = null;
        double lowestUtil = Double.MAX_VALUE;
        for (int i = 0; i < diagnosis.length && i < hosts.size(); i++) {
            if (diagnosis[i] != LoadState.UNDERLOADED) continue;
            HostEntity candidate = hosts.get(i);
            if (readSpace.isHostFailed(candidate) || readSpace.isHostPermanentlyDead(candidate) || readSpace.isHostPoweredDown(candidate)) continue;
            if (readSpace.getId(candidate) == readSpace.getId(sourceHost)) continue;
            if (!readSpace.canMigrateGuestToHost(candidate, busiestVm)) continue;

            double totalMips = readSpace.getHostTotalMips(candidate);
            double candUtil = totalMips > 0
                ? (totalMips - readSpace.getHostAvailableMips(candidate)) / totalMips
                : 1.0;
            if (candUtil < lowestUtil) {
                lowestUtil = candUtil;
                targetHost = candidate;
            }
        }

        if (targetHost == null) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v1] ", "no eligible underloaded host can accept busiest vm, no migration planned");
            return new int[0];
        }

        Log.printlnConcat(readSpace.getNow(), ": [planner_v1] planning migration of vm ", readSpace.getId(busiestVm), " from host ", readSpace.getId(sourceHost), " to host ", readSpace.getId(targetHost));

        return new int[] { readSpace.getId(busiestVm), readSpace.getId(targetHost) };
    }

    @Override
    public String inputSemantic() {
        return "host-loadstate-consolidation-signal";
    }

    @Override
    public String outputSemantic() {
        return "requestVmMigration";
    }

    @Override
    public int inputGuid() {
        return 2200;
    }

    @Override
    public int outputGuid() {
        return 3002;
    }
}
