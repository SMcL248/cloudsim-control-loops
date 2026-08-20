package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;
import java.util.List;

public class executor_v37 implements Executor<int[]> {

    @Override
    public boolean execute(int[] payload, ActionSpace actionSpace) {
        if (payload == null || payload.length != 1) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v37] ",
                    "malformed payload, expected {hostId}");
            return false;
        }

        int hostId = payload[0];
        HostEntity host = actionSpace.getHostById(hostId);
        if (host == null) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v37] ",
                    "unresolved host reference for id " + hostId);
            return false;
        }

        List<GuestEntity> hostedVms = actionSpace.getVmListForHost(host);
        int vmCount = hostedVms == null ? 0 : hostedVms.size();
        long atRiskMi = 0;
        if (hostedVms != null) {
            for (GuestEntity vm : hostedVms) {
                List<Cloudlet> cloudlets = actionSpace.getVmCloudletList(vm);
                if (cloudlets != null) {
                    for (Cloudlet cl : cloudlets) {
                        atRiskMi += actionSpace.getRemainingLength(cl);
                    }
                }
            }
        }

        Log.printlnConcat(actionSpace.getNow(), ": [executor_v37] ",
                "powering down host " + hostId + " will evacuate/destroy " + vmCount
                        + " VM(s) carrying approximately " + atRiskMi + " MI of at-risk work");

        actionSpace.requestHostPowerDown(host);

        Log.printlnConcat(actionSpace.getNow(), ": [executor_v37] ",
                "issued requestHostPowerDown host=" + hostId);
        return true;
    }

    @Override
    public String inputSemantic() {
        return "Power down a host";
    }

    @Override
    public int inputGuid() {
        return 3010;
    }
}
