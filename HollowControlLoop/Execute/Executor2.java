package org.cloudbus.cloudsim.examples;

import java.util.List;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;
import org.cloudbus.cloudsim.util.HistoryStat;

public class Executor2 implements Executor<List<VmMigrationPair>>{
 
    @Override
    public boolean execute(List<VmMigrationPair> migrations, SimulationContext context){

        if (migrations.isEmpty()){
            return false;
        }

        boolean atLeastOneMigration = false;

        for (var entry : migrations){

            GuestEntity vm = entry.vm();
            HostEntity targetHost = entry.targetHost();

            Integer datacenterId = context.getDatacenterFor(vm);    
            if (datacenterId == null) {
                Log.printlnConcat("Cannot migrate VM #", vm.getId(), ": datacenter not found.");
            }else{
                context.requestVmMigration(vm, targetHost);
                Log.printlnConcat("Requested migration of VM #", vm.getId(), " to Host #", targetHost.getId());
                atLeastOneMigration = true;
                break;
            }

        }

        return atLeastOneMigration;

    }

}
