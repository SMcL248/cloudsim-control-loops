package org.cloudbus.cloudsim.examples;// always include

// Import whats needed
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;

// Action: requestHostPowerDown -- power. Only powers down a host that is currently
// idle (no vms allocated) and not already failed/dead/down, so throughput is not harmed.
public class executor_v9 implements Executor<int[]> {

    @Override
    public boolean execute(int[] actions, ActionSpace actionSpace) {
        if (actions == null || actions.length < 1) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v9] invalid payload, aborting requestHostPowerDown");
            return false;
        }

        int hostId = actions[0];
        HostEntity host = actionSpace.getHostById(hostId);

        if (host == null) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v9] host not found, id=", hostId, ", aborting");
            return false;
        }

        if (actionSpace.isHostFailed(host) || actionSpace.isHostPermanentlyDead(host) || actionSpace.isHostPoweredDown(host)) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v9] host=", hostId, " is already failed, dead or powered down, skipping");
            return false;
        }

        java.util.List<GuestEntity> vmsOnHost = actionSpace.getVmListForHost(host);
        if (vmsOnHost != null && !vmsOnHost.isEmpty()) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v9] host=", hostId, " still hosts ", vmsOnHost.size(), " vm(s), refusing to power down");
            return false;
        }

        actionSpace.requestHostPowerDown(host);
        Log.printlnConcat(actionSpace.getNow(), ": [executor_v9] requested power down for idle host=", hostId);
        return true;
    }

    @Override
    public String inputSemantic() {
        return "requestHostPowerDown";
    }

    @Override
    public int inputGuid() {
        return 3014;
    }
}
