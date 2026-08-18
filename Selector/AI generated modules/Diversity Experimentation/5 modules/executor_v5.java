package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.HostEntity;
import org.cloudbus.cloudsim.core.GuestEntity;
import java.util.List;

// Executor variant implementing requestHostPowerDown (GUID suffix 10).
// Payload: {hostId}
// Note: powering down a host destroys its hosted VMs and their allocated workloads.
public class executor_v5 implements Executor<int[]> {

    private static final int EXPECTED_LENGTH = 1;

    @Override
    public boolean execute(int[] actions, ActionSpace actionSpace) {
        if (actions == null || actions.length != EXPECTED_LENGTH) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v5] rejected payload, expected 1 int {hostId} but got length ",
                    actions == null ? "null" : actions.length);
            return false;
        }

        int hostId = actions[0];
        HostEntity host = actionSpace.getHostById(hostId);

        if (host == null) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v5] skipped power-down, unknown host id ", hostId);
            return false;
        }

        if (actionSpace.isHostPoweredDown(host)) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v5] skipped power-down, host ", hostId, " is already off");
            return false;
        }

        List<GuestEntity> hostedVms = actionSpace.getVmListForHost(host);
        if (!hostedVms.isEmpty()) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v5] warning, powering down host ", hostId,
                    " will evict ", hostedVms.size(), " vm(s) and their workloads");
        }

        actionSpace.requestHostPowerDown(host);
        Log.printlnConcat(actionSpace.getNow(), ": [executor_v5] requested power-down of host ", hostId);
        return true;
    }

    @Override
    public String inputSemantic() {
        return "requestHostPowerDown action payload {hostId}";
    }

    @Override
    public int inputGuid() {
        return 3010;
    }
}
