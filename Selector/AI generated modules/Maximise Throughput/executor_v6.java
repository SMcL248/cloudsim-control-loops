package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;

// GUID 3006 -- requestRamScaling
// Strategy: tier-resolving scaler with no-op suppression, mirroring the MIPS
// scaler's discipline but for RAM. RAM headroom governs how many/which VMs a
// host can accept, so keeping RAM appropriately sized is an indirect but
// real lever on achievable placement density and therefore throughput.
public class executor_v6 implements Executor<int[]> {

    private static final int GUID = 3006;
    private int successCount = 0;

    @Override
    public boolean execute(int[] action, ActionSpace actionSpace) {
        if (action == null || action.length != 2) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v6] Malformed payload for requestRamScaling, expected 2 ints, aborting.");
            return false;
        }
        if (isSentinel(action)) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v6] No prescribed action this cycle.");
            return false;
        }

        int vmId = action[0];
        int tierIndex = action[1];

        GuestEntity vm = actionSpace.getVmById(vmId);
        if (vm == null) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v6] Unknown VM reference " + vmId + ", aborting RAM scaling.");
            return false;
        }

        int[] ramTiers = actionSpace.getRamTiers();
        if (tierIndex < 0 || ramTiers == null || tierIndex >= ramTiers.length) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v6] Tier index " + tierIndex + " out of range for VM " + vmId + ", aborting.");
            return false;
        }

        double newRam = ramTiers[tierIndex];
        if (newRam == actionSpace.getVmRam(vm)) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v6] VM " + vmId + " already at requested RAM tier, skipping no-op scaling.");
            return false;
        }

        boolean success = actionSpace.requestRamScaling(vm, newRam);
        if (success) {
            successCount++;
        }
        Log.printlnConcat(actionSpace.getNow(), ": [executor_v6] Requested RAM scaling of VM " + vmId + " to " + newRam + " (tier " + tierIndex + "), success=" + success);
        return true;
    }

    private boolean isSentinel(int[] a) {
        for (int v : a) {
            if (v != -1) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String inputSemantic() {
        return "Scale a VM's RAM to a specified tier to relieve memory-bound placement and scheduling constraints";
    }

    @Override
    public int inputGuid() {
        return GUID;
    }

    @Override
    public int getSuccessfulActionCount() {
        return successCount;
    }
}
