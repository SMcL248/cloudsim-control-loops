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
 * Strategy: estimated-finish-time optimisation.
 * Among cloudlets flagged OVERLOADED, finds the one with the worst
 * estimated finish time on its current VM, then evaluates every other VM
 * in the fleet to find the one that would genuinely minimise that
 * cloudlet's estimated finish time - only moving it if a strict
 * improvement exists.
 */
public class planner_v15 implements Planner<LoadState[], int[]> {

    private static final String MODULE_NAME = "planner_v15";

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {
        List<Cloudlet> cloudlets = readSpace.getActiveCloudlets();

        Cloudlet worstCloudlet = null;
        GuestEntity owner = null;
        double worstFinish = -1.0;
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
            double finish = readSpace.getCloudletEstimatedFinishTime(candidateOwner, cl);
            if (finish > worstFinish) {
                worstFinish = finish;
                worstCloudlet = cl;
                owner = candidateOwner;
            }
        }

        if (worstCloudlet == null) {
            Log.printlnConcat(readSpace.getNow(), ": [" + MODULE_NAME + "] no overloaded cloudlet with resolvable owner found");
            return new int[0];
        }

        GuestEntity bestVm = null;
        double bestFinish = worstFinish;
        for (GuestEntity vm : readSpace.getVmList()) {
            if (readSpace.getId(vm) == readSpace.getId(owner)) {
                continue;
            }
            if (readSpace.isVmMigrating(vm) || readSpace.isVmBeingInstantiated(vm)) {
                continue;
            }
            double candidateFinish = readSpace.getCloudletEstimatedFinishTime(vm, worstCloudlet);
            if (candidateFinish < bestFinish) {
                bestFinish = candidateFinish;
                bestVm = vm;
            }
        }

        if (bestVm == null) {
            Log.printlnConcat(readSpace.getNow(), ": [" + MODULE_NAME + "] no vm improves estimated finish time for cloudlet " + readSpace.getId(worstCloudlet));
            return new int[0];
        }

        int cloudletId = readSpace.getId(worstCloudlet);
        int fromVmId = readSpace.getId(owner);
        int toVmId = readSpace.getId(bestVm);
        Log.printlnConcat(readSpace.getNow(), ": [" + MODULE_NAME + "] moving cloudlet " + cloudletId + " from vm " + fromVmId + " to vm " + toVmId + " for better finish time");
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
        return "cloudlet-estimated-finish-time-risk";
    }

    @Override
    public String outputSemantic() {
        return "move-cloudlet-deadline-optimal";
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
