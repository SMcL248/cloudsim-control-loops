package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.HostEntity;
import org.cloudbus.cloudsim.core.GuestEntity;

import java.util.List;

// Variant angle: requestHostPowerDown, guarded against redundant powerdowns
// (host already off or already mid-power-up) and against destroying a host that
// is already failed/dead (powering down a dead host is meaningless), and it logs
// the blast radius of guest VMs that will be evacuated/destroyed.
public class executor_v9 implements Executor<int[]> {

    @Override
    public boolean execute(int[] actions, ActionSpace actionSpace) {
        double now = actionSpace.getNow();

        if (actions == null || actions.length != 1) {
            Log.printlnConcat(now, ": [executor_v9] REJECTED malformed payload, expected {hostId}");
            return false;
        }

        int hostId = actions[0];
        HostEntity host = actionSpace.getHostById(hostId);

        if (host == null) {
            Log.printlnConcat(now, ": [executor_v9] REJECTED power down, host ", hostId, " could not be resolved");
            return false;
        }

        if (actionSpace.isHostPoweredDown(host)) {
            Log.printlnConcat(now, ": [executor_v9] SKIPPED power down, host ", hostId, " is already off");
            return false;
        }

        if (actionSpace.isHostFailed(host) || actionSpace.isHostPermanentlyDead(host)) {
            Log.printlnConcat(now, ": [executor_v9] SKIPPED power down, host ", hostId, " is already failed/dead");
            return false;
        }

        List<GuestEntity> hostedVms = actionSpace.getVmListForHost(host);
        int hostedCount = (hostedVms == null) ? 0 : hostedVms.size();
        Log.printlnConcat(now, ": [executor_v9] WARNING powering down host ", hostId, " will evacuate/destroy ", hostedCount, " hosted VM(s)");

        actionSpace.requestHostPowerDown(host);
        Log.printlnConcat(now, ": [executor_v9] ATTEMPTED requestHostPowerDown host=", hostId);
        return true;
    }

    @Override
    public String inputSemantic() {
        return "power down host";
    }

    @Override
    public int inputGuid() {
        return 3010;
    }
}
