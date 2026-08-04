package org.cloudbus.cloudsim.examples;// always include

import java.util.List;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.HostEntity;

// Host-level planner. diagnosis[i] is the load state of readSpace.getAllHosts().get(i).
// Goal: maximise service availability - preserve host capacity for new or
// migrating VMs.
// Strategy: when at least one host is OVERLOADED, spare standby capacity is
// worth having on hand even before a migration is attempted. Power up the
// first powered-down host that is not failed or permanently dead, so a
// destination is available for the next migration/creation request.
public class planner_v8 implements Planner<LoadState[], int[]> {

    private static final String MODULE_NAME = "planner_v8";
    private static final int INPUT_GUID = 2200;
    private static final int OUTPUT_GUID = 3011;

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {
        double now = readSpace.getNow();
        List<HostEntity> hosts = readSpace.getAllHosts();

        if (diagnosis == null || diagnosis.length != hosts.size()) {
            Log.printlnConcat(now, ": [" + MODULE_NAME + "] diagnosis/host size mismatch, no-op");
            return new int[]{-1};
        }

        boolean anyOverloaded = false;
        for (LoadState state : diagnosis) {
            if (state == LoadState.OVERLOADED) {
                anyOverloaded = true;
                break;
            }
        }

        if (!anyOverloaded) {
            Log.printlnConcat(now, ": [" + MODULE_NAME + "] no overloaded hosts, standby capacity sufficient, no-op");
            return new int[]{-1};
        }

        for (HostEntity host : hosts) {
            if (readSpace.isHostFailed(host) || readSpace.isHostPermanentlyDead(host)) continue;
            if (!readSpace.isHostPoweredDown(host)) continue;
            if (readSpace.isHostPoweringUp(host)) continue;

            int hostId = readSpace.getId(host);
            Log.printlnConcat(now, ": [" + MODULE_NAME + "] plan power up standby host ", hostId,
                    " to preserve availability under overload");
            return new int[]{hostId};
        }

        Log.printlnConcat(now, ": [" + MODULE_NAME + "] no powered-down host available to bring online, no-op");
        return new int[]{-1};
    }

    @Override
    public String inputSemantic() {
        return "host-mips-congestion-overload";
    }

    @Override
    public String outputSemantic() {
        return "requesthostpowerup";
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
