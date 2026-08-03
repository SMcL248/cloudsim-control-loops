package org.cloudbus.cloudsim.examples;

import java.util.List;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;

/**
 * Planner v2 - Idle host power-down planner (power-oriented).
 *
 * Strategy:
 *   Diagnosis is a per-host LoadState[] (index i corresponds to
 *   readSpace.getAllHosts().get(i)). Looks for an UNDERLOADED host with no
 *   guests currently assigned, that is not failed/dead and not already
 *   powered down or mid power-up, and requests it be powered down to save
 *   energy.
 *
 * Input semantic  : host-loadstate-idle (GUID 2200)
 * Output semantic : host-powerdown      (GUID 3014, requestHostPowerDown)
 */
public class planner_v2 implements Planner<LoadState[], int[]> {

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {
        double now = readSpace.getNow();
        List<HostEntity> hosts = readSpace.getAllHosts();

        if (diagnosis == null || diagnosis.length != hosts.size()) {
            Log.printlnConcat(now, ": [planner_v2] Diagnosis/host size mismatch. No-op.");
            return new int[]{-1};
        }

        for (int i = 0; i < hosts.size(); i++) {
            if (diagnosis[i] != LoadState.UNDERLOADED) continue;

            HostEntity host = hosts.get(i);
            if (readSpace.isHostFailed(host) || readSpace.isHostPermanentlyDead(host)) continue;
            if (readSpace.isHostPoweredDown(host) || readSpace.isHostPoweringUp(host)) continue;

            List<GuestEntity> guests = readSpace.getVmListForHost(host);
            if (!guests.isEmpty()) continue;

            int hostId = readSpace.getId(host);
            Log.printlnConcat(now, ": [planner_v2] Plan power-down for idle host ", hostId);
            return new int[]{hostId};
        }

        Log.printlnConcat(now, ": [planner_v2] No idle, drained host to power down. No-op.");
        return new int[]{-1};
    }

    @Override
    public String inputSemantic() {
        return "host-loadstate-idle";
    }

    @Override
    public String outputSemantic() {
        return "host-powerdown";
    }

    @Override
    public int inputGuid() {
        return 2200;
    }

    @Override
    public int outputGuid() {
        return 3014;
    }
}
