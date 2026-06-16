package org.cloudbus.cloudsim.examples;

import java.util.List;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;

public class Executor1 implements Executor {

    @Override
    public boolean execute(List<MigrationPair> migrations, SimulationContext context) {
        boolean any = false;
        for (MigrationPair p : migrations) {
            migrateCloudlet(p.vm(), p.targetVm(), context);
            any = true;
        }
        return any;
    }

    private void migrateCloudlet(GuestEntity fromVm, GuestEntity toVm, SimulationContext context) {

        Log.printlnConcat("Migrating a cloudlet from VM #", fromVm.getId(), " to VM #", toVm.getId());

        double mostWork = 0;
        Cloudlet cloudletToMigrate = null;
        for (Cloudlet cloudlet : fromVm.getCloudletScheduler().getCloudletExecList()) {
            if (cloudlet.getRemainingCloudletLength() > mostWork) {
                mostWork = cloudlet.getRemainingCloudletLength();
                cloudletToMigrate = cloudlet;
            }
        }

        if (cloudletToMigrate == null) {
            Log.println("No cloudlet to migrate from VM #" + fromVm.getId());
            return;
        }

        long remainingLength = cloudletToMigrate.getRemainingCloudletLength();

        if (!fromVm.getCloudletScheduler().cloudletPause(cloudletToMigrate.getCloudletId())) {
            Log.printlnConcat("Failed to pause cloudlet #", cloudletToMigrate.getCloudletId(), " on VM #", fromVm.getId());
            return;
        }

        cloudletToMigrate.setGuestId(toVm.getId());
        cloudletToMigrate.setCloudletLength(remainingLength);
        cloudletToMigrate.setCloudletFinishedSoFar(0);

        Integer datacenterId = context.getDatacenterFor(toVm);
        if (datacenterId == null) {
            Log.printlnConcat("Destination datacenter for VM #", toVm.getId(), " not found. Migration aborted.");
            return;
        }

        context.sendCloudlet(datacenterId, 0.001, cloudletToMigrate);
    }
}