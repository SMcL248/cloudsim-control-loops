package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;
import java.util.List;

public class planner_v15 implements Planner<LoadState[], int[]> {

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {
        List<HostEntity> hosts = readSpace.getAllHosts();
        int limit = Math.min(diagnosis.length, hosts.size());

        HostEntity sourceHost = null;
        for (int i = 0; i < limit; i++) {
            if (diagnosis[i] != LoadState.UNDERLOADED) {
                continue;
            }
            HostEntity host = hosts.get(i);
            if (readSpace.isHostFailed(host) || readSpace.isHostPermanentlyDead(host) || readSpace.isHostPoweredDown(host)) {
                continue;
            }
            if (!readSpace.getVmListForHost(host).isEmpty()) {
                sourceHost = host;
                break;
            }
        }

        if (sourceHost == null) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v15] no underloaded, occupied host found for evacuation");
            return new int[0];
        }

        List<GuestEntity> hosted = readSpace.getVmListForHost(sourceHost);
        GuestEntity vmToMove = null;
        for (GuestEntity vm : hosted) {
            if (!readSpace.isVmMigrating(vm) && !readSpace.isVmBeingInstantiated(vm)) {
                vmToMove = vm;
                break;
            }
        }

        if (vmToMove == null) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v15] host ", readSpace.getId(sourceHost), " has no movable VM right now");
            return new int[0];
        }

        // Best-fit target: smallest available-MIPS host that can still take the VM,
        // to pack load densely and free whole hosts for later power-down.
        HostEntity bestFitTarget = null;
        double tightestHeadroom = Double.MAX_VALUE;
        for (HostEntity host : hosts) {
            if (host == sourceHost || readSpace.isHostFailed(host) || readSpace.isHostPermanentlyDead(host) || readSpace.isHostPoweredDown(host)) {
                continue;
            }
            if (!readSpace.canMigrateGuestToHost(host, vmToMove)) {
                continue;
            }
            double headroom = readSpace.getHostAvailableMips(host);
            if (headroom < tightestHeadroom) {
                tightestHeadroom = headroom;
                bestFitTarget = host;
            }
        }

        if (bestFitTarget == null) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v15] no best-fit target host found for VM ", readSpace.getId(vmToMove));
            return new int[0];
        }

        int vmId = readSpace.getId(vmToMove);
        int targetHostId = readSpace.getId(bestFitTarget);
        Log.printlnConcat(readSpace.getNow(), ": [planner_v15] consolidating VM ", vmId, " onto best-fit host ", targetHostId, " to vacate source host");
        return new int[] { vmId, targetHostId };
    }

    @Override
    public String inputSemantic() {
        return "host-loadstate-underload-source";
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
