package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.HostEntity;
import org.cloudbus.cloudsim.core.GuestEntity;

import java.util.List;

// Executor for ActionSpace.requestHostPowerDown - GUID suffix 10.
public class executor_v9 implements Executor<int[]> {

    private static final int GUID = 3010;
    private static final String SEMANTIC =
        "requestHostPowerDown: power off a host, evacuating and destroying any VMs and workload it currently hosts. Payload {hostId}.";

    @Override
    public boolean execute(int[] actions, ActionSpace actionSpace) {
        double now = actionSpace.getNow();

        if (actions == null || actions.length != 1) {
            Log.printlnConcat(now, ": [executor_v9] rejected - payload must have exactly 1 entry {hostId}.");
            return false;
        }

        if (actions[0] == -1) {
            Log.printlnConcat(now, ": [executor_v9] no-op sentinel received, no power-down requested.");
            return false;
        }

        int hostId = actions[0];
        HostEntity host = actionSpace.getHostById(hostId);

        if (host == null) {
            Log.printlnConcat(now, ": [executor_v9] rejected - host ", hostId, " could not be resolved.");
            return false;
        }

        if (actionSpace.isHostPoweredDown(host)) {
            Log.printlnConcat(now, ": [executor_v9] rejected - host ", hostId, " is already powered down.");
            return false;
        }

        if (actionSpace.isHostFailed(host) || actionSpace.isHostPermanentlyDead(host)) {
            Log.printlnConcat(now, ": [executor_v9] rejected - host ", hostId, " is already failed or permanently dead, power-down has no effect.");
            return false;
        }

        List<GuestEntity> hosted = actionSpace.getVmListForHost(host);
        if (!hosted.isEmpty()) {
            Log.printlnConcat(now, ": [executor_v9] warning - powering down host ", hostId, " will evacuate/destroy ", hosted.size(), " hosted VM(s).");
        }

        actionSpace.requestHostPowerDown(host);
        Log.printlnConcat(now, ": [executor_v9] requested power-down of host ", hostId, ".");
        return true;
    }

    @Override
    public String inputSemantic() {
        return SEMANTIC;
    }

    @Override
    public int inputGuid() {
        return GUID;
    }
}
