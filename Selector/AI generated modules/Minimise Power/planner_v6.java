package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.core.GuestEntity;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Variant 6 - Cloudlet Repacking for VM Drain.
 *
 * Strategy: operates one level below VM migration. Rather than moving whole
 * VMs, it looks for a VM whose *every* active cloudlet is diagnosed
 * UNDERLOADED (a fully-drainable VM, not just a partially light one) and
 * relocates one of its cloudlets onto whichever other VM currently has the
 * most spare throughput headroom. Repeated application empties the source
 * VM's workload entirely, making it a safe target for later destruction -
 * without ever touching a VM that still has meaningful active work on it.
 */
public class planner_v6 implements Planner<LoadState[], int[]> {

    private static final int INPUT_GUID = 2400;
    private static final int OUTPUT_GUID = 3001;

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {
        List<Cloudlet> activeCloudlets = readSpace.getActiveCloudlets();
        List<GuestEntity> vms = readSpace.getVmList();
        int limit = Math.min(diagnosis.length, activeCloudlets.size());

        Map<Integer, Integer> totalPerVm = new HashMap<>();
        Map<Integer, Integer> underloadedPerVm = new HashMap<>();
        Map<Integer, Integer> firstUnderloadedCloudlet = new HashMap<>();

        for (GuestEntity vm : vms) {
            int vmId = readSpace.getId(vm);
            List<Cloudlet> owned = readSpace.getVmCloudletList(vm);
            totalPerVm.put(vmId, owned.size());
            int underCount = 0;
            for (Cloudlet cl : owned) {
                int clId = readSpace.getId(cl);
                for (int i = 0; i < limit; i++) {
                    if (readSpace.getId(activeCloudlets.get(i)) == clId && diagnosis[i] == LoadState.UNDERLOADED) {
                        underCount++;
                        firstUnderloadedCloudlet.putIfAbsent(vmId, clId);
                        break;
                    }
                }
            }
            underloadedPerVm.put(vmId, underCount);
        }

        int sourceVmId = -1;
        int fewestCloudlets = Integer.MAX_VALUE;
        for (Map.Entry<Integer, Integer> entry : totalPerVm.entrySet()) {
            int vmId = entry.getKey();
            int total = entry.getValue();
            if (total == 0) {
                continue;
            }
            Integer underCount = underloadedPerVm.get(vmId);
            if (underCount == null || underCount != total) {
                continue;
            }
            if (total < fewestCloudlets) {
                fewestCloudlets = total;
                sourceVmId = vmId;
            }
        }

        int[] noOp = new int[]{-1, -1, -1};
        if (sourceVmId == -1) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v6] no fully-drainable vm found, emitting no-op");
            return noOp;
        }

        int cloudletId = firstUnderloadedCloudlet.get(sourceVmId);

        GuestEntity targetVm = null;
        double bestHeadroom = -1.0;
        for (GuestEntity vm : vms) {
            int vmId = readSpace.getId(vm);
            if (vmId == sourceVmId) {
                continue;
            }
            if (readSpace.isVmMigrating(vm) || readSpace.isVmBeingInstantiated(vm)) {
                continue;
            }
            double headroom = readSpace.getVmRequestedMips(vm) - readSpace.getVmEffectiveThroughput(vm);
            if (headroom > bestHeadroom) {
                bestHeadroom = headroom;
                targetVm = vm;
            }
        }

        if (targetVm == null) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v6] no vm with spare headroom to receive cloudlet, emitting no-op");
            return noOp;
        }

        int targetVmId = readSpace.getId(targetVm);
        Log.printlnConcat(readSpace.getNow(), ": [planner_v6] repacking cloudlet ", cloudletId,
                " from drainable vm ", sourceVmId, " onto vm ", targetVmId);
        return new int[]{cloudletId, sourceVmId, targetVmId};
    }

    @Override
    public String inputSemantic() {
        return "cloudlet-drainability-loadstate";
    }

    @Override
    public String outputSemantic() {
        return "moveCloudlet";
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
