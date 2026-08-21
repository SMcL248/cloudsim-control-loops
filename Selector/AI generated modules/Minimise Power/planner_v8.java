package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;
import java.util.List;

/**
 * Variant 8 - Fragmentation-Relief RAM Trim.
 *
 * Strategy: unlike variant 3 (which trims a VM's own over-provisioning), this
 * variant reasons at host granularity. A host diagnosed OVERLOADED is read as
 * "resource pressured" - it cannot cleanly accept incoming guests, which
 * blocks the kind of consolidation migrations that would otherwise let other
 * hosts power down. Rather than migrating VMs off it, this variant relieves
 * the pressure directly by stepping down the RAM tier of whichever resident
 * VM is consuming the most RAM, freeing room on that host for future
 * consolidation without needing to relocate any workload.
 */
public class planner_v8 implements Planner<LoadState[], int[]> {

    private static final int INPUT_GUID = 2200;
    private static final int OUTPUT_GUID = 3006;

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {
        List<HostEntity> hosts = readSpace.getAllHosts();
        int limit = Math.min(diagnosis.length, hosts.size());

        HostEntity pressuredHost = null;
        for (int i = 0; i < limit; i++) {
            if (diagnosis[i] != LoadState.OVERLOADED) {
                continue;
            }
            HostEntity host = hosts.get(i);
            if (readSpace.isHostFailed(host) || readSpace.isHostPoweredDown(host)
                    || readSpace.isHostPoweringUp(host)) {
                continue;
            }
            pressuredHost = host;
            break;
        }

        int[] noOp = new int[]{-1, -1};
        if (pressuredHost == null) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v8] no resource-pressured host found, emitting no-op");
            return noOp;
        }

        GuestEntity biggestConsumer = null;
        double biggestRam = -1.0;
        for (GuestEntity vm : readSpace.getVmListForHost(pressuredHost)) {
            if (readSpace.isVmMigrating(vm) || readSpace.isVmBeingInstantiated(vm)) {
                continue;
            }
            double ram = readSpace.getVmRam(vm);
            if (ram > biggestRam) {
                biggestRam = ram;
                biggestConsumer = vm;
            }
        }

        if (biggestConsumer == null) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v8] pressured host has no eligible resident vm, emitting no-op");
            return noOp;
        }

        int[] ramTiers = readSpace.getRamTiers();
        int currentIdx = currentTierIndex(ramTiers, biggestRam);
        if (currentIdx <= 0) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v8] biggest ram consumer already at lowest tier, emitting no-op");
            return noOp;
        }

        int vmId = readSpace.getId(biggestConsumer);
        int newTierIndex = currentIdx - 1;
        Log.printlnConcat(readSpace.getNow(), ": [planner_v8] trimming ram of vm ", vmId,
                " to tier ", newTierIndex, " to relieve fragmentation on pressured host");
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
        return "host-resource-pressure-loadstate";
    }

    @Override
    public String outputSemantic() {
        return "requestRamScaling";
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
