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
 * Strategy: VM-centric tight-host relief.
 * Rather than starting from the host diagnosis, this planner starts from
 * the VM diagnosis: among VMs flagged OVERLOADED it identifies whichever
 * one sits on the host with the least available MIPS (the most contended
 * host, discovered indirectly through its guests), then migrates that VM
 * to the tightest feasible host that can still accept it.
 */
public class planner_v13 implements Planner<LoadState[], int[]> {

    private static final String MODULE_NAME = "planner_v13";

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {
        List<GuestEntity> vms = readSpace.getVmList();

        GuestEntity candidate = null;
        HostEntity candidateHost = null;
        double leastAvailable = Double.MAX_VALUE;

        for (int i = 0; i < diagnosis.length && i < vms.size(); i++) {
            if (diagnosis[i] != LoadState.OVERLOADED) {
                continue;
            }
            GuestEntity vm = vms.get(i);
            if (readSpace.isVmMigrating(vm) || readSpace.isVmBeingInstantiated(vm)) {
                continue;
            }
            HostEntity host = findHostOfVm(vm, readSpace);
            if (host == null) {
                continue;
            }
            double available = readSpace.getHostAvailableMips(host);
            if (available < leastAvailable) {
                leastAvailable = available;
                candidate = vm;
                candidateHost = host;
            }
        }

        if (candidate == null) {
            Log.printlnConcat(readSpace.getNow(), ": [" + MODULE_NAME + "] no overloaded, migratable vm resolved to a host");
            return new int[0];
        }

        HostEntity target = null;
        double tightestHeadroom = Double.MAX_VALUE;
        for (HostEntity host : readSpace.getAllHosts()) {
            if (host == candidateHost) {
                continue;
            }
            if (readSpace.isHostFailed(host) || readSpace.isHostPermanentlyDead(host)) {
                continue;
            }
            if (readSpace.isHostPoweredDown(host) || readSpace.isHostPoweringUp(host)) {
                continue;
            }
            if (!readSpace.canMigrateGuestToHost(host, candidate)) {
                continue;
            }
            double available = readSpace.getHostAvailableMips(host);
            if (available < tightestHeadroom) {
                tightestHeadroom = available;
                target = host;
            }
        }

        if (target == null) {
            Log.printlnConcat(readSpace.getNow(), ": [" + MODULE_NAME + "] no feasible relief host for vm " + readSpace.getId(candidate));
            return new int[0];
        }

        int vmId = readSpace.getId(candidate);
        int hostId = readSpace.getId(target);
        Log.printlnConcat(readSpace.getNow(), ": [" + MODULE_NAME + "] relieving tight host via vm " + vmId + " to host " + hostId);
        return new int[]{vmId, hostId};
    }

    private HostEntity findHostOfVm(GuestEntity vm, ReadSpace readSpace) {
        for (HostEntity host : readSpace.getAllHosts()) {
            List<GuestEntity> guests = readSpace.getVmListForHost(host);
            for (GuestEntity guest : guests) {
                if (readSpace.getId(guest) == readSpace.getId(vm)) {
                    return host;
                }
            }
        }
        return null;
    }

    @Override
    public String inputSemantic() {
        return "vm-cpu-load-host-contention";
    }

    @Override
    public String outputSemantic() {
        return "migrate-vm-tight-host-relief";
    }

    @Override
    public int inputGuid() {
        return 2300;
    }

    @Override
    public int outputGuid() {
        return 3002;
    }
}
