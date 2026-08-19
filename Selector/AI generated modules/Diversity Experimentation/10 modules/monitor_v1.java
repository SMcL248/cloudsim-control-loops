package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;
import java.util.List;

// Host-level monitor: aggregate CPU utilisation fraction.
// For each host, sums the effective throughput of every VM currently hosted
// there and divides by the host's total MIPS capacity. This gives a live
// demand-over-capacity load signal per host.
public class monitor_v1 implements Monitor<double[]> {

    @Override
    public double[] observe(ReadSpace readSpace) {
        List<HostEntity> hosts = readSpace.getAllHosts();
        double[] result = new double[hosts.size()];

        for (int i = 0; i < hosts.size(); i++) {
            HostEntity host = hosts.get(i);

            // Hosts that are failed, permanently dead, or powered down are not
            // meaningfully "utilised" in the demand-over-capacity sense.
            if (readSpace.isHostFailed(host) || readSpace.isHostPermanentlyDead(host)
                    || readSpace.isHostPoweredDown(host)) {
                result[i] = -1.0;
                continue;
            }

            double totalMips = readSpace.getHostTotalMips(host);
            if (totalMips <= 0.0) {
                result[i] = -1.0;
                continue;
            }

            double usedMips = 0.0;
            List<GuestEntity> vmsOnHost = readSpace.getVmListForHost(host);
            for (GuestEntity vm : vmsOnHost) {
                usedMips += readSpace.getVmEffectiveThroughput(vm);
            }

            result[i] = usedMips / totalMips;
        }

        Log.printlnConcat(readSpace.getNow(), ": [monitor_v1] computed host cpu utilisation fraction for ",
                hosts.size(), " hosts");

        return result;
    }

    @Override
    public String outputSemantic() {
        return "host-cpu-util-weighted-fraction-effective-throughput-over-total-mips";
    }

    @Override
    public int outputGuid() {
        return 1200;
    }
}
