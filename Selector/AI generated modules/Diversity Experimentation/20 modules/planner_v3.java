package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;
import org.cloudbus.cloudsim.core.PowerGuestEntity;
import org.cloudbus.cloudsim.core.PowerHostEntity;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.power.PowerDatacenter;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.power.PowerVm;

import java.util.List;

/**
 * Strategy: busiest-VM eviction with best-fit placement.
 * Finds the most constrained OVERLOADED host (least available MIPS), pulls
 * its busiest VM (by live CPU utilisation), and places it on the feasible
 * host with the smallest remaining MIPS headroom that can still accept it -
 * a classic best-fit bin-packing placement that avoids fragmenting spare
 * capacity across many hosts.
 */
public class planner_v3 implements Planner<LoadState[], int[]> {

    private static final String MODULE_NAME = "planner_v3";

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {
        List<HostEntity> hosts = readSpace.getAllHosts();

        HostEntity source = null;
        double leastAvailable = Double.MAX_VALUE;
        for (int i = 0; i < diagnosis.length && i < hosts.size(); i++) {
            if (diagnosis[i] != LoadState.OVERLOADED) {
                continue;
            }
            HostEntity host = hosts.get(i);
            if (readSpace.isHostFailed(host) || readSpace.isHostPermanentlyDead(host)) {
                continue;
            }
            double available = readSpace.getHostAvailableMips(host);
            if (available < leastAvailable) {
                leastAvailable = available;
                source = host;
            }
        }

        if (source == null) {
            Log.printlnConcat(readSpace.getNow(), ": [" + MODULE_NAME + "] no overloaded host found");
            return new int[0];
        }

        List<GuestEntity> guests = readSpace.getVmListForHost(source);
        GuestEntity busiest = null;
        double busiestUtil = -1.0;
        for (GuestEntity vm : guests) {
            if (readSpace.isVmMigrating(vm) || readSpace.isVmBeingInstantiated(vm)) {
                continue;
            }
            double util = readSpace.getVmCpuUtil(vm);
            if (util > busiestUtil) {
                busiestUtil = util;
                busiest = vm;
            }
        }

        if (busiest == null) {
            Log.printlnConcat(readSpace.getNow(), ": [" + MODULE_NAME + "] overloaded host " + readSpace.getId(source) + " has no migratable vm");
            return new int[0];
        }

        HostEntity target = null;
        double tightestHeadroom = Double.MAX_VALUE;
        for (HostEntity host : hosts) {
            if (host == source) {
                continue;
            }
            if (readSpace.isHostFailed(host) || readSpace.isHostPermanentlyDead(host)) {
                continue;
            }
            if (readSpace.isHostPoweredDown(host) || readSpace.isHostPoweringUp(host)) {
                continue;
            }
            if (!readSpace.canMigrateGuestToHost(host, busiest)) {
                continue;
            }
            double available = readSpace.getHostAvailableMips(host);
            if (available < tightestHeadroom) {
                tightestHeadroom = available;
                target = host;
            }
        }

        if (target == null) {
            Log.printlnConcat(readSpace.getNow(), ": [" + MODULE_NAME + "] no feasible best-fit target host for vm " + readSpace.getId(busiest));
            return new int[0];
        }

        int vmId = readSpace.getId(busiest);
        int hostId = readSpace.getId(target);
        Log.printlnConcat(readSpace.getNow(), ": [" + MODULE_NAME + "] migrating busiest vm " + vmId + " from host " + readSpace.getId(source) + " to best-fit host " + hostId);
        return new int[]{vmId, hostId};
    }

    @Override
    public String inputSemantic() {
        return "host-cpu-load-overload-source";
    }

    @Override
    public String outputSemantic() {
        return "migrate-busiest-vm-best-fit";
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
