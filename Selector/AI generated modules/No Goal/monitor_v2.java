package org.cloudbus.cloudsim.examples;// always include

import java.util.List;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.HostEntity;

// Strategy: power headroom ratio per host = current power draw over the
// host's max-util power draw. Power curves are non-linear w.r.t. CPU load,
// so this diverges from a plain utilisation ratio and surfaces hosts that
// are disproportionately expensive to keep running.
public class monitor_v2 implements Monitor<double[]> {

    @Override
    public double[] observe(ReadSpace readSpace) {
        List<HostEntity> hosts = readSpace.getAllHosts();
        double[] result = new double[hosts.size()];

        for (int i = 0; i < hosts.size(); i++) {
            HostEntity host = hosts.get(i);
            double maxPower = readSpace.getHostMaxPower(host);
            double currentPower = readSpace.getHostPower(host);

            if (maxPower <= 0.0) {
                result[i] = 0.0;
            } else {
                result[i] = currentPower / maxPower;
            }
        }

        Log.printlnConcat(readSpace.getNow(), ": [monitor_v2] host power headroom ratio computed for ", hosts.size(), " hosts");
        return result;
    }

    @Override
    public String outputSemantic() {
        return "host-powerHeadroomRatio-currentPowerOverMaxPower";
    }

    @Override
    public int outputGuid() {
        return 1202;
    }
}
