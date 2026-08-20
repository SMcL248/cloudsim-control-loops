package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;

import java.util.List;

// Host level - estimated time to clear all remaining cloudlet work hosted on this host, at full host throughput.
public class monitor_v7 implements Monitor<double[]> {

    private static final int OUTPUT_GUID = 1200;

    @Override
    public double[] observe(ReadSpace readSpace) {
        List<HostEntity> hosts = readSpace.getAllHosts();
        double[] result = new double[hosts.size()];

        for (int i = 0; i < hosts.size(); i++) {
            HostEntity host = hosts.get(i);

            long backlogMi = 0L;
            List<GuestEntity> guests = readSpace.getVmListForHost(host);
            for (GuestEntity vm : guests) {
                List<Cloudlet> cloudlets = readSpace.getVmCloudletList(vm);
                for (Cloudlet cl : cloudlets) {
                    backlogMi += readSpace.getRemainingLength(cl);
                }
            }

            double totalMips = readSpace.getHostTotalMips(host);
            if (totalMips > 0.0) {
                result[i] = backlogMi / totalMips;
            } else {
                result[i] = (backlogMi > 0L) ? Double.MAX_VALUE : 0.0;
            }
        }

        Log.printlnConcat(readSpace.getNow(), ": [monitor_v7] computed host-workload-backlog-time for ", hosts.size(), " hosts.");

        return result;
    }

    @Override
    public String outputSemantic() {
        return "host-workload-backlog-time: estimated time required to fully process all remaining cloudlet "
                + "workload currently assigned to every VM hosted on this host, assuming the host runs at its full "
                + "total MIPS throughput. Aggregates remaining Cloudlet length across all VMs on the host. Returns "
                + "Double.MAX_VALUE if backlog exists but the host has zero total MIPS. Index i corresponds to "
                + "getAllHosts().get(i).";
    }

    @Override
    public int outputGuid() {
        return OUTPUT_GUID;
    }
}
