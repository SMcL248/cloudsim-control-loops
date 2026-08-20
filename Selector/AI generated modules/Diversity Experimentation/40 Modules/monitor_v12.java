package org.cloudbus.cloudsim.examples;// always include

// Import whats needed
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;
import org.cloudbus.cloudsim.core.PowerGuestEntity;
import org.cloudbus.cloudsim.core.PowerHostEntity;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.power.PowerDatacenter;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.power.PowerVm;

public class monitor_v12 implements Monitor<double[]> {

    @Override
    public double[] observe(ReadSpace readSpace) {
        java.util.List<HostEntity> hosts = readSpace.getAllHosts();
        double[] result = new double[hosts.size()];
        for (int i = 0; i < hosts.size(); i++) {
            HostEntity host = hosts.get(i);
            double totalMips = readSpace.getHostTotalMips(host);
            double totalRam = readSpace.getHostTotalRam(host);
            double totalBw = readSpace.getHostTotalBw(host);
            double mipsPressure = totalMips > 1e-6 ? (1.0 - (readSpace.getHostAvailableMips(host) / totalMips)) : 0.0;
            double ramPressure = totalRam > 1e-6 ? (1.0 - (readSpace.getHostAvailableRam(host) / totalRam)) : 0.0;
            double bwPressure = totalBw > 1e-6 ? (1.0 - (readSpace.getHostAvailableBw(host) / totalBw)) : 0.0;
            double mean = (mipsPressure + ramPressure + bwPressure) / 3.0;
            double variance = (Math.pow(mipsPressure - mean, 2) + Math.pow(ramPressure - mean, 2) + Math.pow(bwPressure - mean, 2)) / 3.0;
            result[i] = Math.sqrt(variance);
        }
        Log.printlnConcat(readSpace.getNow(), ": [monitor_v12] computed cross-dimension pressure imbalance for ", hosts.size(), " hosts");
        return result;
    }

    @Override
    public String outputSemantic() {
        return "host-resource-pressure-imbalance-stddev-across-mips-ram-bw";
    }

    @Override
    public int outputGuid() {
        return 1200;
    }
}
