package org.cloudbus.cloudsim.examples;// always include

import java.util.List;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.core.GuestEntity;

// VM-level planner. diagnosis[i] is the load state of readSpace.getVmList().get(i).
// Goal: maximise throughput / minimise makespan.
// Strategy: find the OVERLOADED VM currently holding the cloudlet with the
// greatest remaining length (the biggest single contributor to queueing
// delay on a hot VM) and drain that cloudlet onto the first UNDERLOADED VM,
// putting idle capacity to work instead of leaving work stacked behind a
// congested VM.
public class planner_v1 implements Planner<LoadState[], int[]> {

    private static final String MODULE_NAME = "planner_v1";
    private static final int INPUT_GUID = 2300;
    private static final int OUTPUT_GUID = 3001;

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {
        double now = readSpace.getNow();
        List<GuestEntity> vms = readSpace.getVmList();

        if (diagnosis == null || diagnosis.length != vms.size()) {
            Log.printlnConcat(now, ": [" + MODULE_NAME + "] diagnosis/VM size mismatch, no-op");
            return new int[]{-1, -1, -1};
        }

        GuestEntity sourceVm = null;
        Cloudlet targetCloudlet = null;
        long longestRemaining = -1L;

        for (int i = 0; i < vms.size(); i++) {
            if (diagnosis[i] != LoadState.OVERLOADED) continue;
            GuestEntity vm = vms.get(i);
            List<Cloudlet> cloudlets = readSpace.getVmCloudletList(vm);
            for (Cloudlet cl : cloudlets) {
                long remaining = readSpace.getRemainingLength(cl);
                if (remaining > longestRemaining) {
                    longestRemaining = remaining;
                    targetCloudlet = cl;
                    sourceVm = vm;
                }
            }
        }

        if (sourceVm == null || targetCloudlet == null) {
            Log.printlnConcat(now, ": [" + MODULE_NAME + "] no overloaded VM with movable cloudlets, no-op");
            return new int[]{-1, -1, -1};
        }

        GuestEntity destVm = null;
        for (int i = 0; i < vms.size(); i++) {
            if (diagnosis[i] != LoadState.UNDERLOADED) continue;
            GuestEntity candidate = vms.get(i);
            if (readSpace.getId(candidate) == readSpace.getId(sourceVm)) continue;
            if (readSpace.isVmBeingInstantiated(candidate)) continue;
            destVm = candidate;
            break;
        }

        if (destVm == null) {
            Log.printlnConcat(now, ": [" + MODULE_NAME + "] no underloaded destination VM available, no-op");
            return new int[]{-1, -1, -1};
        }

        int cloudletId = readSpace.getId(targetCloudlet);
        int fromVmId = readSpace.getId(sourceVm);
        int toVmId = readSpace.getId(destVm);

        Log.printlnConcat(now, ": [" + MODULE_NAME + "] plan move cloudlet ", cloudletId,
                " from VM ", fromVmId, " to VM ", toVmId);
        return new int[]{cloudletId, fromVmId, toVmId};
    }

    @Override
    public String inputSemantic() {
        return "vm-mips-congestion-overload";
    }

    @Override
    public String outputSemantic() {
        return "movecloudlet";
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
