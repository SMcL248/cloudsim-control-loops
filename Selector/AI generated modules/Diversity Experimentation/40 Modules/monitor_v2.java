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

public class monitor_v2 implements Monitor<double[]> {

    @Override
    public double[] observe(ReadSpace readSpace) {
        java.util.List<HostEntity> hosts = readSpace.getAllHosts();
        double[] result = new double[hosts.size()];
        for (int i = 0; i < hosts.size(); i++) {
            result[i] = readSpace.getHostPower(hosts.get(i));
        }
        Log.printlnConcat(readSpace.getNow(), ": [monitor_v2] sampled instantaneous power draw for ", hosts.size(), " hosts");
        return result;
    }

    @Override
    public String outputSemantic() {
        return "host-power-draw-instantaneous-watts";
    }

    @Override
    public int outputGuid() {
        return 1200;
    }
}
