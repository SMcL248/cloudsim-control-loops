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

public class monitor_v33 implements Monitor<double[]> {

    @Override
    public double[] observe(ReadSpace readSpace) {
        java.util.List<Cloudlet> cloudlets = readSpace.getActiveCloudlets();
        double sumLength = 0.0;
        for (Cloudlet cl : cloudlets) {
            sumLength += readSpace.getTotalLength(cl);
        }
        double meanLength = cloudlets.size() > 0 ? (sumLength / cloudlets.size()) : 0.0;
        double[] result = new double[cloudlets.size()];
        for (int i = 0; i < cloudlets.size(); i++) {
            double total = readSpace.getTotalLength(cloudlets.get(i));
            result[i] = meanLength > 1e-6 ? (total / meanLength) : 0.0;
        }
        Log.printlnConcat(readSpace.getNow(), ": [monitor_v33] computed relative size against population mean for ", cloudlets.size(), " cloudlets");
        return result;
    }

    @Override
    public String outputSemantic() {
        return "cloudlet-relative-size-ratio-total-length-over-population-mean-total-length";
    }

    @Override
    public int outputGuid() {
        return 1400;
    }
}
