package org.cloudbus.cloudsim.examples;// always include

// Import whats needed
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;

// Action: requestVmMigration -- power-aware. Refuses to migrate a vm onto a failed, dead,
// or currently powered-down host, since waking a host purely to receive a migration works
// against the minimise-power goal.
public class executor_v3 implements Executor<int[]> {

    private int successfulActionCount = 0;

    @Override
    public boolean execute(int[] actions, ActionSpace actionSpace) {
        if (actions == null || actions.length < 2) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v3] invalid payload, aborting requestVmMigration");
            return false;
        }

        int vmId = actions[0];
        int targetHostId = actions[1];

        GuestEntity vm = actionSpace.getVmById(vmId);
        HostEntity targetHost = actionSpace.getHostById(targetHostId);

        if (vm == null || targetHost == null) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v3] could not resolve vm or host ids, aborting");
            return false;
        }

        if (actionSpace.isHostFailed(targetHost) || actionSpace.isHostPermanentlyDead(targetHost)) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v3] target host=", targetHostId, " is failed or dead, aborting migration");
            return false;
        }

        if (actionSpace.isHostPoweredDown(targetHost)) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v3] target host=", targetHostId, " is powered down, skipping migration to save power");
            return false;
        }

        actionSpace.requestVmMigration(vm, targetHost);
        Log.printlnConcat(actionSpace.getNow(), ": [executor_v3] requested migration of vm=", vmId, " to host=", targetHostId);
        return true;
    }

    @Override
    public String inputSemantic() {
        return "requestVmMigration";
    }

    @Override
    public int inputGuid() {
        return 3006;
    }

    @Override
    public int getSuccessfulActionCount() {
        return successfulActionCount;
    }
}
