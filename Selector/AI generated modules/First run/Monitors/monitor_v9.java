package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.Cloudlet;
import java.util.List;

public class monitor_v9 implements Monitor<double[]> {

    private static final String SEMANTIC = "vm-backlog-ratio";
    private static final int GUID = 1300;

    @Override
    public double[] observe(ReadSpace readSpace) {
        List<GuestEntity> vms = readSpace.getVmList();
        double[] result = new double[vms.size()];

        for (int i = 0; i < vms.size(); i++) {
            GuestEntity vm = vms.get(i);
            List<Cloudlet> cloudlets = readSpace.getVmCloudletList(vm);

            long remainingTotal = 0;
            long lengthTotal = 0;

            for (Cloudlet cl : cloudlets) {
                remainingTotal += readSpace.getRemainingLength(cl);
                lengthTotal += readSpace.getTotalLength(cl);
            }

            if (lengthTotal <= 0) {
                result[i] = 0.0;
            } else {
                result[i] = (double) remainingTotal / (double) lengthTotal;
            }
        }

        Log.printlnConcat(readSpace.getNow(), ": [monitor_v9] vm backlog ratio computed for ", vms.size(), " vms");
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
