package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.core.GuestEntity;
import java.util.List;

/**
 * Variant 10 - Wind-Down Before Completion.
 *
 * Strategy: distinct from the static waste-trimming of variants 3 and 8,
 * this variant is triggered by imminence, not by a persistent gap. It looks
 * for a cloudlet diagnosed UNDERLOADED that is the *sole* remaining active
 * cloudlet on its VM (nothing queued behind it) and whose estimated finish
 * time is close at hand. In that narrow window the VM's remaining bandwidth
 * needs are minimal, so its BW tier is wound down to the lowest tier right
 * before the VM goes idle - squeezing out the last bit of avoidable
 * provisioning during the tail of the VM's life rather than reacting to a
 * long-lived over-provisioning pattern.
 */
public class planner_v10 implements Planner<LoadState[], int[]> {

    private static final int INPUT_GUID = 2400;
    private static final int OUTPUT_GUID = 3007;

    private static final double WIND_DOWN_WINDOW = 10.0;

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {
        List<Cloudlet> activeCloudlets = readSpace.getActiveCloudlets();
        List<GuestEntity> vms = readSpace.getVmList();
        double now = readSpace.getNow();
        int limit = Math.min(diagnosis.length, activeCloudlets.size());

        GuestEntity chosenVm = null;
        double soonestRemaining = Double.MAX_VALUE;

        for (int i = 0; i < limit; i++) {
            if (diagnosis[i] != LoadState.UNDERLOADED) {
                continue;
            }
            Cloudlet cloudlet = activeCloudlets.get(i);
            GuestEntity owner = findOwningVm(cloudlet, vms, readSpace);
            if (owner == null) {
                continue;
            }
            if (readSpace.isVmMigrating(owner) || readSpace.isVmBeingInstantiated(owner)) {
                continue;
            }
            List<Cloudlet> owned = readSpace.getVmCloudletList(owner);
            if (owned.size() != 1) {
                continue;
            }
            double remaining = readSpace.getCloudletEstimatedFinishTime(owner, cloudlet) - now;
            if (remaining < 0 || remaining > WIND_DOWN_WINDOW) {
                continue;
            }
            if (remaining < soonestRemaining) {
                soonestRemaining = remaining;
                chosenVm = owner;
            }
        }

        int[] noOp = new int[]{-1, -1};
        if (chosenVm == null) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v10] no vm nearing sole-cloudlet completion found, emitting no-op");
            return noOp;
        }

        int[] bwTiers = readSpace.getBwTiers();
        int currentIdx = currentTierIndex(bwTiers, readSpace.getVmBw(chosenVm));
        if (currentIdx <= 0) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v10] winding-down vm already at lowest bw tier, emitting no-op");
            return noOp;
        }

        int vmId = readSpace.getId(chosenVm);
        Log.printlnConcat(readSpace.getNow(), ": [planner_v10] winding down bw of vm ", vmId,
                " to lowest tier ahead of imminent completion");
        return new int[]{vmId, 0};
    }

    private GuestEntity findOwningVm(Cloudlet cloudlet, List<GuestEntity> vms, ReadSpace readSpace) {
        int cloudletId = readSpace.getId(cloudlet);
        for (GuestEntity vm : vms) {
            for (Cloudlet owned : readSpace.getVmCloudletList(vm)) {
                if (readSpace.getId(owned) == cloudletId) {
                    return vm;
                }
            }
        }
        return null;
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
        return "cloudlet-completion-imminence-loadstate";
    }

    @Override
    public String outputSemantic() {
        return "requestBwScaling";
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
