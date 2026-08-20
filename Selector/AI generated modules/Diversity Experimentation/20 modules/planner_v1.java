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

import java.util.List;

/**
 * Strategy: capacity-relief power-up.
 * When any host is flagged OVERLOADED, bring the largest-capacity powered-down
 * host online to absorb the excess load. Candidate ranking is purely by total
 * MIPS capacity, on the assumption that the biggest available host gives the
 * most relief per power-up action.
 */
public class planner_v1 implements Planner<LoadState[], int[]> {

    private static final String MODULE_NAME = "planner_v1";

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

        HostEntity best = null;
        double bestCapacity = -1.0;
        for (HostEntity host : hosts) {
            if (readSpace.isHostFailed(host) || readSpace.isHostPermanentlyDead(host)) {
                continue;
            }
            if (!readSpace.isHostPoweredDown(host) || readSpace.isHostPoweringUp(host)) {
                continue;
            }
            double capacity = readSpace.getHostTotalMips(host);
            if (capacity > bestCapacity) {
                bestCapacity = capacity;
                best = host;
            }
        }

        if (best == null) {
            Log.printlnConcat(readSpace.getNow(), ": [" + MODULE_NAME + "] overload detected but no powered-down host available to power up");
            return new int[0];
        }

        int hostId = readSpace.getId(best);
        Log.printlnConcat(readSpace.getNow(), ": [" + MODULE_NAME + "] powering up host " + hostId + " with capacity " + bestCapacity + " to relieve overload");
        return new int[]{hostId};
    }

    @Override
    public String inputSemantic() {
        return "host-cpu-load-capacity-relief";
    }

    @Override
    public String outputSemantic() {
        return "power-up-largest-capacity-host";
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
