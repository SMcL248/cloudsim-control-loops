package org.cloudbus.cloudsim.examples;

import java.util.List;

import org.cloudbus.cloudsim.CloudletSchedulerTimeShared;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.GuestEntity;

public class Executor3 implements Executor<List<CreateVmAction>>{
 
    @Override
    public boolean execute(List<CreateVmAction> scalingActions, ActionSpace actionSpace){

        if (scalingActions.isEmpty()){
            return false;
        }

        boolean atLeastOneScale = false;

        for (var entry : scalingActions){


            int id = entry.datacenterId();

            long size = 10000; //image size (MB)
            int ram = 512; //vm memory (MB)
            int mips = 250;
            long bw = 1000;
            int pesNumber = 1; //number of cpus
            String vmm = "Xen"; //VMM name

            //Find largest current ID to ensure unique ID allocation
            List<GuestEntity> vmList = actionSpace.getVmList();

            int largestId = 0;
            for (GuestEntity vm : vmList){
                if (vm.getId() > largestId){
                    largestId = vm.getId();
                }
            }

            GuestEntity newVM = new Vm(largestId + 1, actionSpace.getUserId(), mips, pesNumber, ram, bw, size, vmm, new CloudletSchedulerTimeShared());

            actionSpace.requestVmCreation(newVM, id);
            atLeastOneScale = true;

        }

        return atLeastOneScale;

    }

    @Override
    public String actionDescription(){
        return "VM Scaling";
    }

    @Override
    public String inputGuid() {
        return "host-scaleup";
    }

}
