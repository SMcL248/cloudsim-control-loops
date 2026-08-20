package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.core.GuestEntity;

import java.util.List;

// Cloudlet level - PEs this cloudlet requests as a fraction of its owning VM's total PEs.
public class monitor_v19 implements Monitor<double[]> {

    private static final int OUTPUT_GUID = 1400;

    @Override
    public double[] observe(ReadSpace readSpace) {
        List<Cloudlet> cloudlets = readSpace.getActiveCloudlets();
        List<GuestEntity> vms = readSpace.getVmList();
        double[] result = new double[cloudlets.size()];

        for (int i = 0; i < cloudlets.size(); i++) {
            Cloudlet cl = cloudlets.get(i);
            GuestEntity owner = findOwningVm(readSpace, vms, cl);

            if (owner == null) {
                result[i] = 0.0;
            } else {
                int cloudletPes = readSpace.getCloudletNumberOfPes(cl);
                int vmPes = readSpace.getVmNumberOfPes(owner);
                result[i] = (vmPes > 0) ? (cloudletPes / (double) vmPes) : 0.0;
            }
        }

        Log.printlnConcat(readSpace.getNow(), ": [monitor_v19] computed cloudlet-pe-footprint-fraction for ", cloudlets.size(), " cloudlets.");

        return result;
    }

    private GuestEntity findOwningVm(ReadSpace readSpace, List<GuestEntity> vms, Cloudlet cl) {
        int clId = readSpace.getId(cl);
        for (GuestEntity vm : vms) {
            for (Cloudlet candidate : readSpace.getVmCloudletList(vm)) {
                if (readSpace.getId(candidate) == clId) {
                    return vm;
                }
            }
        }
        return null;
    }

    @Override
    public String outputSemantic() {
        return "cloudlet-pe-footprint-fraction: number of PEs this cloudlet requests as a fraction of its owning "
                + "VM's total allocated PEs, indicating how much of the VM's parallel processing capacity a single "
                + "cloudlet consumes. The owning VM is resolved by matching the cloudlet's id against each VM's "
                + "assigned cloudlet list; value is 0.0 if the owning VM cannot be resolved. Index i corresponds "
                + "to getActiveCloudlets().get(i).";
    }

    @Override
    public int outputGuid() {
        return OUTPUT_GUID;
    }
}
