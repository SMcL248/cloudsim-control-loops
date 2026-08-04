package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import java.util.List;

// VM-level monitor: reports current CPU utilization fraction per VM.
// Supports the throughput goal - sustained low utilization signals spare processing
// headroom, while sustained high utilization signals a throughput bottleneck.
public class monitor_v6 implements Monitor<double[]> {

    @Override
    public double[] observe(ReadSpace readSpace) {
        List<GuestEntity> vms = readSpace.getVmList();
        double[] metrics = new double[vms.size()];

        for (int i = 0; i < vms.size(); i++) {
            GuestEntity vm = vms.get(i);
            metrics[i] = readSpace.getVmCpuUtil(vm);
        }

        Log.printlnConcat(readSpace.getNow(), ": [monitor_v6] computed CPU utilization for ", vms.size(), " VMs");
        return metrics;
    }

    @Override
    public String outputSemantic() {
        return "vm-cpu-utilization-fraction-current-per-vm";
    }

    @Override
    public int outputGuid() {
        return 1300;
    }

}
