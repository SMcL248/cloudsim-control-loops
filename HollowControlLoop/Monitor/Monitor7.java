package org.cloudbus.cloudsim.examples;

import java.util.List;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;
import org.cloudbus.cloudsim.Cloudlet;

/**
 * Monitor v2 - Per-host CPU Demand Pressure
 *
 * Metric: sum of MIPS currently requested by all VMs on a host,
 * divided by the host's total MIPS capacity.
 * Unlike utilisation, this reflects what VMs want, not what they are getting.
 * Output range: [0, ...); values > 1 indicate demand exceeds capacity.
 * GUID: host-cpu-demand
 */
public class Monitor7 implements Monitor<double[]> {

    @Override
    public double[] observe(ReadSpace readSpace) {

        Log.enable();
        double now = readSpace.getNow();
        List<HostEntity> hosts = readSpace.getAllHosts();
        double[] metrics = new double[hosts.size()];

        for (int i = 0; i < hosts.size(); i++) {
            HostEntity host = hosts.get(i);
            double totalMips = host.getTotalMips();
            double requestedMips = 0.0;
            double testMips = 0.0;//debugging

            double totalVmLoad = 0.0;

            for (GuestEntity vm : host.getGuestList()) {
                requestedMips += vm.getCurrentRequestedTotalMips();
                testMips += vm.getMips();//debugging
                double cloudletUtil = 0.0;

                for (Cloudlet cl : vm.getCloudletScheduler().getCloudletExecList()){
                    cloudletUtil += cl.getUtilizationOfCpu(now);
                }

                int execCount = vm.getCloudletScheduler().getCloudletExecList().size();
                double avgUtil = (execCount > 0) ? cloudletUtil / execCount : 0.0;

                double vmLoad = avgUtil * vm.getMips();

                totalVmLoad += vmLoad;
                Log.printlnConcat("VM " + vm.getId() + " isBeingInstantiated=" + vm.isBeingInstantiated());

            }


            metrics[i] = (totalMips > 0.0) ? requestedMips / totalMips : 0.0;

            Log.printlnConcat(requestedMips/totalMips, " | ", testMips/totalMips, " | ", totalVmLoad/totalMips);//Debugging
            Log.printlnConcat(now, ": [monitor_v2] Host ", host.getId(),
                    " cpu-demand=", metrics[i]);
            Log.disable();
        }

        return metrics;
    }

    @Override
    public String outputGuid() {
        return "host-cpu-demand";
    }
}