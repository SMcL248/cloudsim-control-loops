package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.core.GuestEntity;
import java.util.List;

public class executor_v13 implements Executor<int[]> {

    @Override
    public boolean execute(int[] payload, ActionSpace actionSpace) {
        if (payload == null || payload.length != 1) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v13] ",
                    "malformed payload, expected {vmId}");
            return false;
        }

        int vmId = payload[0];
        GuestEntity vm = actionSpace.getVmById(vmId);

        if (vm == null) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v13] ",
                    "unresolved VM reference for id " + vmId + ", cannot issue destruction");
            return false;
        }

        List<Cloudlet> atRisk = actionSpace.getVmCloudletList(vm);
        long lostMi = 0;
        int lostCount = 0;
        if (atRisk != null) {
            lostCount = atRisk.size();
            for (Cloudlet cl : atRisk) {
                lostMi += actionSpace.getRemainingLength(cl);
            }
        }

        Log.printlnConcat(actionSpace.getNow(), ": [executor_v13] ",
                "destroying VM " + vmId + " will strand " + lostCount + " cloudlet(s) totalling " + lostMi + " MI");

        actionSpace.requestVmDestruction(vm);

        Log.printlnConcat(actionSpace.getNow(), ": [executor_v13] ",
                "issued requestVmDestruction vm=" + vmId);
        return true;
    }

    @Override
    public String inputSemantic() {
        return "Destroy a VM";
    }

    @Override
    public int inputGuid() {
        return 3004;
    }
}
