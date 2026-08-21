package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;
import java.util.List;

/**
 * Variant 9 - Spare-Capacity Exploitation.
 *
 * Strategy: a host that is already powered on is already paying its idle
 * power cost regardless of how busy it is, so pushing a little more work
 * through it is nearly free in power terms compared to powering up a whole
 * new host (contrast with variant 7). This variant looks for OVERLOADED VMs
 * whose *current* host still has a free PE and MIPS headroom, and grants
 * that VM an extra PE so its queued work finishes sooner - shortening
 * makespan using capacity that is already switched on, instead of adding
 * more powered infrastructure.
 */
public class planner_v9 implements Planner<LoadState[], int[]> {

    private static final int INPUT_GUID = 2300;
    private static final int OUTPUT_GUID = 3008;

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {
        List<GuestEntity> vms = readSpace.getVmList();
        List<HostEntity> hosts = readSpace.getAllHosts();
        int limit = Math.min(diagnosis.length, vms.size());

        GuestEntity chosenVm = null;
        double highestUtil = -1.0;

        for (int i = 0; i < limit; i++) {
            if (diagnosis[i] != LoadState.OVERLOADED) {
                continue;
            }
            GuestEntity vm = vms.get(i);
            if (readSpace.isVmMigrating(vm) || readSpace.isVmBeingInstantiated(vm)) {
                continue;
            }
            HostEntity host = findHostingHost(vm, hosts, readSpace);
            if (host == null) {
                continue;
            }
            if (readSpace.isHostFailed(host) || readSpace.isHostPoweredDown(host)
                    || readSpace.isHostPoweringUp(host)) {
                continue;
            }
            if (!readSpace.hostHasFreePe(host) || readSpace.getHostAvailableMips(host) <= 0) {
                continue;
            }
            double util = readSpace.getVmCpuUtil(vm);
            if (util > highestUtil) {
                highestUtil = util;
                chosenVm = vm;
            }
        }

        int[] noOp = new int[]{-1};
        if (chosenVm == null) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v9] no overloaded vm with spare host headroom found, emitting no-op");
            return noOp;
        }

        int vmId = readSpace.getId(chosenVm);
        Log.printlnConcat(readSpace.getNow(), ": [planner_v9] allocating spare pe on already-powered host to overloaded vm ", vmId);
        return new int[]{vmId};
    }

    private HostEntity findHostingHost(GuestEntity vm, List<HostEntity> hosts, ReadSpace readSpace) {
        int vmId = readSpace.getId(vm);
        for (HostEntity host : hosts) {
            for (GuestEntity resident : readSpace.getVmListForHost(host)) {
                if (readSpace.getId(resident) == vmId) {
                    return host;
                }
            }
        }
        return null;
    }

    @Override
    public String inputSemantic() {
        return "vm-cpu-overload-loadstate";
    }

    @Override
    public String outputSemantic() {
        return "requestPeAllocation";
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
