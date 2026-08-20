package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;
import org.cloudbus.cloudsim.core.PowerGuestEntity;
import org.cloudbus.cloudsim.core.PowerHostEntity;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.power.PowerDatacenter;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.power.PowerVm;

import java.util.ArrayList;
import java.util.List;

/**
 * Strategy: time-rotation power-up (wear levelling).
 * When any host is flagged OVERLOADED, this planner does not rank
 * powered-down hosts by capacity; instead it rotates through them using
 * the current simulation time as a cursor. This spreads power-up cycles
 * evenly across the idle fleet over the life of the simulation rather
 * than always favouring the same "best" host.
 */
public class planner_v17 implements Planner<LoadState[], int[]> {

    private static final String MODULE_NAME = "planner_v17";

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {
        List<HostEntity> hosts = readSpace.getAllHosts();

        boolean overloadPresent = false;
        for (int i = 0; i < diagnosis.length && i < hosts.size(); i++) {
            if (diagnosis[i] == LoadState.OVERLOADED) {
                overloadPresent = true;
                break;
            }
        }

        if (!overloadPresent) {
            Log.printlnConcat(readSpace.getNow(), ": [" + MODULE_NAME + "] no host overload detected, no action taken");
            return new int[0];
        }

        List<HostEntity> poweredDown = new ArrayList<HostEntity>();
        for (HostEntity host : hosts) {
            if (readSpace.isHostFailed(host) || readSpace.isHostPermanentlyDead(host)) {
                continue;
            }
            if (readSpace.isHostPoweredDown(host) && !readSpace.isHostPoweringUp(host)) {
                poweredDown.add(host);
            }
        }

        if (poweredDown.isEmpty()) {
            Log.printlnConcat(readSpace.getNow(), ": [" + MODULE_NAME + "] overload detected but no powered-down host available in rotation");
            return new int[0];
        }

        long cursor = (long) readSpace.getNow();
        int index = (int) (cursor % poweredDown.size());
        HostEntity chosen = poweredDown.get(index);

        int hostId = readSpace.getId(chosen);
        Log.printlnConcat(readSpace.getNow(), ": [" + MODULE_NAME + "] rotation cursor " + index + " selected host " + hostId + " for power-up");
        return new int[]{hostId};
    }

    @Override
    public String inputSemantic() {
        return "host-cpu-load-capacity-relief";
    }

    @Override
    public String outputSemantic() {
        return "power-up-round-robin-host";
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
