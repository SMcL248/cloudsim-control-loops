package org.cloudbus.cloudsim.examples;

import java.util.List;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;

public class Monitor6 implements Monitor<double[]>{

    @Override
    public double[] observe(ReadSpace readSpace) {

        double now = readSpace.getNow();

        Log.printlnConcat(now, "| Observing...");

        // List of all hosts
        List<HostEntity> hosts = readSpace.getAllHosts();

        double[] metrics = new double[hosts.size()];

        int i = 0;

        // Calculate CPU utilisation for each host
        for (HostEntity host : hosts){

            double total = host.getTotalMips();

            double used = 0;

            for (GuestEntity vm : host.getGuestList()){
                used += vm.getTotalUtilizationOfCpuMips(now); 
            }

            double util = used/total;

            Log.printlnConcat(now, ": Host #", host.getId(), "| Total CPU Utilisation: ", util);
            metrics[i] = util;

            i++;    

        }

        return metrics;

    }

    @Override
    public String outputGuid() {
        return "host-cpu";
    }
    
}
