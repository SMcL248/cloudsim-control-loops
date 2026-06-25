package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;

/**
 * executor_v1 - Eager Executor
 *
 * Strategy: Minimal guards, fast path to migration.
 * Resolves VM and Host entities by ID, then immediately
 * submits the migration request. Logs at key decision points only.
 */
public class executor_v1 implements Executor<int[]> {

    private int actionsExecuted = 0;

    @Override
    public boolean execute(int[] actions, ActionSpace actionSpace) {
        double now = actionSpace.getNow();

        if (actions[0] == -1 && actions[1] == -1) {
            Log.printlnConcat(now, ": [Executor_v1] Sentinel received. No migration.");
            return false;
        }

        int vmId   = actions[0];
        int hostId = actions[1];

        GuestEntity targetVm   = findVm(vmId, actionSpace);
        HostEntity  targetHost = findHost(hostId, actionSpace);

        if (targetVm == null || targetHost == null) {
            Log.printlnConcat(now, ": [Executor_v1] Entity lookup failed. vmId=", vmId, " hostId=", hostId, ". Migration aborted.");
            return false;
        }

        Log.printlnConcat(now, ": [Executor_v1] Migrating VM ", vmId, " -> Host ", hostId);
        actionSpace.requestVmMigration(targetVm, targetHost);
        actionsExecuted++;
        return true;
    }

    private GuestEntity findVm(int id, ActionSpace actionSpace) {
        for (GuestEntity vm : actionSpace.getVmList()) {
            if (vm.getId() == id) return vm;
        }
        return null;
    }

    private HostEntity findHost(int id, ActionSpace actionSpace) {
        for (HostEntity host : actionSpace.getAllHosts()) {
            if (host.getId() == id) return host;
        }
        return null;
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
