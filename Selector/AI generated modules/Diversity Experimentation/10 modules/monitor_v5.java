package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.core.GuestEntity;
import java.util.List;

/**
 * VM-level monitor. Sums the remaining length of every cloudlet queued
 * on a VM and divides by that VM's current effective throughput to
 * estimate how long it would take to drain the existing backlog if no
 * new work arrived and throughput stayed constant. This is a queueing
 * style "time-to-clear" signal, distinct from a raw utilisation reading:
 * a VM can be highly utilised with a short backlog, or lightly utilised
 * with a long one still queued behind it.
 * Sentinel: -1.0 means the VM has queued work but zero effective
 * throughput right now (fully stalled).
 */
public class monitor_v5 implements Monitor<double[]> {

    @Override
    public double[] observe(ReadSpace readSpace) {
        List<GuestEntity> vms = readSpace.getVmList();
        double[] result = new double[vms.size()];

        for (int i = 0; i < vms.size(); i++) {
            GuestEntity vm = vms.get(i);

            long remainingSum = 0L;
            List<Cloudlet> cloudlets = readSpace.getVmCloudletList(vm);
            if (cloudlets != null) {
                for (Cloudlet cl : cloudlets) {
                    remainingSum += readSpace.getRemainingLength(cl);
                }
            }

            double throughput = readSpace.getVmEffectiveThroughput(vm);

            if (remainingSum <= 0) {
                result[i] = 0.0;
            } else if (throughput <= 0) {
                result[i] = -1.0;
            } else {
                result[i] = remainingSum / throughput;
            }
        }

        Log.printlnConcat(readSpace.getNow(), ": [monitor_v5] ", "computed backlog drain time for ", vms.size(), " vms");
        return result;
    }

    @Override
    public String outputSemantic() {
        return "vm-cloudlet-backlog-drain-time-estimate";
    }

    @Override
    public int outputGuid() {
        return 1300;
    }
}
