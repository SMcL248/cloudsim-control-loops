package org.cloudbus.cloudsim.examples;// always include

import java.util.List;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.HostEntity;
import org.cloudbus.cloudsim.core.GuestEntity;

/**
 * planner_v2
 *
 * Strategy: "Overload relief migration"
 * Host-level diagnosis. Finds the most saturated host flagged OVERLOADED
 * (lowest available MIPS headroom), picks its busiest non-migrating VM
 * (highest CPU utilisation) as the migration candidate, and searches for a
 * destination host that can accept it. Destination search first tries hosts
 * NOT flagged OVERLOADED (preferred), falling back to any host that can
 * accept the VM if no preferred host exists; within a pass, the candidate
 * with the most spare MIPS headroom wins.
 * Emits requestVmMigration{vmId, targetHostId}, or an empty array if no
 * viable source VM / destination host pair exists.
 */
public class planner_v2 implements Planner<LoadState[], int[]> {

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {

        List<HostEntity> hosts = readSpace.getAllHosts();
        int limit = Math.min(diagnosis.length, hosts.size());

        HostEntity worstHost = findMostSaturatedOverloadedHost(readSpace, hosts, diagnosis, limit);
        if (worstHost == null) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v2] ", "no overloaded host found");
            return new int[0];
        }

        GuestEntity vmToMove = findBusiestVm(readSpace, readSpace.getVmListForHost(worstHost));
        if (vmToMove == null) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v2] ", "overloaded host has no movable vm");
            return new int[0];
        }

        // Prefer a destination host that isn't itself diagnosed OVERLOADED;
        // fall back to any host that can accept the VM otherwise.
        HostEntity bestTarget = findBestTarget(readSpace, hosts, diagnosis, limit, worstHost, vmToMove, true);
        if (bestTarget == null) {
            bestTarget = findBestTarget(readSpace, hosts, diagnosis, limit, worstHost, vmToMove, false);
        }

        if (bestTarget == null) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v2] ", "no viable destination host for migration");
            return new int[0];
        }

        int vmId = readSpace.getId(vmToMove);
        int targetHostId = readSpace.getId(bestTarget);
        Log.printlnConcat(readSpace.getNow(), ": [planner_v2] ",
                "migrating vm " + vmId + " off overloaded host to host " + targetHostId);
        return new int[] { vmId, targetHostId };
    }

    private HostEntity findMostSaturatedOverloadedHost(ReadSpace readSpace, List<HostEntity> hosts,
            LoadState[] diagnosis, int limit) {
        HostEntity worstHost = null;
        double lowestHeadroom = Double.MAX_VALUE;

        for (int i = 0; i < limit; i++) {
            if (diagnosis[i] != LoadState.OVERLOADED) {
                continue;
            }
            HostEntity host = hosts.get(i);
            if (readSpace.isHostFailed(host) || readSpace.isHostPermanentlyDead(host)) {
                continue;
            }
            if (readSpace.isHostPoweredDown(host) || readSpace.isHostPoweringUp(host)) {
                continue;
            }

            double headroom = readSpace.getHostAvailableMips(host);
            if (headroom < lowestHeadroom) {
                lowestHeadroom = headroom;
                worstHost = host;
            }
        }
        return worstHost;
    }

    private GuestEntity findBusiestVm(ReadSpace readSpace, List<GuestEntity> candidates) {
        GuestEntity busiest = null;
        double highestUtil = -1.0;
        for (GuestEntity vm : candidates) {
            if (readSpace.isVmMigrating(vm) || readSpace.isVmBeingInstantiated(vm)) {
                continue;
            }
            double util = readSpace.getVmCpuUtil(vm);
            if (util > highestUtil) {
                highestUtil = util;
                busiest = vm;
            }
        }
        return busiest;
    }

    private HostEntity findBestTarget(ReadSpace readSpace, List<HostEntity> hosts, LoadState[] diagnosis, int limit,
            HostEntity exclude, GuestEntity vm, boolean requireNotOverloaded) {
        HostEntity best = null;
        double bestHeadroom = -1.0;

        for (int j = 0; j < hosts.size(); j++) {
            HostEntity candidate = hosts.get(j);
            if (candidate == exclude) {
                continue;
            }
            if (readSpace.isHostFailed(candidate) || readSpace.isHostPermanentlyDead(candidate)) {
                continue;
            }
            if (readSpace.isHostPoweredDown(candidate) || readSpace.isHostPoweringUp(candidate)) {
                continue;
            }
            if (!readSpace.hostHasFreePe(candidate) || !readSpace.canMigrateGuestToHost(candidate, vm)) {
                continue;
            }
            if (requireNotOverloaded && j < limit && diagnosis[j] == LoadState.OVERLOADED) {
                continue;
            }

            double headroom = readSpace.getHostAvailableMips(candidate);
            if (headroom > bestHeadroom) {
                bestHeadroom = headroom;
                best = candidate;
            }
        }
        return best;
    }

    @Override
    public String inputSemantic() {
        return "host-loadstate-classification";
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
