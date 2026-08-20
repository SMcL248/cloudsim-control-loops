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

public class monitor_v11 implements Monitor<double[]> {

    @Override
    public double[] observe(ReadSpace readSpace) {
        java.util.List<HostEntity> hosts = readSpace.getAllHosts();
        double[] result = new double[hosts.size()];
        for (int i = 0; i < hosts.size(); i++) {
            HostEntity host = hosts.get(i);
            double totalMips = readSpace.getHostTotalMips(host);
            double availableMips = readSpace.getHostAvailableMips(host);
            double currentUtil = totalMips > 1e-6 ? (1.0 - (availableMips / totalMips)) : 0.0;
            double bumpedUtil = Math.min(1.0, currentUtil + 0.1);
            result[i] = readSpace.getHostEnergyEstimate(host, currentUtil, bumpedUtil, 1.0);
        }
        Log.printlnConcat(readSpace.getNow(), ": [monitor_v11] estimated marginal energy cost for ", hosts.size(), " hosts");
        return result;
    }

    @Override
    public String outputSemantic() {
        return "host-marginal-energy-cost-estimate-10pct-util-bump-unit-time";
    }

    @Override
    public int outputGuid() {
        return 1200;
    }
}
