package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.core.GuestEntity;

import java.util.List;

// VM level - estimated time for this VM to clear its own assigned cloudlet backlog at current throughput.
public class monitor_v13 implements Monitor<double[]> {

    private static final int OUTPUT_GUID = 1300;

    @Override
    public double[] observe(ReadSpace readSpace) {
        List<GuestEntity> vms = readSpace.getVmList();
        double[] result = new double[vms.size()];

        for (int i = 0; i < vms.size(); i++) {
            GuestEntity vm = vms.get(i);

            long remainingSum = 0L;
            List<Cloudlet> cloudlets = readSpace.getVmCloudletList(vm);
            for (Cloudlet cl : cloudlets) {
                remainingSum += readSpace.getRemainingLength(cl);
            }

            if (remainingSum <= 0L) {
                result[i] = 0.0;
            } else {
                double throughput = readSpace.getVmEffectiveThroughput(vm);
                result[i] = (throughput > 0.0) ? (remainingSum / throughput) : Double.MAX_VALUE;
            }
        }

        Log.printlnConcat(readSpace.getNow(), ": [monitor_v13] computed vm-workload-backlog-time for ", vms.size(), " vms.");

        return result;
    }

    @Override
    public String outputSemantic() {
        return "vm-workload-backlog-time: estimated time for this VM to fully process all currently assigned "
                + "cloudlet remaining length at its current effective throughput. Returns Double.MAX_VALUE if a "
                + "backlog exists but effective throughput is currently zero (a stalled VM), or 0.0 if there is no "
                + "backlog. Index i corresponds to getVmList().get(i).";
    }

    @Override
    public int outputGuid() {
        return OUTPUT_GUID;
    }
}
