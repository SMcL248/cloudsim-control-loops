package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import java.util.List;

// Strategy: Population-level fleet expansion.
// Unlike the other planners in this set, this one never inspects a single host,
// VM or cloudlet in isolation. It looks at the OVERLOADED proportion across the
// entire host-level diagnosis array and treats that aggregate ratio as a single
// system-wide signal: if a majority of hosts are overloaded, existing capacity is
// judged structurally insufficient and a new VM is requested rather than shuffling
// load among already-strained hosts. The severity of the ratio scales which
// compute tier is requested.
public class planner_v10 implements Planner<LoadState[], int[]> {

    private static final double MAJORITY_THRESHOLD = 0.5;
    private static final double SEVERE_THRESHOLD = 0.8;

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {

        Log.printlnConcat(readSpace.getNow(), ": [planner_v10] ", "assessing aggregate host overload ratio");

        if (diagnosis.length == 0) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v10] ", "empty diagnosis, no fleet decision made");
            return null;
        }

        int overloadedCount = 0;
        for (int i = 0; i < diagnosis.length; i++) {
            if (diagnosis[i] == LoadState.OVERLOADED) {
                overloadedCount++;
            }
        }

        double ratio = ((double) overloadedCount) / diagnosis.length;

        if (ratio <= MAJORITY_THRESHOLD) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v10] ", "overload ratio below majority threshold, no expansion");
            return null;
        }

        List<GuestEntity> existingVms = readSpace.getVmList();
        if (existingVms.isEmpty()) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v10] ", "no existing VM to resolve target datacenter from");
            return null;
        }

        GuestEntity referenceVm = existingVms.get(0);
        int datacenterId = readSpace.getDatacenterFor(readSpace.getId(referenceVm));

        int[] mipsTiers = readSpace.getMipsTiers();
        int severityIndex;
        if (ratio >= SEVERE_THRESHOLD) {
            severityIndex = mipsTiers.length - 1;
        } else {
            severityIndex = mipsTiers.length / 2;
        }

        // No dedicated storage-size-tier accessor is exposed on ReadSpace, so the
        // same severity-mapped index is reused as a coarse proxy for VM size class.
        int tierIndex = severityIndex;
        int sizeTierIndex = severityIndex;

        Log.printlnConcat(readSpace.getNow(), ": [planner_v10] ", "majority overload ratio=" + ratio + " requesting new VM tierIndex=" + tierIndex);

        return new int[] { tierIndex, sizeTierIndex, datacenterId };
    }

    @Override
    public String inputSemantic() {
        return "host-aggregateutil-loadstate";
    }

    @Override
    public String outputSemantic() {
        return "vm-creation-fleet-expansion";
    }

    @Override
    public int inputGuid() {
        return 2200;
    }

    @Override
    public int outputGuid() {
        return 3003;
    }
}
