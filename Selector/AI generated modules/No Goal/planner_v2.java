package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;
import java.util.List;

/**
 * Variant 2: Hotspot-relief migration via widest-headroom targeting.
 * Strategy: read host-level diagnosis. For an OVERLOADED host, identify its
 * heaviest-utilization resident VM and migrate it to whichever live,
 * reachable host currently has the greatest available MIPS headroom -
 * spreading load away from congestion rather than optimizing for density.
 */
public class planner_v2 implements Planner<LoadState[], int[]> {

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {
        List<HostEntity> hosts = readSpace.getAllHosts();
        int considered = Math.min(diagnosis.length, hosts.size());

        for (int i = 0; i < considered; i++) {
            if (diagnosis[i] != LoadState.OVERLOADED) {
                continue;
            }
            HostEntity sourceHost = hosts.get(i);
            if (readSpace.isHostFailed(sourceHost) || readSpace.isHostPermanentlyDead(sourceHost)) {
                continue;
            }

            GuestEntity heaviest = null;
            double heaviestUtil = -1.0;
            for (GuestEntity vm : readSpace.getVmListForHost(sourceHost)) {
                if (readSpace.isVmMigrating(vm) || readSpace.isVmBeingInstantiated(vm)) {
                    continue;
                }
                double util = readSpace.getVmCpuUtil(vm);
                if (util > heaviestUtil) {
                    heaviestUtil = util;
                    heaviest = vm;
                }
            }
            if (heaviest == null) {
                continue;
            }

            HostEntity bestTarget = null;
            double bestHeadroom = -1.0;
            for (HostEntity candidate : hosts) {
                if (readSpace.getId(candidate) == readSpace.getId(sourceHost)) {
                    continue;
                }
                if (readSpace.isHostFailed(candidate) || readSpace.isHostPermanentlyDead(candidate)
                        || readSpace.isHostPoweredDown(candidate) || readSpace.isHostPoweringUp(candidate)) {
                    continue;
                }
                if (!readSpace.hostHasFreePe(candidate)) {
                    continue;
                }
                if (!readSpace.canMigrateGuestToHost(candidate, heaviest)) {
                    continue;
                }
                double headroom = readSpace.getHostAvailableMips(candidate);
                if (headroom > bestHeadroom) {
                    bestHeadroom = headroom;
                    bestTarget = candidate;
                }
            }

            if (bestTarget != null) {
                int vmId = readSpace.getId(heaviest);
                int hostId = readSpace.getId(bestTarget);
                Log.printlnConcat(readSpace.getNow(), ": [planner_v2] ",
                        "Host overloaded, migrating heaviest VM " + vmId + " to widest-headroom host " + hostId);
                return new int[]{vmId, hostId};
            }
        }

        Log.printlnConcat(readSpace.getNow(), ": [planner_v2] ",
                "No feasible relief migration found.");
        return new int[]{-1, -1};
    }

    @Override
    public String inputSemantic() {
        return "host-loadstate-overload-relief";
    }

    @Override
    public String outputSemantic() {
        return "vm-migration-relief";
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
