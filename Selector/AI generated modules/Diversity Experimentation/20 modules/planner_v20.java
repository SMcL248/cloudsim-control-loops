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
 * Strategy: safety-first evacuation.
 * Among cloudlets flagged OVERLOADED, resolves the owning VM and its host,
 * and if that host is failed or permanently dead, evacuates the cloudlet
 * to the very first other VM found whose host is healthy - a pure
 * first-fit scan with no comparison or optimisation step, prioritising
 * getting the cloudlet off a doomed host quickly over finding the best
 * possible destination.
 */
public class planner_v20 implements Planner<LoadState[], int[]> {

    private static final String MODULE_NAME = "planner_v20";

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {
        List<Cloudlet> cloudlets = readSpace.getActiveCloudlets();

        Cloudlet atRisk = null;
        GuestEntity owner = null;
        int limit = Math.min(diagnosis.length, cloudlets.size());
        for (int i = 0; i < limit; i++) {
            if (diagnosis[i] != LoadState.OVERLOADED) {
                continue;
            }
            Cloudlet cl = cloudlets.get(i);
            GuestEntity candidateOwner = findOwningVm(cl, readSpace);
            if (candidateOwner == null) {
                continue;
            }
            HostEntity host = findHostOfVm(candidateOwner, readSpace);
            if (host == null) {
                continue;
            }
            if (readSpace.isHostFailed(host) || readSpace.isHostPermanentlyDead(host)) {
                atRisk = cl;
                owner = candidateOwner;
                break;
            }
        }

        if (atRisk == null) {
            Log.printlnConcat(readSpace.getNow(), ": [" + MODULE_NAME + "] no overloaded cloudlet on a failed or dead host found");
            return new int[0];
        }

        GuestEntity safeTarget = null;
        for (GuestEntity vm : readSpace.getVmList()) {
            if (readSpace.getId(vm) == readSpace.getId(owner)) {
                continue;
            }
            if (readSpace.isVmMigrating(vm) || readSpace.isVmBeingInstantiated(vm)) {
                continue;
            }
            HostEntity host = findHostOfVm(vm, readSpace);
            if (host == null) {
                continue;
            }
            if (readSpace.isHostFailed(host) || readSpace.isHostPermanentlyDead(host) || readSpace.isHostPoweredDown(host)) {
                continue;
            }
            safeTarget = vm;
            break;
        }

        if (safeTarget == null) {
            Log.printlnConcat(readSpace.getNow(), ": [" + MODULE_NAME + "] no safe vm found to evacuate cloudlet " + readSpace.getId(atRisk));
            return new int[0];
        }

        int cloudletId = readSpace.getId(atRisk);
        int fromVmId = readSpace.getId(owner);
        int toVmId = readSpace.getId(safeTarget);
        Log.printlnConcat(readSpace.getNow(), ": [" + MODULE_NAME + "] emergency evacuating cloudlet " + cloudletId + " from vm " + fromVmId + " to safe vm " + toVmId);
        return new int[]{cloudletId, fromVmId, toVmId};
    }

    private GuestEntity findOwningVm(Cloudlet cl, ReadSpace readSpace) {
        for (GuestEntity vm : readSpace.getVmList()) {
            List<Cloudlet> assigned = readSpace.getVmCloudletList(vm);
            for (Cloudlet c : assigned) {
                if (readSpace.getId(c) == readSpace.getId(cl)) {
                    return vm;
                }
            }
        }
        return null;
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
        return "cloudlet-host-failure-risk";
    }

    @Override
    public String outputSemantic() {
        return "move-cloudlet-safety-first-fit";
    }

    @Override
    public int inputGuid() {
        return 2400;
    }

    @Override
    public int outputGuid() {
        return 3001;
    }
}
