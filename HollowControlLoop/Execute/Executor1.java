package org.cloudbus.cloudsim.examples;

import java.util.List;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;

public class Executor1 implements Executor<List<MigrationPair>> {

    @Override
    public boolean execute(List<MigrationPair> migrations, ActionSpace actionSpace) {
        boolean any = false;
        for (MigrationPair p : migrations) {
            migrateCloudlet(p.targetCloudlet(), p.fromVm(), p.targetVm(), actionSpace);
            any = true;
        }
        return any;
    }

    private void migrateCloudlet(Cloudlet targetCloudlet, GuestEntity fromVm, GuestEntity toVm, ActionSpace actionSpace) {
        Log.printlnConcat("Migrating cloudlet #", targetCloudlet.getCloudletId(), " from VM #", fromVm.getId(), " to VM #", toVm.getId());

        Integer datacenterId = actionSpace.getDatacenterFor(toVm);
        if (datacenterId == null) {
            Log.printlnConcat("Destination datacenter for VM #", toVm.getId(), " not found. Migration aborted.");
            return;
        }

        actionSpace.moveCloudlet(targetCloudlet, fromVm, toVm, datacenterId);
    }

    @Override
    public String inputGuid() {
        return "vm-migration";
    }


}