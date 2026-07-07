package org.cloudbus.cloudsim.examples;

import java.util.List;
import java.util.HashSet;
import java.util.Set;

import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;

import org.cloudbus.cloudsim.power.PowerVm;
import org.cloudbus.cloudsim.power.PowerHost;


public class Planner5 implements Planner<LoadState[], int[]>{

    @Override
    public int[] plan(LoadState[] classification, ReadSpace readSpace){

        int[] migration = new int[]{-1,-1};

        HostEntity leastLoaded = null;
        HostEntity mostLoaded = null;
        double mostDemand = 0.0;
        double leastDemand = Double.MAX_VALUE;
        
        List<HostEntity> hosts = readSpace.getAllHosts();

        int i = 0;

        // Find the hosts with the least and most demand
        for (HostEntity host : hosts){

            if (classification[i] == LoadState.BALANCED){

                double totalMips = host.getTotalMips();
                double requestedMips = 0.0;

                for (GuestEntity vm : host.getGuestList()) {
                    requestedMips += vm.getCurrentRequestedTotalMips();
                }

                double demand = requestedMips/totalMips;

                if (demand < leastDemand){
                    leastLoaded = host;
                    leastDemand = demand;
                }
                if (demand > mostDemand){
                    mostLoaded = host;
                    mostDemand = demand;
                }

            }

            i++;
    
        }

        // If least and most loaded hosts found, check suitability and plan migration
        if (leastLoaded != null && mostLoaded != null && mostLoaded != leastLoaded){
            GuestEntity targetVm = leastLoaded.getGuestList().getFirst();
            if (mostLoaded.isSuitableForGuest(targetVm)){
                migration[0] = targetVm.getId();
                migration[1] = mostLoaded.getId();
                double now = readSpace.getNow();
                // Log.enable();
                Log.printlnConcat(now, "| Planning to migrate VM #", migration[0], " to Host #", migration[1], ".");
                // Log.disable();
            }
        }

        return migration;

    }

    @Override
    public String inputGuid() {
        return "host-demand-loadstate";
    }

    @Override
    public String outputGuid() {
        return "vm-migration";
    }
}
