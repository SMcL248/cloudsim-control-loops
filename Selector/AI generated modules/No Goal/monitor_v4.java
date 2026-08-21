package org.cloudbus.cloudsim.examples;// always include

import java.util.List;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.HostEntity;

// Strategy: categorical availability/risk score per host, not a continuous
// resource metric at all. Encodes operational state (dead / failed /
// powering up / powered down / healthy) so downstream logic can react to
// *situational* risk rather than load.
public class monitor_v4 implements Monitor<double[]> {

    @Override
    public double[] observe(ReadSpace readSpace) {
        List<HostEntity> hosts = readSpace.getAllHosts();
        double[] result = new double[hosts.size()];

        for (int i = 0; i < hosts.size(); i++) {
            HostEntity host = hosts.get(i);

            if (readSpace.isHostPermanentlyDead(host)) {
                result[i] = 3.0;
            } else if (readSpace.isHostFailed(host)) {
                result[i] = 2.0;
            } else if (readSpace.isHostPoweringUp(host)) {
                result[i] = 1.0;
            } else if (readSpace.isHostPoweredDown(host)) {
                result[i] = 0.5;
            } else {
                result[i] = 0.0;
            }
        }

        Log.printlnConcat(readSpace.getNow(), ": [monitor_v4] host availability risk score computed for ", hosts.size(), " hosts");
        return result;
    }

    @Override
    public String outputSemantic() {
        return "host-availabilityRiskScore-categoricalFailureState";
    }

    @Override
    public int outputGuid() {
        return 1204;
    }
}
