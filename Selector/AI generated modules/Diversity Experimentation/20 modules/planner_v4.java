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
 * Strategy: heaviest-consumer eviction with worst-fit spread.
 * Finds the most constrained OVERLOADED host, evicts the VM with the
 * largest requested MIPS footprint (the biggest single contributor to the
 * congestion), and spreads it onto whichever feasible host currently has
 * the MOST spare MIPS. Worst-fit placement deliberately trades packing
 * density for maximum future slack on the receiving host.
 */
public class planner_v4 implements Planner<LoadState[], int[]> {

    private static final String MODULE_NAME = "planner_v4";

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
        GuestEntity heaviest = null;
        double heaviestMips = -1.0;
        for (GuestEntity vm : guests) {
            if (readSpace.isVmMigrating(vm) || readSpace.isVmBeingInstantiated(vm)) {
                continue;
            }
            double requested = readSpace.getVmRequestedMips(vm);
            if (requested > heaviestMips) {
                heaviestMips = requested;
                heaviest = vm;
            }
        }

        if (heaviest == null) {
            Log.printlnConcat(readSpace.getNow(), ": [" + MODULE_NAME + "] overloaded host " + readSpace.getId(source) + " has no migratable vm");
            return new int[0];
        }

        HostEntity target = null;
        double mostSlack = -1.0;
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
            if (!readSpace.canMigrateGuestToHost(host, heaviest)) {
                continue;
            }
            double available = readSpace.getHostAvailableMips(host);
            if (available > mostSlack) {
                mostSlack = available;
                target = host;
            }
        }

        if (target == null) {
            Log.printlnConcat(readSpace.getNow(), ": [" + MODULE_NAME + "] no feasible worst-fit target host for vm " + readSpace.getId(heaviest));
            return new int[0];
        }

        int vmId = readSpace.getId(heaviest);
        int hostId = readSpace.getId(target);
        Log.printlnConcat(readSpace.getNow(), ": [" + MODULE_NAME + "] spreading heaviest vm " + vmId + " from host " + readSpace.getId(source) + " to slack host " + hostId);
        return new int[]{vmId, hostId};
    }

    @Override
    public String inputSemantic() {
        return "host-cpu-load-overload-source";
    }

    @Override
    public String outputSemantic() {
        return "migrate-vm-worst-fit-spread";
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
