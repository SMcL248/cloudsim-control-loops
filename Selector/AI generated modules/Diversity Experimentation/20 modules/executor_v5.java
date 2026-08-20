package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;

// Defensive Tier-Clamping Executor for requestVmCreation (3003).
// Clamps out-of-range tier indices into valid bounds rather than rejecting
// the instruction outright, favouring a best-effort creation attempt over
// strict refusal. Size tiers are the domain's fixed small/medium/large set.
public class executor_v5 implements Executor<int[]> {

    private static final int SIZE_TIER_COUNT = 3; // small, medium, large

    @Override
    public boolean execute(int[] actions, ActionSpace actionSpace) {
        if (actions == null || actions.length < 3) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v5] ", "Malformed payload, expected {tierIndex, sizeTierIndex, datacenterId}");
            return false;
        }

        int tierIndex = actions[0];
        int sizeTierIndex = actions[1];
        int datacenterId = actions[2];

        int[] mipsTiers = actionSpace.getMipsTiers();
        int clampedTierIndex = clamp(tierIndex, 0, mipsTiers.length - 1);
        int clampedSizeIndex = clamp(sizeTierIndex, 0, SIZE_TIER_COUNT - 1);

        if (clampedTierIndex != tierIndex || clampedSizeIndex != sizeTierIndex) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v5] ", "Clamped out-of-range tiers, requested(" + tierIndex + "," + sizeTierIndex + ") -> used(" + clampedTierIndex + "," + clampedSizeIndex + ")");
        }

        GuestEntity created = actionSpace.requestVmCreation(clampedTierIndex, clampedSizeIndex, datacenterId);
        if (created != null) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v5] ", "Created vm=" + actionSpace.getId(created) + " in datacenter=" + datacenterId);
        } else {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v5] ", "Creation request returned null for datacenter=" + datacenterId + ", likely invalid datacenter id");
        }
        return true;
    }

    private int clamp(int value, int min, int max) {
        if (max < min) {
            return min;
        }
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }

    @Override
    public String inputSemantic() {
        return "requestVmCreation: create a new VM at a given tier and size";
    }

    @Override
    public int inputGuid() {
        return 3003;
    }
}
