package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;

import java.util.List;

// Strategy: throughput-led elasticity. Diagnosis is per-VM. Only scales the fleet out when overload
// is the dominant state cluster-wide (a single hot VM is better handled by migration/scaling than by
// growing the fleet), sizing the new VM to the severity of the overload.
public class planner_v3 implements Planner<LoadState[], int[]> {

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {

        List<GuestEntity> vms = readSpace.getVmList();

        if (diagnosis == null || vms == null || diagnosis.length != vms.size() || vms.isEmpty()) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v3] diagnosis/VM list mismatch or no VMs, no-op");
            return new int[0];
        }

        int overloadedCount = 0;
        for (LoadState s : diagnosis) {
            if (s == LoadState.OVERLOADED) overloadedCount++;
        }
        double overloadFraction = (double) overloadedCount / diagnosis.length;

        if (overloadFraction <= 0.5) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v3] overload fraction ", overloadFraction, " below scale-out threshold, no-op");
            return new int[0];
        }

        int[] mipsTiers = readSpace.getMipsTiers();
        int tierCount = mipsTiers.length;

        // Size the new VM to the severity of the overload: near-saturation calls for the largest tier.
        int tierIndex;
        if (overloadFraction > 0.75) {
            tierIndex = tierCount - 1;
        } else {
            tierIndex = tierCount / 2;
        }
        int sizeTierIndex = tierIndex;

        int datacenterId = readSpace.getDatacenterFor(readSpace.getId(vms.get(0)));

        Log.printlnConcat(readSpace.getNow(), ": [planner_v3] overload fraction ", overloadFraction, " -- requesting new VM at tier ", tierIndex, " in datacenter ", datacenterId);

        return new int[] { tierIndex, sizeTierIndex, datacenterId };
    }

    @Override
    public String inputSemantic() {
        return "vm-cpu-load-state";
    }

    @Override
    public String outputSemantic() {
        return "requestVmCreation";
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
