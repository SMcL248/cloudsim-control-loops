package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;
import java.util.List;

/**
 * Variant 2 - Drain-and-Consolidate Best-Fit Migration.
 *
 * Strategy: rather than waiting for a host to become fully empty, actively
 * shrinks the number of active hosts by migrating VMs off the least-loaded
 * active host onto the tightest-fitting host that can still take them
 * (best-fit bin packing). Repeated application drains lightly-loaded hosts
 * one VM at a time so they eventually qualify for power-down, while never
 * spreading load onto an already-idle host (which would work against
 * consolidation).
 */
public class planner_v2 implements Planner<LoadState[], int[]> {

    private static final int INPUT_GUID = 2200;
    private static final int OUTPUT_GUID = 3002;

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {
        List<HostEntity> hosts = readSpace.getAllHosts();
        int limit = Math.min(diagnosis.length, hosts.size());

        // Step 1: pick the drain candidate - an active, non-empty, UNDERLOADED
        // host with the fewest resident VMs (cheapest to fully empty).
        HostEntity sourceHost = null;
        int fewestVms = Integer.MAX_VALUE;
        for (int i = 0; i < limit; i++) {
            if (diagnosis[i] != LoadState.UNDERLOADED) {
                continue;
            }
            HostEntity host = hosts.get(i);
            if (readSpace.isHostFailed(host) || readSpace.isHostPoweredDown(host)
                    || readSpace.isHostPoweringUp(host)) {
                continue;
            }
            List<GuestEntity> resident = readSpace.getVmListForHost(host);
            if (resident.isEmpty() || resident.size() >= fewestVms) {
                continue;
            }
            fewestVms = resident.size();
            sourceHost = host;
        }

        int[] noOp = new int[]{-1, -1};
        if (sourceHost == null) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v2] no drainable underloaded host found, emitting no-op");
            return noOp;
        }

        // Step 2: pick the least busy VM on that host to move first (least
        // disruptive migration).
        GuestEntity vmToMove = null;
        double lowestUtil = Double.MAX_VALUE;
        for (GuestEntity vm : readSpace.getVmListForHost(sourceHost)) {
            if (readSpace.isVmMigrating(vm) || readSpace.isVmBeingInstantiated(vm)) {
                continue;
            }
            double util = readSpace.getVmCpuUtil(vm);
            if (util < lowestUtil) {
                lowestUtil = util;
                vmToMove = vm;
            }
        }
        if (vmToMove == null) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v2] drain candidate host has no movable VM, emitting no-op");
            return noOp;
        }

        // Step 3: best-fit target - among all other active hosts that can
        // actually accept the VM, pick the one with the least available MIPS
        // headroom (tightest fit), packing load as densely as possible.
        HostEntity bestTarget = null;
        double tightestHeadroom = Double.MAX_VALUE;
        for (int i = 0; i < hosts.size(); i++) {
            HostEntity candidate = hosts.get(i);
            if (candidate == sourceHost) {
                continue;
            }
            if (readSpace.isHostFailed(candidate) || readSpace.isHostPoweredDown(candidate)
                    || readSpace.isHostPoweringUp(candidate)) {
                continue;
            }
            if (!readSpace.canMigrateGuestToHost(candidate, vmToMove)) {
                continue;
            }
            double headroom = readSpace.getHostAvailableMips(candidate);
            if (headroom < tightestHeadroom) {
                tightestHeadroom = headroom;
                bestTarget = candidate;
            }
        }

        if (bestTarget == null) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v2] no viable best-fit target host, emitting no-op");
            return noOp;
        }

        int vmId = readSpace.getId(vmToMove);
        int targetHostId = readSpace.getId(bestTarget);
        Log.printlnConcat(readSpace.getNow(), ": [planner_v2] migrating vm ", vmId,
                " off underloaded host toward best-fit host ", targetHostId);
        return new int[]{vmId, targetHostId};
    }

    @Override
    public String inputSemantic() {
        return "host-underutilisation-loadstate";
    }

    @Override
    public String outputSemantic() {
        return "requestVmMigration";
    }

    @Override
    public int inputGuid() {
        return INPUT_GUID;
    }

    @Override
    public int outputGuid() {
        return OUTPUT_GUID;
    }
}
