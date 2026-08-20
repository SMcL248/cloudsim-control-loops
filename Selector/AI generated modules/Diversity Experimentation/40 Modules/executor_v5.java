package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;

public class executor_v5 implements Executor<int[]> {

    @Override
    public boolean execute(int[] payload, ActionSpace actionSpace) {
        if (payload == null || payload.length != 2) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v5] ",
                    "malformed payload, expected {vmId, targetHostId}");
            return false;
        }

        int vmId = payload[0];
        int targetHostId = payload[1];

        GuestEntity vm = actionSpace.getVmById(vmId);
        HostEntity targetHost = actionSpace.getHostById(targetHostId);

        if (vm == null || targetHost == null) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v5] ",
                    "unresolved VM or host reference, migration cannot be issued");
            return false;
        }

        actionSpace.requestVmMigration(vm, targetHost);

        Log.printlnConcat(actionSpace.getNow(), ": [executor_v5] ",
                "issued requestVmMigration vm=" + vmId + " target=" + targetHostId + " with no further checks");
        return true;
    }

    @Override
    public String inputSemantic() {
        return "Migrate a VM to a target host";
    }

    @Override
    public int inputGuid() {
        return 3002;
    }
}
