package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.HostEntity;
import java.util.List;

public class planner_v2 implements Planner<LoadState[], int[]> {

    private static final double OVERLOAD_TRIGGER_FRACTION = 0.34;

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {
        int overloadedCount = 0;
        for (LoadState state : diagnosis) {
            if (state == LoadState.OVERLOADED) {
                overloadedCount++;
            }
        }

        if (diagnosis.length == 0 || (double) overloadedCount / diagnosis.length < OVERLOAD_TRIGGER_FRACTION) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v2] overload fraction below trigger, no power-up requested");
            return new int[0];
        }

        List<HostEntity> hosts = readSpace.getAllHosts();
        for (HostEntity host : hosts) {
            if (readSpace.isHostPoweredDown(host) && !readSpace.isHostPermanentlyDead(host) && !readSpace.isHostFailed(host) && !readSpace.isHostPoweringUp(host)) {
                int hostId = readSpace.getId(host);
                Log.printlnConcat(readSpace.getNow(), ": [planner_v2] fleet overload fraction=", (double) overloadedCount / diagnosis.length, ", powering up host ", hostId);
                return new int[] { hostId };
            }
        }

        Log.printlnConcat(readSpace.getNow(), ": [planner_v2] fleet overloaded but no dormant host available to power up");
        return new int[0];
    }

    @Override
    public String inputSemantic() {
        return "host-loadstate-fleetwide-overload-pressure";
    }

    @Override
    public String outputSemantic() {
        return "host-power-up";
    }

    @Override
    public int inputGuid() {
        return 2200;
    }

    @Override
    public int outputGuid() {
        return 3011;
    }
}
