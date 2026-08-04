package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import java.util.List;

// VM-level monitor: reports measured effective throughput per VM.
// Supports the throughput goal (maximise throughput / minimise makespan) directly,
// as a delivered-work signal rather than a resource-utilization proxy.
public class monitor_v7 implements Monitor<double[]> {

    @Override
    public double[] observe(ReadSpace readSpace) {
        List<GuestEntity> vms = readSpace.getVmList();
        double[] metrics = new double[vms.size()];

        for (int i = 0; i < vms.size(); i++) {
            GuestEntity vm = vms.get(i);
            metrics[i] = readSpace.getVmEffectiveThroughput(vm);
        }

        Log.printlnConcat(readSpace.getNow(), ": [monitor_v7] computed effective throughput for ", vms.size(), " VMs");
        return metrics;
    }

    @Override
    public String outputSemantic() {
        return "vm-effective-throughput-delivered-mips-per-vm";
    }

    @Override
    public int outputGuid() {
        return 1301;
    }

}
