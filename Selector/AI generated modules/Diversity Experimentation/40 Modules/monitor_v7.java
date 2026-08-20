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

public class monitor_v7 implements Monitor<double[]> {

    @Override
    public double[] observe(ReadSpace readSpace) {
        java.util.List<HostEntity> hosts = readSpace.getAllHosts();
        double[] result = new double[hosts.size()];
        for (int i = 0; i < hosts.size(); i++) {
            result[i] = readSpace.hostHasFreePe(hosts.get(i)) ? 1.0 : 0.0;
        }
        Log.printlnConcat(readSpace.getNow(), ": [monitor_v7] computed free-PE availability flag for ", hosts.size(), " hosts");
        return result;
    }

    @Override
    public String outputSemantic() {
        return "host-free-pe-availability-binary-flag";
    }

    @Override
    public int outputGuid() {
        return 1200;
    }
}
