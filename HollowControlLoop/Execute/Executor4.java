package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;
import org.cloudbus.cloudsim.lists.HostList;
import org.cloudbus.cloudsim.lists.VmList;

public class Executor4 implements Executor<int[]>{
 
    @Override
    public boolean execute(int[] migration, ActionSpace actionSpace){

        double now = actionSpace.getNow();

        if (migration[0] == -1){
            return false;
        }

        boolean atLeastOneMigration = false;

        GuestEntity vm = VmList.getById(actionSpace.getVmList(), migration[0]);
        HostEntity targetHost = HostList.getById(actionSpace.getAllHosts(), migration[1]);

        Integer datacenterId = actionSpace.getDatacenterFor(vm);    
        if (datacenterId == null) {
            Log.printlnConcat(now, ": Cannot migrate VM #", vm.getId(), ": datacenter not found.");
        }else{
            actionSpace.requestVmMigration(vm, targetHost);
            Log.printlnConcat(now, ": Requested migration of VM #", vm.getId(), " to Host #", targetHost.getId());
            atLeastOneMigration = true;
        }

        return atLeastOneMigration;

    }

    @Override
    public String inputGuid() {
        return "host-migration";
    }

}
