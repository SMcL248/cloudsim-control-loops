package org.cloudbus.cloudsim.examples;

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

// Overload-Triggered Migration Planner.
// Finds the first VM flagged OVERLOADED in the VM-level LoadState[], locates
// its current host by scanning host VM lists, then migrates it to the
// eligible non-failed, powered-on host with the most available MIPS
// headroom that can actually accept it.
public class planner_v3 implements Planner<LoadState[], int[]> {

    private static final int INPUT_GUID = 2300;
    private static final int OUTPUT_GUID = 3002;

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {
        List<GuestEntity> vms = readSpace.getVmList();
        List<HostEntity> hosts = readSpace.getAllHosts();

        GuestEntity overloadedVm = null;
        int limit = Math.min(diagnosis.length, vms.size());
        for (int i = 0; i < limit; i++) {
            if (diagnosis[i] == LoadState.OVERLOADED) {
                overloadedVm = vms.get(i);
                break;
            }
        }

        if (overloadedVm == null) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v3] no overloaded vm found, no migration needed");
            return new int[0];
        }

        if (readSpace.isVmMigrating(overloadedVm) || readSpace.isVmBeingInstantiated(overloadedVm)) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v3] target vm already migrating or instantiating, skipping");
            return new int[0];
        }

        HostEntity currentHost = null;
        for (HostEntity host : hosts) {
            List<GuestEntity> hosted = readSpace.getVmListForHost(host);
            for (GuestEntity hostedVm : hosted) {
                if (readSpace.getId(hostedVm) == readSpace.getId(overloadedVm)) {
                    currentHost = host;
                    break;
                }
            }
            if (currentHost != null) {
                break;
            }
        }

        HostEntity bestTarget = null;
        double bestAvailableMips = -1.0;
        for (HostEntity host : hosts) {
            if (currentHost != null && readSpace.getId(host) == readSpace.getId(currentHost)) {
                continue;
            }
            if (readSpace.isHostFailed(host) || readSpace.isHostPermanentlyDead(host)) {
                continue;
            }
            if (readSpace.isHostPoweredDown(host) || readSpace.isHostPoweringUp(host)) {
                continue;
            }
            if (!readSpace.hostHasFreePe(host)) {
                continue;
            }
            if (!readSpace.canMigrateGuestToHost(host, overloadedVm)) {
                continue;
            }
            double available = readSpace.getHostAvailableMips(host);
            if (available > bestAvailableMips) {
                bestAvailableMips = available;
                bestTarget = host;
            }
        }

        if (bestTarget == null) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v3] no suitable migration target found for vm ", readSpace.getId(overloadedVm));
            return new int[0];
        }

        int vmId = readSpace.getId(overloadedVm);
        int hostId = readSpace.getId(bestTarget);
        Log.printlnConcat(readSpace.getNow(), ": [planner_v3] migrating vm ", vmId, " to host ", hostId);
        return new int[] { vmId, hostId };
    }

    @Override
    public String inputSemantic() {
        return "vm-loadstate-classification";
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
