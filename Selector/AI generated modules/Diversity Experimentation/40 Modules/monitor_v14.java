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

public class monitor_v14 implements Monitor<double[]> {

    @Override
    public double[] observe(ReadSpace readSpace) {
        java.util.List<HostEntity> hosts = readSpace.getAllHosts();
        double[] result = new double[hosts.size()];
        for (int i = 0; i < hosts.size(); i++) {
            HostEntity host = hosts.get(i);
            double totalMips = readSpace.getHostTotalMips(host);
            double sumEffective = 0.0;
            for (GuestEntity vm : readSpace.getVmListForHost(host)) {
                sumEffective += readSpace.getVmEffectiveThroughput(vm);
            }
            result[i] = totalMips > 1e-6 ? (sumEffective / totalMips) : 0.0;
        }
        Log.printlnConcat(readSpace.getNow(), ": [monitor_v14] computed demand-based utilization for ", hosts.size(), " hosts");
        return result;
    }

    @Override
    public String outputSemantic() {
        return "host-demand-based-utilization-sum-vm-effective-throughput-over-total-mips";
    }

    @Override
    public int outputGuid() {
        return 1200;
    }
}
