package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.core.GuestEntity;
import java.util.List;

// Strategy: Sunk-work-aware deadline rescue.
// Among cloudlets flagged OVERLOADED (i.e. at risk on their current VM), this
// planner prioritises the one with the LEAST remaining length: it is closest to
// completion, so it has the most already-invested work to protect and the least
// distance left to cover elsewhere. It is moved to whichever other VM currently
// carries the fewest queued cloudlets, treating queue depth (not instantaneous
// utilisation) as the signal for real spare service capacity.
public class planner_v9 implements Planner<LoadState[], int[]> {

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {

        Log.printlnConcat(readSpace.getNow(), ": [planner_v9] ", "scanning at-risk cloudlets for deadline rescue");

        List<Cloudlet> cloudlets = readSpace.getActiveCloudlets();
        List<GuestEntity> vms = readSpace.getVmList();

        Cloudlet rescueTarget = null;
        long leastRemaining = Long.MAX_VALUE;

        int limit = Math.min(diagnosis.length, cloudlets.size());
        for (int i = 0; i < limit; i++) {

            if (diagnosis[i] != LoadState.OVERLOADED) {
                continue;
            }

            Cloudlet cl = cloudlets.get(i);
            long remaining = readSpace.getRemainingLength(cl);
            if (remaining < leastRemaining) {
                leastRemaining = remaining;
                rescueTarget = cl;
            }
        }

        if (rescueTarget == null) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v9] ", "no at-risk cloudlets found this cycle");
            return null;
        }

        GuestEntity fromVm = findOwningVm(readSpace, vms, rescueTarget);
        if (fromVm == null) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v9] ", "could not resolve source VM for rescue cloudlet");
            return null;
        }

        GuestEntity toVm = null;
        int fewestQueued = Integer.MAX_VALUE;

        for (GuestEntity vm : vms) {
            if (vm == fromVm) {
                continue;
            }
            if (readSpace.isVmMigrating(vm) || readSpace.isVmBeingInstantiated(vm)) {
                continue;
            }
            int queueDepth = readSpace.getVmCloudletList(vm).size();
            if (queueDepth < fewestQueued) {
                fewestQueued = queueDepth;
                toVm = vm;
            }
        }

        if (toVm == null) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v9] ", "no alternate VM available to receive rescue cloudlet");
            return null;
        }

        int cloudletId = readSpace.getId(rescueTarget);
        int fromVmId = readSpace.getId(fromVm);
        int toVmId = readSpace.getId(toVm);

        Log.printlnConcat(readSpace.getNow(), ": [planner_v9] ", "rescuing cloudletId=" + cloudletId + " from vmId=" + fromVmId + " to vmId=" + toVmId);

        return new int[] { cloudletId, fromVmId, toVmId };
    }

    private GuestEntity findOwningVm(ReadSpace readSpace, List<GuestEntity> vms, Cloudlet cl) {
        for (GuestEntity vm : vms) {
            if (readSpace.getVmCloudletList(vm).contains(cl)) {
                return vm;
            }
        }
        return null;
    }

    @Override
    public String inputSemantic() {
        return "cloudlet-completionrisk-loadstate";
    }

    @Override
    public String outputSemantic() {
        return "cloudlet-move-deadlinerescue";
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
