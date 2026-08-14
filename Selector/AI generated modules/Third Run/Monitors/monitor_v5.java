package org.cloudbus.cloudsim.examples;

import java.util.List;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;

// VM-level CPU utilization trend delta: instantaneous utilization minus the running
// mean utilization. Positive values indicate load ramping up (throughput risk if
// sustained), negative values indicate load easing off (a power-saving opportunity).
public class monitor_v5 implements Monitor<double[]> {

    @Override
    public double[] observe(ReadSpace readSpace) {
        List<GuestEntity> vms = readSpace.getVmList();
        double[] result = new double[vms.size()];

        for (int i = 0; i < vms.size(); i++) {
            GuestEntity vm = vms.get(i);
            double instantaneous = readSpace.getVmCpuUtil(vm);
            double mean = readSpace.getVmUtilizationMean(vm);
            result[i] = instantaneous - mean;
        }

        Log.printlnConcat(readSpace.getNow(), ": [monitor_v5] ", "computed CPU utilization trend delta for ", vms.size(), " VMs");
        return result;
    }

    @Override
    public String outputSemantic() {
        return "vm-cpuUtilizationTrendDelta-instantaneous";
    }

    @Override
    public int outputGuid() {
        return 1300;
    }
}
