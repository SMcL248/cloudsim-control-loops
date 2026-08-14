package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;

import java.util.List;

// Strategy: throughput-led with a soft power constraint. Diagnosis is per-host. Evacuates the biggest
// MIPS-demanding VM off the most severely overloaded host, but only onto a target host that is already
// powered on, so relief never requires waking new capacity.
public class planner_v2 implements Planner<LoadState[], int[]> {

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {

        List<HostEntity> hosts = readSpace.getAllHosts();

        if (diagnosis == null || hosts == null || diagnosis.length != hosts.size()) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v2] diagnosis/host list mismatch, no-op");
            return new int[0];
        }

        // Find the overloaded host with the least spare MIPS: most urgent relief target.
        int sourceIndex = -1;
        double worstSpare = Double.MAX_VALUE;
        for (int i = 0; i < diagnosis.length; i++) {
            HostEntity h = hosts.get(i);
            if (diagnosis[i] == LoadState.OVERLOADED && !readSpace.isHostFailed(h) && !readSpace.isHostPermanentlyDead(h)) {
                double spare = readSpace.getHostAvailableMips(h);
                if (spare < worstSpare) {
                    worstSpare = spare;
                    sourceIndex = i;
                }
            }
        }

        if (sourceIndex == -1) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v2] no overloaded host found, no-op");
            return new int[0];
        }

        HostEntity sourceHost = hosts.get(sourceIndex);
        List<GuestEntity> hostedVms = readSpace.getVmListForHost(sourceHost);

        if (hostedVms == null || hostedVms.isEmpty()) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v2] overloaded host ", readSpace.getId(sourceHost), " has no VMs to migrate, no-op");
            return new int[0];
        }

        // Migrate the VM demanding the most MIPS - the single biggest contributor to the overload.
        GuestEntity vmToMove = null;
        double biggestDemand = -1;
        for (GuestEntity vm : hostedVms) {
            if (readSpace.isVmMigrating(vm)) continue;
            double demand = readSpace.getVmRequestedMips(vm);
            if (demand > biggestDemand) {
                biggestDemand = demand;
                vmToMove = vm;
            }
        }

        if (vmToMove == null) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v2] all VMs on host ", readSpace.getId(sourceHost), " already migrating, no-op");
            return new int[0];
        }

        // Prefer a target host that is already powered and not itself overloaded, avoiding the extra
        // energy cost of waking a powered-down host purely to relieve a throughput bottleneck.
        int targetIndex = -1;
        double bestSpare = -1;
        for (int i = 0; i < diagnosis.length; i++) {
            HostEntity h = hosts.get(i);
            if (i == sourceIndex) continue;
            if (readSpace.isHostFailed(h) || readSpace.isHostPermanentlyDead(h) || readSpace.isHostPoweredDown(h)) continue;
            if (diagnosis[i] == LoadState.OVERLOADED) continue;
            if (!readSpace.canMigrateGuestToHost(h, vmToMove)) continue;
            double spare = readSpace.getHostAvailableMips(h);
            if (spare > bestSpare) {
                bestSpare = spare;
                targetIndex = i;
            }
        }

        if (targetIndex == -1) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v2] no suitable powered target host found, no-op");
            return new int[0];
        }

        int vmId = readSpace.getId(vmToMove);
        int targetHostId = readSpace.getId(hosts.get(targetIndex));

        Log.printlnConcat(readSpace.getNow(), ": [planner_v2] migrating VM ", vmId, " off overloaded host ", readSpace.getId(sourceHost), " to host ", targetHostId);

        return new int[] { vmId, targetHostId };
    }

    @Override
    public String inputSemantic() {
        return "host-mips-load-state";
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
