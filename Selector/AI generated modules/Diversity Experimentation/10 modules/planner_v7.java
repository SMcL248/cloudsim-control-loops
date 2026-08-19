package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.core.GuestEntity;
import java.util.List;

// Strategy: Backlog-signalled, single-step bandwidth elasticity.
// Rather than reacting to raw utilisation, this planner infers network-bound
// pressure indirectly: a VM whose queued cloudlet count exceeds its own PE count
// is carrying more concurrent work items than it can compute in parallel, which
// is treated as a proxy for a bandwidth/IO bottleneck rather than a compute one.
// Only a single BW tier step is requested at a time, never jumping further ahead.
public class planner_v7 implements Planner<LoadState[], int[]> {

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {

        Log.printlnConcat(readSpace.getNow(), ": [planner_v7] ", "checking VMs for backlog-signalled bandwidth pressure");

        List<GuestEntity> vms = readSpace.getVmList();
        int[] bwTiers = readSpace.getBwTiers();

        int limit = Math.min(diagnosis.length, vms.size());
        for (int i = 0; i < limit; i++) {

            if (diagnosis[i] != LoadState.OVERLOADED) {
                continue;
            }

            GuestEntity vm = vms.get(i);

            List<Cloudlet> queued = readSpace.getVmCloudletList(vm);
            int peCount = readSpace.getVmNumberOfPes(vm);

            boolean networkBoundSignal = queued.size() > peCount;
            if (!networkBoundSignal) {
                continue;
            }

            double nextBwTierValue = readSpace.getNextBwTier(vm);
            if (nextBwTierValue < 0) {
                continue;
            }

            int tierIndex = indexOfTier(bwTiers, nextBwTierValue);
            if (tierIndex < 0) {
                continue;
            }

            int vmId = readSpace.getId(vm);
            Log.printlnConcat(readSpace.getNow(), ": [planner_v7] ", "backlog signal confirmed, scaling BW for vmId=" + vmId + " to tier index=" + tierIndex);
            return new int[] { vmId, tierIndex };
        }

        Log.printlnConcat(readSpace.getNow(), ": [planner_v7] ", "no VM showed a backlog-based bandwidth signal this cycle");
        return null;
    }

    private int indexOfTier(int[] tiers, double value) {
        for (int i = 0; i < tiers.length; i++) {
            if (tiers[i] == value) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public String inputSemantic() {
        return "vm-bwpressure-loadstate";
    }

    @Override
    public String outputSemantic() {
        return "vm-bw-scaleup-incremental";
    }

    @Override
    public int inputGuid() {
        return 2300;
    }

    @Override
    public int outputGuid() {
        return 3007;
    }
}
