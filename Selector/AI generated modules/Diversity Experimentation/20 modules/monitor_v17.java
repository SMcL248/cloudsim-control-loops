package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.core.GuestEntity;

import java.util.List;

// Cloudlet level - estimated wall-clock simulation time remaining until this cloudlet completes.
public class monitor_v17 implements Monitor<double[]> {

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
                result[i] = -1.0;
            } else {
                double finishTime = readSpace.getCloudletEstimatedFinishTime(owner, cl);
                result[i] = finishTime - readSpace.getNow();
            }
        }

        Log.printlnConcat(readSpace.getNow(), ": [monitor_v17] computed cloudlet-estimated-remaining-time for ", cloudlets.size(), " cloudlets.");

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
        return "cloudlet-estimated-remaining-time: estimated wall-clock simulation time remaining until this "
                + "cloudlet completes, computed as getCloudletEstimatedFinishTime(owningVm, cloudlet) minus the "
                + "current simulation time. The owning VM is resolved by matching the cloudlet's id against each "
                + "VM's assigned cloudlet list. Returns -1.0 if no owning VM can be resolved. Index i corresponds "
                + "to getActiveCloudlets().get(i).";
    }

    @Override
    public int outputGuid() {
        return OUTPUT_GUID;
    }
}
