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
 * Strategy: rescue-to-idlest.
 * Among active cloudlets flagged OVERLOADED (their host VM is struggling),
 * picks the first one, finds its current VM, and relocates it to whichever
 * other VM in the fleet is currently least busy - a simple load-balancing
 * rescue that favours immediate relief over global optimality.
 */
public class planner_v14 implements Planner<LoadState[], int[]> {

    private static final String MODULE_NAME = "planner_v14";

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {
        List<Cloudlet> cloudlets = readSpace.getActiveCloudlets();

        Cloudlet flagged = null;
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
            flagged = cl;
            owner = candidateOwner;
            break;
        }

        if (flagged == null) {
            Log.printlnConcat(readSpace.getNow(), ": [" + MODULE_NAME + "] no overloaded cloudlet with a resolvable owning vm found");
            return new int[0];
        }

        GuestEntity idlest = null;
        double lowestUtil = Double.MAX_VALUE;
        for (GuestEntity vm : readSpace.getVmList()) {
            if (readSpace.getId(vm) == readSpace.getId(owner)) {
                continue;
            }
            if (readSpace.isVmMigrating(vm) || readSpace.isVmBeingInstantiated(vm)) {
                continue;
            }
            double util = readSpace.getVmCpuUtil(vm);
            if (util < lowestUtil) {
                lowestUtil = util;
                idlest = vm;
            }
        }

        if (idlest == null) {
            Log.printlnConcat(readSpace.getNow(), ": [" + MODULE_NAME + "] no alternate vm available to receive cloudlet " + readSpace.getId(flagged));
            return new int[0];
        }

        int cloudletId = readSpace.getId(flagged);
        int fromVmId = readSpace.getId(owner);
        int toVmId = readSpace.getId(idlest);
        Log.printlnConcat(readSpace.getNow(), ": [" + MODULE_NAME + "] moving cloudlet " + cloudletId + " from vm " + fromVmId + " to idlest vm " + toVmId);
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

    @Override
    public String inputSemantic() {
        return "cloudlet-host-vm-load-risk";
    }

    @Override
    public String outputSemantic() {
        return "move-cloudlet-to-idlest-vm";
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
