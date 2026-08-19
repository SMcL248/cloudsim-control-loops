package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import java.util.List;

public class planner_v13 implements Planner<LoadState[], int[]> {

    private static final double OVERLOAD_TRIGGER_FRACTION = 0.5;

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {
        List<GuestEntity> vms = readSpace.getVmList();
        int limit = Math.min(diagnosis.length, vms.size());

        int overloadedCount = 0;
        GuestEntity referenceVm = null;
        for (int i = 0; i < limit; i++) {
            if (diagnosis[i] == LoadState.OVERLOADED) {
                overloadedCount++;
                if (referenceVm == null) {
                    referenceVm = vms.get(i);
                }
            }
        }

        if (limit == 0 || (double) overloadedCount / limit < OVERLOAD_TRIGGER_FRACTION || referenceVm == null) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v13] fleet overload fraction below creation threshold");
            return new int[0];
        }

        Integer datacenterId = readSpace.getDatacenterFor(readSpace.getId(referenceVm));
        if (datacenterId == null) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v13] could not resolve datacenter for reference VM, aborting");
            return new int[0];
        }

        int[] mipsTiers = readSpace.getMipsTiers();
        int[] ramTiers = readSpace.getRamTiers();
        int tierIndex = mipsTiers.length / 2;
        int sizeTierIndex = ramTiers.length / 2;

        Log.printlnConcat(readSpace.getNow(), ": [planner_v13] fleet overload fraction=", (double) overloadedCount / limit, ", requesting new mid-tier VM in datacenter ", datacenterId);
        return new int[] { tierIndex, sizeTierIndex, datacenterId };
    }

    @Override
    public String inputSemantic() {
        return "vm-loadstate-fleet-saturation";
    }

    @Override
    public String outputSemantic() {
        return "vm-creation";
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
