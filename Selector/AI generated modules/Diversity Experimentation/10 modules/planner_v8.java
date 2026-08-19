package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.core.GuestEntity;
import java.util.List;

// Strategy: Parallelism-widening core elasticity.
// Distinguishes VMs that are overloaded because their workload is wide (many
// small, independently schedulable cloudlets queued) from VMs that are overloaded
// because individual cloudlets are simply long-running. Only the former benefits
// from an additional PE, since more cores let more queued cloudlets run
// concurrently; a VM with few but heavy cloudlets is left for a raw-speed remedy
// implemented elsewhere in the module set.
public class planner_v8 implements Planner<LoadState[], int[]> {

    private static final int BACKLOG_MULTIPLE = 2;

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {

        Log.printlnConcat(readSpace.getNow(), ": [planner_v8] ", "checking overloaded VMs for wide parallel backlog");

        List<GuestEntity> vms = readSpace.getVmList();

        int limit = Math.min(diagnosis.length, vms.size());
        for (int i = 0; i < limit; i++) {

            if (diagnosis[i] != LoadState.OVERLOADED) {
                continue;
            }

            GuestEntity vm = vms.get(i);

            List<Cloudlet> queued = readSpace.getVmCloudletList(vm);
            int peCount = readSpace.getVmNumberOfPes(vm);

            boolean wideBacklog = queued.size() >= peCount * BACKLOG_MULTIPLE;
            if (!wideBacklog) {
                continue;
            }

            int vmId = readSpace.getId(vm);
            Log.printlnConcat(readSpace.getNow(), ": [planner_v8] ", "wide backlog detected, allocating PE for vmId=" + vmId);
            return new int[] { vmId };
        }

        Log.printlnConcat(readSpace.getNow(), ": [planner_v8] ", "no VM showed a wide enough backlog to justify a new PE");
        return null;
    }

    @Override
    public String inputSemantic() {
        return "vm-cpuutil-loadstate";
    }

    @Override
    public String outputSemantic() {
        return "vm-pe-allocate-backlog";
    }

    @Override
    public int inputGuid() {
        return 2300;
    }

    @Override
    public int outputGuid() {
        return 3008;
    }
}
