package org.cloudbus.cloudsim.examples;// always include

// Import whats needed
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.core.GuestEntity;
import java.util.List;

// Strategy: fine-grained cloudlet-level load shedding.
// Operates below the VM/host granularity entirely: picks the flagged OVERLOADED cloudlet with
// the most remaining work (most to gain from moving), resolves which VM currently owns it, and
// relocates it directly onto the globally least-utilised VM. No host-level reasoning at all.
public class planner_v4 implements Planner<LoadState[], int[]> {

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {
        List<Cloudlet> cloudlets = readSpace.getActiveCloudlets();

        Cloudlet flagged = null;
        long biggestRemaining = -1L;

        // Among cloudlets whose (host/VM-inherited) load state is OVERLOADED, shed the one
        // with the most remaining work, since it has the most to gain from relocation.
        for (int i = 0; i < diagnosis.length && i < cloudlets.size(); i++) {
            if (diagnosis[i] != LoadState.OVERLOADED) continue;
            Cloudlet cl = cloudlets.get(i);
            long remaining = readSpace.getRemainingLength(cl);
            if (remaining > biggestRemaining) {
                biggestRemaining = remaining;
                flagged = cl;
            }
        }

        if (flagged == null) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v4] ", "no overloaded cloudlet found, no action");
            return new int[0];
        }

        List<GuestEntity> vms = readSpace.getVmList();

        GuestEntity sourceVm = null;
        for (GuestEntity vm : vms) {
            for (Cloudlet owned : readSpace.getVmCloudletList(vm)) {
                if (readSpace.getId(owned) == readSpace.getId(flagged)) {
                    sourceVm = vm;
                    break;
                }
            }
            if (sourceVm != null) break;
        }

        if (sourceVm == null) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v4] ", "could not resolve owning vm for flagged cloudlet, no action");
            return new int[0];
        }

        GuestEntity targetVm = null;
        double lowestUtil = Double.MAX_VALUE;
        for (GuestEntity vm : vms) {
            if (readSpace.getId(vm) == readSpace.getId(sourceVm)) continue;
            if (readSpace.isVmMigrating(vm) || readSpace.isVmBeingInstantiated(vm)) continue;
            double util = readSpace.getVmCpuUtil(vm);
            if (util < lowestUtil) {
                lowestUtil = util;
                targetVm = vm;
            }
        }

        if (targetVm == null) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v4] ", "no eligible destination vm found, no action");
            return new int[0];
        }

        Log.printlnConcat(readSpace.getNow(), ": [planner_v4] planning move of cloudlet ", readSpace.getId(flagged), " from vm ", readSpace.getId(sourceVm), " to vm ", readSpace.getId(targetVm));

        return new int[] { readSpace.getId(flagged), readSpace.getId(sourceVm), readSpace.getId(targetVm) };
    }

    @Override
    public String inputSemantic() {
        return "cloudlet-loadstate-hostvm-inherited";
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
