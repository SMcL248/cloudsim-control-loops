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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Strategy: Shortest-Queue Cloudlet Rebalancing.
// Diagnosis is cloudlet-level. Among cloudlets flagged OVERLOADED, picks the
// one with the largest remaining length (the biggest offender contributing
// to its VM's backlog), then reroutes it to whichever VM in the whole
// cluster currently carries the smallest aggregate remaining backlog. This
// is a shortest-queue routing heuristic aimed at flattening queue-length
// disparity across VMs so no single VM's backlog dominates the makespan.
public class planner_v2 implements Planner<LoadState[], int[]> {

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {

        List<Cloudlet> cloudlets = readSpace.getActiveCloudlets();
        List<GuestEntity> vms = readSpace.getVmList();

        Map<Integer, GuestEntity> ownerOf = new HashMap<Integer, GuestEntity>();
        for (GuestEntity vm : vms) {
            for (Cloudlet cl : readSpace.getVmCloudletList(vm)) {
                ownerOf.put(readSpace.getId(cl), vm);
            }
        }

        Cloudlet worstCloudlet = null;
        GuestEntity fromVm = null;
        long worstRemaining = -1;

        for (int i = 0; i < diagnosis.length && i < cloudlets.size(); i++) {
            if (diagnosis[i] != LoadState.OVERLOADED) {
                continue;
            }
            Cloudlet cl = cloudlets.get(i);
            GuestEntity owner = ownerOf.get(readSpace.getId(cl));
            if (owner == null) {
                continue;
            }
            long remaining = readSpace.getRemainingLength(cl);
            if (remaining > worstRemaining) {
                worstRemaining = remaining;
                worstCloudlet = cl;
                fromVm = owner;
            }
        }

        if (worstCloudlet == null) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v2] No overloaded cloudlet found, no move issued.");
            return new int[]{-1, -1, -1};
        }

        GuestEntity targetVm = null;
        double shortestBacklog = Double.MAX_VALUE;

        for (GuestEntity vm : vms) {
            if (readSpace.getId(vm) == readSpace.getId(fromVm)) {
                continue;
            }
            if (readSpace.isVmMigrating(vm) || readSpace.isVmBeingInstantiated(vm)) {
                continue;
            }
            double backlog = 0;
            for (Cloudlet cl : readSpace.getVmCloudletList(vm)) {
                backlog += readSpace.getRemainingLength(cl);
            }
            if (backlog < shortestBacklog) {
                shortestBacklog = backlog;
                targetVm = vm;
            }
        }

        if (targetVm == null) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v2] No eligible target VM found for cloudlet ", readSpace.getId(worstCloudlet), ".");
            return new int[]{-1, -1, -1};
        }

        Log.printlnConcat(readSpace.getNow(), ": [planner_v2] Shortest-queue reroute: cloudlet ", readSpace.getId(worstCloudlet), " from VM ", readSpace.getId(fromVm), " to VM ", readSpace.getId(targetVm), ".");

        return new int[]{readSpace.getId(worstCloudlet), readSpace.getId(fromVm), readSpace.getId(targetVm)};
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
        return 2400;
    }

    @Override
    public int outputGuid() {
        return 3001;
    }
}
