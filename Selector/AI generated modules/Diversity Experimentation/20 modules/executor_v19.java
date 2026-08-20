package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.HostEntity;
import org.cloudbus.cloudsim.core.GuestEntity;
import java.util.List;

// Impact-Assessment / Audit-first Executor for requestHostPowerDown (3010).
// Skips hosts that are already powered down, and otherwise logs the number
// of resident VMs and cloudlets that will be paused/evacuated before
// dispatching the power-down request.
public class executor_v19 implements Executor<int[]> {

    @Override
    public boolean execute(int[] actions, ActionSpace actionSpace) {
        if (actions == null || actions.length < 1) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v19] ", "Malformed payload, expected {hostId}");
            return false;
        }

        int hostId = actions[0];
        HostEntity host = actionSpace.getHostById(hostId);
        if (host == null) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v19] ", "Rejected power down, unknown host=" + hostId);
            return false;
        }

        if (actionSpace.isHostPoweredDown(host)) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v19] ", "Skipped power down, host=" + hostId + " is already powered down");
            return false;
        }

        List<GuestEntity> vms = actionSpace.getVmListForHost(host);
        int cloudletCount = 0;
        for (GuestEntity vm : vms) {
            cloudletCount += actionSpace.getVmCloudletList(vm).size();
        }
        Log.printlnConcat(actionSpace.getNow(), ": [executor_v19] ", "Powering down host=" + hostId + " affects " + vms.size() + " vms and " + cloudletCount + " cloudlets");

        actionSpace.requestHostPowerDown(host);
        Log.printlnConcat(actionSpace.getNow(), ": [executor_v19] ", "Powered down host=" + hostId);
        return true;
    }

    @Override
    public String inputSemantic() {
        return "requestHostPowerDown: power off a host";
    }

    @Override
    public int inputGuid() {
        return 3010;
    }
}
