package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;

import java.util.List;

// Strategy: pure throughput, no power regard. Diagnosis is per-VM. Grants an extra PE to whichever
// overloaded VM is under the most CPU pressure, on the assumption that more cores is the fastest lever
// for relieving a CPU-bound bottleneck, regardless of the resulting power cost.
public class planner_v7 implements Planner<LoadState[], int[]> {

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {

        List<GuestEntity> vms = readSpace.getVmList();

        if (diagnosis == null || vms == null || diagnosis.length != vms.size()) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v7] diagnosis/VM list mismatch, no-op");
            return new int[0];
        }

        int targetIndex = -1;
        double worstUtil = -1;
        for (int i = 0; i < diagnosis.length; i++) {
            if (diagnosis[i] == LoadState.OVERLOADED) {
                double util = readSpace.getVmCpuUtil(vms.get(i));
                if (util > worstUtil) {
                    worstUtil = util;
                    targetIndex = i;
                }
            }
        }

        if (targetIndex == -1) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v7] no overloaded VM found, no-op");
            return new int[0];
        }

        int vmId = readSpace.getId(vms.get(targetIndex));

        Log.printlnConcat(readSpace.getNow(), ": [planner_v7] allocating extra PE to overloaded VM ", vmId);

        return new int[] { vmId };
    }

    @Override
    public String inputSemantic() {
        return "vm-cpu-load-state";
    }

    @Override
    public String outputSemantic() {
        return "requestPeAllocation";
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
