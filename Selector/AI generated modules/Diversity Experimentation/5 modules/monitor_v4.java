package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;

import java.util.List;

/**
 * VM-level monitor.
 *
 * Approach: contention efficiency. Compares a VM's actual effective
 * throughput against its nominal share of host capacity (host MIPS-per-PE
 * multiplied by the VM's own PE count). Unlike a raw utilisation reading,
 * this is a relative signal: a value near 1.0 means the VM is getting
 * close to the throughput it is nominally entitled to, while a value well
 * below 1.0 means something on the host (co-tenant contention,
 * oversubscription) is suppressing the VM's real throughput even if the
 * VM's own utilisation counter looks unremarkable.
 */
public class monitor_v4 implements Monitor<double[]> {

    private static final double EPSILON = 1e-9;
    private static final String SEMANTIC = "vm-hostContentionEfficiency-throughputRatio";
    private static final int GUID = 1300;

    @Override
    public double[] observe(ReadSpace readSpace) {
        List<GuestEntity> vms = readSpace.getVmList();
        double[] result = new double[vms.size()];

        double sum = 0.0;
        for (int i = 0; i < vms.size(); i++) {
            GuestEntity vm = vms.get(i);

            double effectiveThroughput = readSpace.getVmEffectiveThroughput(vm);
            double hostCapacityPerPe = readSpace.getHostCapacity(vm);
            int numPes = readSpace.getVmNumberOfPes(vm);
            double nominalShare = hostCapacityPerPe * numPes;

            double efficiency = effectiveThroughput / (nominalShare + EPSILON);
            if (efficiency > 1.0) {
                efficiency = 1.0;
            } else if (efficiency < 0.0) {
                efficiency = 0.0;
            }

            result[i] = efficiency;
            sum += efficiency;
        }

        double mean = vms.isEmpty() ? 0.0 : sum / vms.size();
        String message = "vms=" + vms.size() + " meanHostContentionEfficiency=" + mean;
        Log.printlnConcat(readSpace.getNow(), ": [monitor_v4] ", message);

        return result;
    }

    @Override
    public String outputSemantic() {
        return SEMANTIC;
    }

    @Override
    public int outputGuid() {
        return GUID;
    }
}
