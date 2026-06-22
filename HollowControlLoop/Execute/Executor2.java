package org.cloudbus.cloudsim.examples;

import java.util.List;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;

public class Executor2 implements Executor<List<VmMigrationPair>>{
 
    @Override
    public boolean execute(List<VmMigrationPair> migrations, ActionSpace actionSpace){

        if (migrations.isEmpty()){
            return false;
        }

        boolean atLeastOneMigration = false;

        for (var entry : migrations){

            GuestEntity vm = entry.vm();
            HostEntity targetHost = entry.targetHost();

            Integer datacenterId = actionSpace.getDatacenterFor(vm);    
            if (datacenterId == null) {
                Log.printlnConcat("Cannot migrate VM #", vm.getId(), ": datacenter not found.");
            }else{
                actionSpace.requestVmMigration(vm, targetHost);
                Log.printlnConcat("Requested migration of VM #", vm.getId(), " to Host #", targetHost.getId());
                atLeastOneMigration = true;
                break;
            }

        }

        return atLeastOneMigration;

    }

    @Override
    public String actionDescription(){
        return "VM migration";
    }

    @Override
    public String inputGuid() {
        return "host-migration";
    }

}
