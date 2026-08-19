package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.HostEntity;
import java.util.List;

/**
 * Host-level monitor. Estimates the energy cost the datacenter would
 * incur if this host were suddenly pushed from its current load to full
 * utilisation within the next unit of simulation time. Uses the host's
 * current power draw relative to its max power draw as a proxy for its
 * present utilisation, then asks the energy model what a ramp to 100%
 * would cost. Hosts with a high value are the ones where an incoming
 * burst of work would be most energy-expensive right now, independent
 * of whether they currently have spare resource headroom.
 */
public class monitor_v3 implements Monitor<double[]> {

    private static final double RAMP_HORIZON = 1.0;

    @Override
    public double[] observe(ReadSpace readSpace) {
        List<HostEntity> hosts = readSpace.getAllHosts();
        double[] result = new double[hosts.size()];

        for (int i = 0; i < hosts.size(); i++) {
            HostEntity host = hosts.get(i);

            double maxPower = readSpace.getHostMaxPower(host);
            double currentPower = readSpace.getHostPower(host);
            double fromUtil = maxPower > 0 ? currentPower / maxPower : 0.0;

            result[i] = readSpace.getHostEnergyEstimate(host, fromUtil, 1.0, RAMP_HORIZON);
        }

        Log.printlnConcat(readSpace.getNow(), ": [monitor_v3] ", "computed power-spike exposure for ", hosts.size(), " hosts");
        return result;
    }

    @Override
    public String outputSemantic() {
        return "host-power-spike-exposure-ramp-to-max-energy-estimate";
    }

    @Override
    public int outputGuid() {
        return 1200;
    }
}
