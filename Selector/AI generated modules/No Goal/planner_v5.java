package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import java.util.List;

/**
 * Variant 5: Aggregate-demand elastic scale-out.
 * Strategy: ignore individual VMs entirely and instead read the VM-level
 * diagnosis as a fleet-wide demand signal. Once the fraction of OVERLOADED
 * VMs crosses a threshold, request a brand-new VM, sizing it up further if
 * the overload ratio is severe. This is a horizontal, population-level
 * response rather than a per-guest remediation.
 */
public class planner_v5 implements Planner<LoadState[], int[]> {

    private static final double SCALE_OUT_THRESHOLD = 0.5;
    private static final double SEVERE_THRESHOLD = 0.75;

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {
        List<GuestEntity> vms = readSpace.getVmList();

        if (diagnosis.length == 0 || vms.isEmpty()) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v5] ",
                    "No diagnosis data or no reference VM for datacenter lookup, skipping.");
            return new int[]{-1, -1, -1};
        }

        int considered = Math.min(diagnosis.length, vms.size());
        int overloadedCount = 0;
        for (int i = 0; i < considered; i++) {
            if (diagnosis[i] == LoadState.OVERLOADED) {
                overloadedCount++;
            }
        }
        double ratio = (double) overloadedCount / considered;

        if (ratio <= SCALE_OUT_THRESHOLD) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v5] ",
                    "Fleet overload ratio " + ratio + " below elastic scale-out threshold.");
            return new int[]{-1, -1, -1};
        }

        int severityTier = (ratio >= SEVERE_THRESHOLD) ? 2 : 1;
        int datacenterId = readSpace.getDatacenterFor(readSpace.getId(vms.get(0)));

        Log.printlnConcat(readSpace.getNow(), ": [planner_v5] ",
                "Fleet-wide overload ratio " + ratio + " exceeds threshold, requesting new VM at severity tier " + severityTier);
        return new int[]{severityTier, severityTier, datacenterId};
    }

    @Override
    public String inputSemantic() {
        return "vm-loadstate-overload-aggregate-demand";
    }

    @Override
    public String outputSemantic() {
        return "vm-creation-scaleout";
    }

    @Override
    public int inputGuid() {
        return 2300;
    }

    @Override
    public int outputGuid() {
        return 3003;
    }
}
