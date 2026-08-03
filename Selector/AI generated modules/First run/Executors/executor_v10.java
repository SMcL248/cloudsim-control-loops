package org.cloudbus.cloudsim.examples;// always include

// Import whats needed
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.HostEntity;

// Action: requestHostPowerUp -- throughput. Brings a powered-down host back online to add
// capacity, skipping hosts that are already up or already mid power-up to avoid redundant calls.
public class executor_v10 implements Executor<int[]> {

    @Override
    public boolean execute(int[] actions, ActionSpace actionSpace) {
        if (actions == null || actions.length < 1) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v10] invalid payload, aborting requestHostPowerUp");
            return false;
        }

        int hostId = actions[0];
        HostEntity host = actionSpace.getHostById(hostId);

        if (host == null) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v10] host not found, id=", hostId, ", aborting");
            return false;
        }

        if (actionSpace.isHostPermanentlyDead(host)) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v10] host=", hostId, " is permanently dead, refusing to power up");
            return false;
        }

        if (!actionSpace.isHostPoweredDown(host) || actionSpace.isHostPoweringUp(host)) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v10] host=", hostId, " is already up or already powering up, skipping redundant call");
            return false;
        }

        actionSpace.requestHostPowerUp(host);
        Log.printlnConcat(actionSpace.getNow(), ": [executor_v10] requested power up for host=", hostId);
        return true;
    }

    @Override
    public String inputSemantic() {
        return "requestHostPowerUp";
    }

    @Override
    public int inputGuid() {
        return 3015;
    }
}
