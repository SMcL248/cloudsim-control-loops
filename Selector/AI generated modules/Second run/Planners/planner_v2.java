package org.cloudbus.cloudsim.examples;// always include

import java.util.List;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;

// Host-level planner. diagnosis[i] is the load state of readSpace.getAllHosts().get(i).
// Goal: maximise throughput / minimise makespan.
// Strategy: relieve the most congested host by migrating its highest-demand
// VM (by requested MIPS) onto the best available UNDERLOADED host, so the
// hot host stops throttling the cloudlets queued behind it.
public class planner_v2 implements Planner<LoadState[], int[]> {

    private static final String MODULE_NAME = "planner_v2";
    private static final int INPUT_GUID = 2200;
    private static final int OUTPUT_GUID = 3002;

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {
        double now = readSpace.getNow();
        List<HostEntity> hosts = readSpace.getAllHosts();

        if (diagnosis == null || diagnosis.length != hosts.size()) {
            Log.printlnConcat(now, ": [" + MODULE_NAME + "] diagnosis/host size mismatch, no-op");
            return new int[]{-1, -1};
        }

        HostEntity sourceHost = null;
        GuestEntity vmToMigrate = null;
        double bestMips = -1.0;

        for (int i = 0; i < hosts.size(); i++) {
            if (diagnosis[i] != LoadState.OVERLOADED) continue;
            HostEntity host = hosts.get(i);
            if (readSpace.isHostFailed(host) || readSpace.isHostPermanentlyDead(host)) continue;

            for (GuestEntity vm : readSpace.getVmListForHost(host)) {
                if (readSpace.isVmMigrating(vm) || readSpace.isVmBeingInstantiated(vm)) continue;
                double mips = readSpace.getVmRequestedMips(vm);
                if (mips > bestMips) {
                    bestMips = mips;
                    vmToMigrate = vm;
                    sourceHost = host;
                }
            }
        }

        if (vmToMigrate == null) {
            Log.printlnConcat(now, ": [" + MODULE_NAME + "] no overloaded host with migratable VM, no-op");
            return new int[]{-1, -1};
        }

        HostEntity destHost = null;
        for (int i = 0; i < hosts.size(); i++) {
            if (diagnosis[i] != LoadState.UNDERLOADED) continue;
            HostEntity candidate = hosts.get(i);
            if (readSpace.getId(candidate) == readSpace.getId(sourceHost)) continue;
            if (readSpace.isHostFailed(candidate) || readSpace.isHostPermanentlyDead(candidate)) continue;
            if (readSpace.isHostPoweredDown(candidate) || readSpace.isHostPoweringUp(candidate)) continue;
            if (!readSpace.canMigrateGuestToHost(candidate, vmToMigrate)) continue;
            destHost = candidate;
            break;
        }

        if (destHost == null) {
            Log.printlnConcat(now, ": [" + MODULE_NAME + "] no suitable destination host, no-op");
            return new int[]{-1, -1};
        }

        int vmId = readSpace.getId(vmToMigrate);
        int destId = readSpace.getId(destHost);
        Log.printlnConcat(now, ": [" + MODULE_NAME + "] plan migrate VM ", vmId,
                " off overloaded host ", readSpace.getId(sourceHost), " -> host ", destId);
        return new int[]{vmId, destId};
    }

    @Override
    public String inputSemantic() {
        return "host-mips-congestion-overload";
    }

    @Override
    public String outputSemantic() {
        return "requestvmmigration";
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
