package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.HostEntity;

import java.util.List;

// Host level - guest co-location density relative to PE count.
public class monitor_v5 implements Monitor<double[]> {

    private static final int OUTPUT_GUID = 1200;

    @Override
    public double[] observe(ReadSpace readSpace) {
        List<HostEntity> hosts = readSpace.getAllHosts();
        double[] result = new double[hosts.size()];

        for (int i = 0; i < hosts.size(); i++) {
            HostEntity host = hosts.get(i);
            int guestCount = readSpace.getVmListForHost(host).size();
            int peCount = readSpace.getHostPeCount(host);
            result[i] = (peCount > 0) ? (guestCount / (double) peCount) : 0.0;
        }

        Log.printlnConcat(readSpace.getNow(), ": [monitor_v5] computed host-guest-packing-density for ", hosts.size(), " hosts.");

        return result;
    }

    @Override
    public String outputSemantic() {
        return "host-guest-packing-density: number of VMs currently hosted divided by the host's PE count, a proxy "
                + "for co-location pressure per processing element. Since PEs represent a MIPS-rate ceiling that "
                + "degrades throughput rather than a hard slot count, this is a pressure indicator, not an "
                + "occupancy fraction. Index i corresponds to getAllHosts().get(i).";
    }

    @Override
    public int outputGuid() {
        return OUTPUT_GUID;
    }
}
