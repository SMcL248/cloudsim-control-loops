package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;
import java.util.List;

public class planner_v3 implements Planner<LoadState[], int[]> {

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {
        List<HostEntity> hosts = readSpace.getAllHosts();
        HostEntity sourceHost = null;

        int limit = Math.min(diagnosis.length, hosts.size());
        for (int i = 0; i < limit; i++) {
            if (diagnosis[i] == LoadState.OVERLOADED) {
                HostEntity host = hosts.get(i);
                if (!readSpace.isHostFailed(host) && !readSpace.isHostPermanentlyDead(host)) {
                    sourceHost = host;
                    break;
                }
            }
        }

        if (sourceHost == null) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v3] no overloaded host found");
            return new int[0];
        }

        List<GuestEntity> candidates = readSpace.getVmListForHost(sourceHost);
        GuestEntity busiestVm = null;
        double worstUtil = -1;
        for (GuestEntity vm : candidates) {
            if (readSpace.isVmMigrating(vm) || readSpace.isVmBeingInstantiated(vm)) {
                continue;
            }
            double util = readSpace.getVmCpuUtil(vm);
            if (util > worstUtil) {
                worstUtil = util;
                busiestVm = vm;
            }
        }

        if (busiestVm == null) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v3] overloaded host ", readSpace.getId(sourceHost), " has no migratable VM");
            return new int[0];
        }

        HostEntity bestTarget = null;
        double mostHeadroom = -1;
        for (HostEntity host : hosts) {
            if (host == sourceHost || readSpace.isHostFailed(host) || readSpace.isHostPermanentlyDead(host) || readSpace.isHostPoweredDown(host)) {
                continue;
            }
            if (!readSpace.canMigrateGuestToHost(host, busiestVm)) {
                continue;
            }
            double headroom = readSpace.getHostAvailableMips(host);
            if (headroom > mostHeadroom) {
                mostHeadroom = headroom;
                bestTarget = host;
            }
        }

        if (bestTarget == null) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v3] no eligible target host for VM ", readSpace.getId(busiestVm));
            return new int[0];
        }

        int vmId = readSpace.getId(busiestVm);
        int targetHostId = readSpace.getId(bestTarget);
        Log.printlnConcat(readSpace.getNow(), ": [planner_v3] migrating VM ", vmId, " off overloaded host to host ", targetHostId, " with headroom=", mostHeadroom);
        return new int[] { vmId, targetHostId };
    }

    @Override
    public String inputSemantic() {
        return "host-loadstate-overload-source";
    }

    @Override
    public String outputSemantic() {
        return "vm-migration";
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
