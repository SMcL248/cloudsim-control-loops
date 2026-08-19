package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.HostEntity;

// Variant angle: requestHostPowerUp, guarded to only fire when the host is
// actually off and not already mid-power-up, and refuses to power up a host
// deemed permanently dead (that host cannot recover regardless of the request).
public class executor_v10 implements Executor<int[]> {

    @Override
    public boolean execute(int[] actions, ActionSpace actionSpace) {
        double now = actionSpace.getNow();

        if (actions == null || actions.length != 1) {
            Log.printlnConcat(now, ": [executor_v10] REJECTED malformed payload, expected {hostId}");
            return false;
        }

        int hostId = actions[0];
        HostEntity host = actionSpace.getHostById(hostId);

        if (host == null) {
            Log.printlnConcat(now, ": [executor_v10] REJECTED power up, host ", hostId, " could not be resolved");
            return false;
        }

        if (actionSpace.isHostPermanentlyDead(host)) {
            Log.printlnConcat(now, ": [executor_v10] SKIPPED power up, host ", hostId, " is permanently dead");
            return false;
        }

        if (actionSpace.isHostPoweringUp(host)) {
            Log.printlnConcat(now, ": [executor_v10] SKIPPED power up, host ", hostId, " is already powering up");
            return false;
        }

        if (!actionSpace.isHostPoweredDown(host)) {
            Log.printlnConcat(now, ": [executor_v10] SKIPPED power up, host ", hostId, " is not currently off");
            return false;
        }

        actionSpace.requestHostPowerUp(host);
        Log.printlnConcat(now, ": [executor_v10] ATTEMPTED requestHostPowerUp host=", hostId);
        return true;
    }

    @Override
    public String inputSemantic() {
        return "power up host";
    }

    @Override
    public int inputGuid() {
        return 3011;
    }
}
