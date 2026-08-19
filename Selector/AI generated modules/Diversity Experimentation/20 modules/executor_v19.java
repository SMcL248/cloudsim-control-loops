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


// Strategy: State-Guarded Power-Down. Skips the request if the host is already
// powered down, already failed or permanently dead, or mid power-up transition,
// only firing when the host is in a stable, healthy, powered-on state.
public class executor_v19 implements Executor<int[]> {

    @Override
    public boolean execute(int[] actions, ActionSpace actionSpace) {
        String tag = "executor_v19";

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

        if (actionSpace.isHostPoweredDown(host)) {
            Log.printlnConcat(actionSpace.getNow(), ": [" + tag + "] skipped power down of host ", hostId,
                    ", already powered down");
            return false;
        }

        if (actionSpace.isHostFailed(host) || actionSpace.isHostPermanentlyDead(host)) {
            Log.printlnConcat(actionSpace.getNow(), ": [" + tag + "] skipped power down of host ", hostId,
                    ", host is already failed or permanently dead");
            return false;
        }

        if (actionSpace.isHostPoweringUp(host)) {
            Log.printlnConcat(actionSpace.getNow(), ": [" + tag + "] skipped power down of host ", hostId,
                    ", host is mid power-up transition");
            return false;
        }

        actionSpace.requestHostPowerDown(host);
        Log.printlnConcat(actionSpace.getNow(), ": [" + tag + "] attempted power down of host ", hostId,
                " after confirming it is in a safe, stable, powered-on state");
        return true;
    }

    @Override
    public String inputSemantic() {
        return "power down host, gated on host being stably powered on";
    }

    @Override
    public int inputGuid() {
        return 3010;
    }
}
