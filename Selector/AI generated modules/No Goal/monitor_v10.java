package org.cloudbus.cloudsim.examples;// always include

import java.util.List;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Log;

// Strategy: PE density per cloudlet = remaining length divided by the
// number of PEs the cloudlet requests. A structural sizing metric rather
// than a temporal one -- flags cloudlets whose remaining work is
// concentrated onto few PEs and therefore likely to bottleneck.
public class monitor_v10 implements Monitor<double[]> {

    @Override
    public double[] observe(ReadSpace readSpace) {
        List<Cloudlet> cloudlets = readSpace.getActiveCloudlets();
        double[] result = new double[cloudlets.size()];

        for (int i = 0; i < cloudlets.size(); i++) {
            Cloudlet cl = cloudlets.get(i);
            long remaining = readSpace.getRemainingLength(cl);
            int pes = readSpace.getCloudletNumberOfPes(cl);

            if (pes <= 0) {
                result[i] = 0.0;
            } else {
                result[i] = (double) remaining / (double) pes;
            }
        }

        Log.printlnConcat(readSpace.getNow(), ": [monitor_v10] cloudlet PE density computed for ", cloudlets.size(), " cloudlets");
        return result;
    }

    @Override
    public String outputSemantic() {
        return "cloudlet-peDensity-remainingLengthPerRequestedPe";
    }

    @Override
    public int outputGuid() {
        return 1403;
    }
}
