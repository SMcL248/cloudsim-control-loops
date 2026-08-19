package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.HostEntity;
import java.util.List;

// Host-level monitor: bottleneck headroom score.
// For each host, computes the fractional headroom remaining on each of
// MIPS, RAM, and Bandwidth, then reports the minimum of the three. This
// surfaces whichever resource dimension is closest to exhaustion on that
// host, rather than averaging dimensions together and hiding a tight one.
public class monitor_v4 implements Monitor<double[]> {

    @Override
    public double[] observe(ReadSpace readSpace) {
        List<HostEntity> hosts = readSpace.getAllHosts();
        double[] result = new double[hosts.size()];

        for (int i = 0; i < hosts.size(); i++) {
            HostEntity host = hosts.get(i);

            if (readSpace.isHostFailed(host) || readSpace.isHostPermanentlyDead(host)
                    || readSpace.isHostPoweredDown(host)) {
                result[i] = -1.0;
                continue;
            }

            double totalMips = readSpace.getHostTotalMips(host);
            double totalRam = readSpace.getHostTotalRam(host);
            double totalBw = readSpace.getHostTotalBw(host);

            if (totalMips <= 0.0 || totalRam <= 0.0 || totalBw <= 0.0) {
                result[i] = -1.0;
                continue;
            }

            double mipsFrac = readSpace.getHostAvailableMips(host) / totalMips;
            double ramFrac = readSpace.getHostAvailableRam(host) / totalRam;
            double bwFrac = readSpace.getHostAvailableBw(host) / totalBw;

            result[i] = Math.min(mipsFrac, Math.min(ramFrac, bwFrac));
        }

        Log.printlnConcat(readSpace.getNow(), ": [monitor_v4] computed host bottleneck headroom score for ",
                hosts.size(), " hosts");

        return result;
    }

    @Override
    public String outputSemantic() {
        return "host-bottleneck-headroom-min-frac-mips-ram-bw";
    }

    @Override
    public int outputGuid() {
        return 1200;
    }
}
