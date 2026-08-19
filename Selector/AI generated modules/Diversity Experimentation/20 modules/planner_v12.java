package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.core.GuestEntity;
import java.util.List;

public class planner_v12 implements Planner<LoadState[], int[]> {

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {
        List<Cloudlet> activeCloudlets = readSpace.getActiveCloudlets();
        int limit = Math.min(diagnosis.length, activeCloudlets.size());

        for (int i = 0; i < limit; i++) {
            if (diagnosis[i] != LoadState.OVERLOADED) {
                continue;
            }
            Cloudlet cloudlet = activeCloudlets.get(i);
            GuestEntity fromVm = findOwningVm(cloudlet, readSpace);
            if (fromVm == null) {
                continue;
            }

            GuestEntity toVm = findLeastUtilisedVm(readSpace, fromVm);
            if (toVm == null) {
                continue;
            }

            int cloudletId = readSpace.getId(cloudlet);
            int fromVmId = readSpace.getId(fromVm);
            int toVmId = readSpace.getId(toVm);
            Log.printlnConcat(readSpace.getNow(), ": [planner_v12] moving cloudlet ", cloudletId, " from VM ", fromVmId, " to least-utilised VM ", toVmId);
            return new int[] { cloudletId, fromVmId, toVmId };
        }

        Log.printlnConcat(readSpace.getNow(), ": [planner_v12] no relocatable overloaded cloudlet found");
        return new int[0];
    }

    private GuestEntity findOwningVm(Cloudlet cloudlet, ReadSpace readSpace) {
        int cloudletId = readSpace.getId(cloudlet);
        for (GuestEntity vm : readSpace.getVmList()) {
            for (Cloudlet cl : readSpace.getVmCloudletList(vm)) {
                if (readSpace.getId(cl) == cloudletId) {
                    return vm;
                }
            }
        }
        return null;
    }

    private GuestEntity findLeastUtilisedVm(ReadSpace readSpace, GuestEntity exclude) {
        GuestEntity best = null;
        double lowestUtil = Double.MAX_VALUE;
        for (GuestEntity vm : readSpace.getVmList()) {
            if (vm == exclude || readSpace.isVmMigrating(vm) || readSpace.isVmBeingInstantiated(vm)) {
                continue;
            }
            double util = readSpace.getVmCpuUtil(vm);
            if (util < lowestUtil) {
                lowestUtil = util;
                best = vm;
            }
        }
        return best;
    }

    @Override
    public String inputSemantic() {
        return "cloudlet-loadstate-host-vm-congestion";
    }

    @Override
    public String outputSemantic() {
        return "cloudlet-move";
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
