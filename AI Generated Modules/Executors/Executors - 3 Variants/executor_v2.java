package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;

/**
 * executor_v2 - Validated Executor
 *
 * Strategy: Step-by-step validation with a distinct log message at each
 * failure mode. Separates the VM-not-found and Host-not-found cases so
 * the caller can diagnose which lookup failed. Confirms success with a
 * running count of actions submitted.
 */
public class executor_v2 implements Executor<int[]> {

    private int actionsExecuted = 0;

    @Override
    public boolean execute(int[] actions, ActionSpace actionSpace) {
        double now = actionSpace.getNow();

        if (!isValidAction(actions)) {
            Log.printlnConcat(now, ": [Executor_v2] Invalid or sentinel action. Skipping.");
            return false;
        }

        int vmId   = actions[0];
        int hostId = actions[1];
        Log.printlnConcat(now, ": [Executor_v2] Validating migration plan: VM=", vmId, " TargetHost=", hostId);

        GuestEntity targetVm = null;
        for (GuestEntity g : actionSpace.getVmList()) {
            if (g.getId() == vmId) { targetVm = g; break; }
        }
        if (targetVm == null) {
            Log.printlnConcat(now, ": [Executor_v2] VM ", vmId, " not found in broker VM list. Migration aborted.");
            return false;
        }

        HostEntity targetHost = null;
        for (HostEntity h : actionSpace.getAllHosts()) {
            if (h.getId() == hostId) { targetHost = h; break; }
        }
        if (targetHost == null) {
            Log.printlnConcat(now, ": [Executor_v2] Host ", hostId, " not found in host list. Migration aborted.");
            return false;
        }

        Log.printlnConcat(now, ": [Executor_v2] Validation passed. Requesting migration of VM ", vmId, " to Host ", hostId);
        actionSpace.requestVmMigration(targetVm, targetHost);
        actionsExecuted++;
        Log.printlnConcat(now, ": [Executor_v2] Migration submitted. Total actions executed: ", actionsExecuted);
        return true;
    }

    private boolean isValidAction(int[] actions) {
        return actions != null
                && actions.length == 2
                && !(actions[0] == -1 && actions[1] == -1);
    }

    @Override
    public int getActionsExecuted() {
        return actionsExecuted;
    }

    @Override
    public String inputGuid() {
        return "host-migration-pair";
    }
}
