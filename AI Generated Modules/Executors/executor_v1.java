package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;

/**
 * Executor variant 1: VM Migration Executor (host level).
 *
 * Receives a pair [vmId, hostId] and requests migration of the identified VM
 * to the identified host via the ActionSpace.
 *
 * Sentinel input {-1, -1} signals no migration is needed; returns false.
 */
public class executor_v1 implements Executor<int[]> {

    @Override
    public String inputGuid() {
        return "host-migration-pair";
    }

    @Override
    public boolean execute(int[] actions, ActionSpace actionSpace) {
        double now = actionSpace.getNow();

        // Sentinel check: no migration requested
        if (actions == null || actions.length < 2 || (actions[0] == -1 && actions[1] == -1)) {
            Log.printlnConcat(now, ": [Executor] No migration requested (sentinel received). Skipping.");
            return false;
        }

        int vmId   = actions[0];
        int hostId = actions[1];

        Log.printlnConcat(now, ": [Executor] Migration request received: VM ", vmId, " -> Host ", hostId);

        // Resolve VM by ID
        GuestEntity targetVm = null;
        for (GuestEntity vm : actionSpace.getVmList()) {
            if (vm.getId() == vmId) {
                targetVm = vm;
                break;
            }
        }

        if (targetVm == null) {
            Log.printlnConcat(now, ": [Executor] VM ", vmId, " not found in VM list. Migration aborted.");
            return false;
        }

        // Resolve Host by ID
        HostEntity targetHost = null;
        for (HostEntity host : actionSpace.getAllHosts()) {
            if (host.getId() == hostId) {
                targetHost = host;
                break;
            }
        }

        if (targetHost == null) {
            Log.printlnConcat(now, ": [Executor] Host ", hostId, " not found in host list. Migration aborted.");
            return false;
        }

        // Request migration
        Log.printlnConcat(now, ": [Executor] Issuing migration: VM ", vmId, " to Host ", hostId);
        actionSpace.requestVmMigration(targetVm, targetHost);

        Log.printlnConcat(now, ": [Executor] Migration request submitted successfully.");
        return true;
    }
}
