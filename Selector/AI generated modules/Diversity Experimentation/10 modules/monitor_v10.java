package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.core.GuestEntity;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Cloudlet-level monitor: PE share of hosting VM.
// For each active cloudlet, reports the fraction of its hosting VM's PE
// count that the cloudlet itself requests: cloudletPes / vmPes. This
// surfaces how resource-intensive a single cloudlet is relative to the VM
// it runs on, independent of that VM's absolute size. Uses the same
// cloudlet-id -> VM lookup approach as monitor_v9, since ownership is not
// otherwise obtainable from a bare Cloudlet reference.
public class monitor_v10 implements Monitor<double[]> {

    @Override
    public double[] observe(ReadSpace readSpace) {
        List<Cloudlet> cloudlets = readSpace.getActiveCloudlets();
        double[] result = new double[cloudlets.size()];

        Map<Integer, GuestEntity> cloudletOwner = new HashMap<Integer, GuestEntity>();
        for (GuestEntity vm : readSpace.getVmList()) {
            for (Cloudlet cl : readSpace.getVmCloudletList(vm)) {
                cloudletOwner.put(readSpace.getId(cl), vm);
            }
        }

        for (int i = 0; i < cloudlets.size(); i++) {
            Cloudlet cl = cloudlets.get(i);
            GuestEntity owner = cloudletOwner.get(readSpace.getId(cl));

            if (owner == null) {
                result[i] = -1.0;
                continue;
            }

            int vmPes = readSpace.getVmNumberOfPes(owner);
            if (vmPes <= 0) {
                result[i] = -1.0;
                continue;
            }

            int clPes = readSpace.getCloudletNumberOfPes(cl);
            result[i] = (double) clPes / (double) vmPes;
        }

        Log.printlnConcat(readSpace.getNow(), ": [monitor_v10] computed cloudlet pe share of hosting vm for ",
                cloudlets.size(), " cloudlets");

        return result;
    }

    @Override
    public String outputSemantic() {
        return "cloudlet-pe-share-of-hosting-vm-pe-count";
    }

    @Override
    public int outputGuid() {
        return 1400;
    }
}
