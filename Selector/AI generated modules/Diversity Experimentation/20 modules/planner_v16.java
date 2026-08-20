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
 * Strategy: cloudlet-granularity consolidation.
 * Among cloudlets flagged UNDERLOADED (their VM has spare capacity to
 * give away), picks one and packs it onto the busiest VM in the fleet
 * that still has headroom below full utilisation - tight-packing
 * workloads onto fewer, fuller VMs so that emptied VMs become
 * consolidation or power-down candidates elsewhere in the loop.
 */
public class planner_v16 implements Planner<LoadState[], int[]> {

    private static final String MODULE_NAME = "planner_v16";
    private static final double HEADROOM_CEILING = 0.95;

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {
        List<Cloudlet> cloudlets = readSpace.getActiveCloudlets();

        Cloudlet flagged = null;
        GuestEntity owner = null;
        int limit = Math.min(diagnosis.length, cloudlets.size());
        for (int i = 0; i < limit; i++) {
            if (diagnosis[i] != LoadState.UNDERLOADED) {
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
            Log.printlnConcat(readSpace.getNow(), ": [" + MODULE_NAME + "] no underloaded cloudlet with a resolvable owning vm found");
            return new int[0];
        }

        GuestEntity target = null;
        double highestUtilBelowCeiling = -1.0;
        for (GuestEntity vm : readSpace.getVmList()) {
            if (readSpace.getId(vm) == readSpace.getId(owner)) {
                continue;
            }
            if (readSpace.isVmMigrating(vm) || readSpace.isVmBeingInstantiated(vm)) {
                continue;
            }
            double util = readSpace.getVmCpuUtil(vm);
            if (util >= HEADROOM_CEILING) {
                continue;
            }
            if (util > highestUtilBelowCeiling) {
                highestUtilBelowCeiling = util;
                target = vm;
            }
        }

        if (target == null) {
            Log.printlnConcat(readSpace.getNow(), ": [" + MODULE_NAME + "] no consolidation target vm with headroom found for cloudlet " + readSpace.getId(flagged));
            return new int[0];
        }

        int cloudletId = readSpace.getId(flagged);
        int fromVmId = readSpace.getId(owner);
        int toVmId = readSpace.getId(target);
        Log.printlnConcat(readSpace.getNow(), ": [" + MODULE_NAME + "] consolidating cloudlet " + cloudletId + " from vm " + fromVmId + " onto tightly-packed vm " + toVmId);
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
        return "cloudlet-host-vm-load-spare-capacity";
    }

    @Override
    public String outputSemantic() {
        return "consolidate-cloudlet-tight-pack";
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
