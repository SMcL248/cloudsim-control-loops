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

// Straggler Cloudlet Rebalancing Planner.
// Reads a cloudlet-level LoadState[] (index i corresponds to
// readSpace.getActiveCloudlets().get(i)) and finds the first cloudlet
// flagged OVERLOADED, i.e. lagging relative to its host VM. Resolves the
// cloudlet's current VM by scanning per-VM cloudlet lists, then moves the
// cloudlet to whichever other VM currently has the lowest CPU utilisation.
public class planner_v10 implements Planner<LoadState[], int[]> {

    private static final int INPUT_GUID = 2400;
    private static final int OUTPUT_GUID = 3001;

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {
        List<Cloudlet> cloudlets = readSpace.getActiveCloudlets();
        int limit = Math.min(diagnosis.length, cloudlets.size());

        Cloudlet straggler = null;
        for (int i = 0; i < limit; i++) {
            if (diagnosis[i] == LoadState.OVERLOADED) {
                straggler = cloudlets.get(i);
                break;
            }
        }

        if (straggler == null) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v10] no overloaded cloudlet found, no rebalancing needed");
            return new int[0];
        }

        int cloudletId = readSpace.getId(straggler);
        List<GuestEntity> vms = readSpace.getVmList();

        GuestEntity sourceVm = null;
        for (GuestEntity vm : vms) {
            List<Cloudlet> assigned = readSpace.getVmCloudletList(vm);
            for (Cloudlet cl : assigned) {
                if (readSpace.getId(cl) == cloudletId) {
                    sourceVm = vm;
                    break;
                }
            }
            if (sourceVm != null) {
                break;
            }
        }

        if (sourceVm == null) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v10] could not resolve source vm for cloudlet ", cloudletId);
            return new int[0];
        }

        GuestEntity destVm = null;
        double lowestUtil = Double.MAX_VALUE;
        for (GuestEntity vm : vms) {
            if (readSpace.getId(vm) == readSpace.getId(sourceVm)) {
                continue;
            }
            if (readSpace.isVmMigrating(vm) || readSpace.isVmBeingInstantiated(vm)) {
                continue;
            }
            double util = readSpace.getVmCpuUtil(vm);
            if (util < lowestUtil) {
                lowestUtil = util;
                destVm = vm;
            }
        }

        if (destVm == null) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v10] no destination vm available for straggler cloudlet ", cloudletId);
            return new int[0];
        }

        int fromVmId = readSpace.getId(sourceVm);
        int toVmId = readSpace.getId(destVm);
        Log.printlnConcat(readSpace.getNow(), ": [planner_v10] moving straggler cloudlet ", cloudletId, " from vm ", fromVmId, " to vm ", toVmId);
        return new int[] { cloudletId, fromVmId, toVmId };
    }

    @Override
    public String inputSemantic() {
        return "cloudlet-loadstate-classification";
    }

    @Override
    public String outputSemantic() {
        return "moveCloudlet";
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
