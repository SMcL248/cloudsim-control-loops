package org.cloudbus.cloudsim.examples;// always include

// Import whats needed
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;
import org.cloudbus.cloudsim.core.PowerGuestEntity;
import org.cloudbus.cloudsim.core.PowerHostEntity;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.power.PowerDatacenter;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.power.PowerVm;


// Strategy: Direct Creation. Decodes {tierIndex, sizeTierIndex, datacenterId} and
// requests vm creation unconditionally, with no bounds validation on the indices.
public class executor_v4 implements Executor<int[]> {

    @Override
    public boolean execute(int[] actions, ActionSpace actionSpace) {
        String tag = "executor_v4";

        if (actions == null || actions.length != 3) {
            Log.printlnConcat(actionSpace.getNow(), ": [" + tag + "] rejected malformed payload, expected 3 ints, got ",
                    (actions == null ? "null" : actions.length));
            return false;
        }

        int tierIndex = actions[0];
        int sizeTierIndex = actions[1];
        int datacenterId = actions[2];

        GuestEntity created = actionSpace.requestVmCreation(tierIndex, sizeTierIndex, datacenterId);
        Log.printlnConcat(actionSpace.getNow(), ": [" + tag + "] attempted creation of vm at tier ", tierIndex,
                ", size tier ", sizeTierIndex, ", in datacenter ", datacenterId,
                ", result is ", (created == null ? "null" : "non-null"));
        return true;
    }

    @Override
    public String inputSemantic() {
        return "create new vm at given tier and size tier in given datacenter";
    }

    @Override
    public int inputGuid() {
        return 3003;
    }
}
