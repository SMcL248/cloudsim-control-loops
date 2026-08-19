package org.cloudbus.cloudsim.examples;// always include

// Import whats needed
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


// Strategy: State-Guarded Power-Up. Only fires when the host is confirmed to be
// powered down and not permanently dead, avoiding redundant or futile power-up
// attempts on hosts that are already on or beyond recovery.
public class executor_v20 implements Executor<int[]> {

    @Override
    public boolean execute(int[] actions, ActionSpace actionSpace) {
        String tag = "executor_v20";

        if (actions == null || actions.length != 1) {
            Log.printlnConcat(actionSpace.getNow(), ": [" + tag + "] rejected malformed payload, expected 1 int, got ",
                    (actions == null ? "null" : actions.length));
            return false;
        }

        int hostId = actions[0];
        HostEntity host = actionSpace.getHostById(hostId);

        if (host == null) {
            Log.printlnConcat(actionSpace.getNow(), ": [" + tag + "] cannot resolve host ", hostId);
            return false;
        }

        if (actionSpace.isHostPermanentlyDead(host)) {
            Log.printlnConcat(actionSpace.getNow(), ": [" + tag + "] refused power up of host ", hostId,
                    ", host is permanently dead and cannot be revived");
            return false;
        }

        if (!actionSpace.isHostPoweredDown(host)) {
            Log.printlnConcat(actionSpace.getNow(), ": [" + tag + "] skipped power up of host ", hostId,
                    ", host is not currently powered down");
            return false;
        }

        actionSpace.requestHostPowerUp(host);
        Log.printlnConcat(actionSpace.getNow(), ": [" + tag + "] attempted power up of host ", hostId,
                " after confirming it is powered down and not permanently dead");
        return true;
    }

    @Override
    public String inputSemantic() {
        return "power up host, gated on host being powered down and not permanently dead";
    }

    @Override
    public int inputGuid() {
        return 3011;
    }
}
