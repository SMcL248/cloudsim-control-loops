package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;
import java.util.List;

public class executor_v7 implements Executor<int[]> {

    @Override
    public boolean execute(int[] payload, ActionSpace actionSpace) {
        if (payload == null || payload.length != 2) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v7] ",
                    "malformed payload, expected {vmId, targetHostId}");
            return false;
        }

        int vmId = payload[0];
        int targetHostId = payload[1];

        GuestEntity vm = actionSpace.getVmById(vmId);
        HostEntity targetHost = actionSpace.getHostById(targetHostId);

        if (vm == null || targetHost == null) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v7] ",
                    "aborting migration, unresolved VM or host reference");
            return false;
        }

        List<GuestEntity> alreadyHosted = actionSpace.getVmListForHost(targetHost);
        if (alreadyHosted != null && alreadyHosted.contains(vm)) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v7] ",
                    "skipping migration, VM " + vmId + " is already resident on host " + targetHostId);
            return false;
        }

        if (actionSpace.isVmMigrating(vm)) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v7] ",
                    "skipping migration, VM " + vmId + " already has a migration in flight");
            return false;
        }

        actionSpace.requestVmMigration(vm, targetHost);

        Log.printlnConcat(actionSpace.getNow(), ": [executor_v7] ",
                "redundancy check passed, issued requestVmMigration vm=" + vmId + " target=" + targetHostId);
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
