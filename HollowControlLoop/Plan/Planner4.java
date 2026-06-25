package org.cloudbus.cloudsim.examples;

import java.util.List;
import java.util.HashSet;
import java.util.Set;

import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;


public class Planner4 implements Planner<LoadState[], int[]>{

    @Override
    public int[] plan(LoadState[] classification, ReadSpace readSpace){

        int[] migration = new int[]{-1,-1};
        
        Set<HostEntity> allocatedTargets = new HashSet<>();

        List<HostEntity> hosts = readSpace.getAllHosts();

        int i = 0;
        // Iterate by host in datacenter to find overloaded
        for (HostEntity host : hosts){

            HostEntity targetHost = null;
            LoadState state = classification[i];

            if (state == LoadState.OVERLOADED){

                int ii = 0;
                // Iterate by host in datacenter again to find underloaded match
                for (HostEntity host2 : hosts){

                    LoadState state2 = classification[ii];

                    if (state2 == LoadState.UNDERLOADED && !allocatedTargets.contains(host2)){

                        targetHost = host2;
                        double max_mips = 0;
                        GuestEntity targetVm = null;

                        //Find VM using most MIPS
                        for (GuestEntity vm : readSpace.getVmList()){

                            if (vm.getCurrentRequestedTotalMips() > max_mips){
                                targetVm = vm;
                                max_mips = vm.getCurrentRequestedTotalMips();
                            }
                        }

                        if (targetVm == null){
                            break;
                        }

                        if (targetHost.isSuitableForGuest(targetVm)){
                            allocatedTargets.add(targetHost);
                            migration[0] = targetVm.getId();
                            migration[1] = targetHost.getId();
                            break;
                        }

                    }

                    ii++;

                }

            }

            i++;
        }

        return migration;

    }

    @Override
    public String inputGuid() {
        return "host-loadstate";
    }

    @Override
    public String outputGuid() {
        return "host-migration";
    }
}
