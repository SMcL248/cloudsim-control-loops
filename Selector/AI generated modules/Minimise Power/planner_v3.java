package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import java.util.List;

/**
 * Variant 3 - Over-Provisioning Trim.
 *
 * Strategy: targets VMs whose *provisioned* MIPS greatly exceeds what they
 * are *actually* pushing through (requested MIPS vs effective throughput).
 * That gap is wasted provisioned capacity that still costs power to host,
 * independent of whether the VM looks "busy" moment to moment. Among
 * UNDERLOADED VMs, the one with the single largest gap is stepped down by
 * exactly one MIPS tier, trimming waste while leaving margin for the VM's
 * observed throughput.
 */
public class planner_v3 implements Planner<LoadState[], int[]> {

    private static final int INPUT_GUID = 2300;
    private static final int OUTPUT_GUID = 3005;

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {
        List<GuestEntity> vms = readSpace.getVmList();
        int[] mipsTiers = readSpace.getMipsTiers();
        int limit = Math.min(diagnosis.length, vms.size());

        GuestEntity worstOffender = null;
        double largestGap = 0.0;

        for (int i = 0; i < limit; i++) {
            if (diagnosis[i] != LoadState.UNDERLOADED) {
                continue;
            }
            GuestEntity vm = vms.get(i);
            if (readSpace.isVmMigrating(vm) || readSpace.isVmBeingInstantiated(vm)) {
                continue;
            }
            double gap = readSpace.getVmRequestedMips(vm) - readSpace.getVmEffectiveThroughput(vm);
            if (gap > largestGap) {
                largestGap = gap;
                worstOffender = vm;
            }
        }

        int[] noOp = new int[]{-1, -1};
        if (worstOffender == null) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v3] no significantly over-provisioned vm found, emitting no-op");
            return noOp;
        }

        int currentIdx = currentTierIndex(mipsTiers, readSpace.getVmMips(worstOffender));
        if (currentIdx <= 0) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v3] over-provisioned vm ", readSpace.getId(worstOffender),
                    " already at lowest mips tier, emitting no-op");
            return noOp;
        }

        int vmId = readSpace.getId(worstOffender);
        int newTierIndex = currentIdx - 1;
        Log.printlnConcat(readSpace.getNow(), ": [planner_v3] trimming vm ", vmId,
                " down to mips tier ", newTierIndex, " to shed provisioning waste");
        return new int[]{vmId, newTierIndex};
    }

    private int currentTierIndex(int[] tiers, double value) {
        int rounded = (int) Math.round(value);
        for (int i = 0; i < tiers.length; i++) {
            if (tiers[i] == rounded) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public String inputSemantic() {
        return "vm-provisioning-waste-loadstate";
    }

    @Override
    public String outputSemantic() {
        return "requestMipsScaling";
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
