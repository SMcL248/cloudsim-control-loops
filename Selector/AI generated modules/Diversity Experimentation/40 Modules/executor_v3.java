package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.core.GuestEntity;
import java.util.List;

public class executor_v3 implements Executor<int[]> {

    // Reject moves that would push the destination VM's estimated backlog
    // beyond this many seconds of processing time.
    private static final double MAX_BACKLOG_SECONDS = 60.0;

    @Override
    public boolean execute(int[] payload, ActionSpace actionSpace) {
        if (payload == null || payload.length != 3) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v3] ",
                    "malformed payload, expected {cloudletId, fromVmId, toVmId}");
            return false;
        }

        int cloudletId = payload[0];
        int fromVmId = payload[1];
        int toVmId = payload[2];

        Cloudlet cl = actionSpace.getCloudletById(cloudletId);
        GuestEntity toVm = actionSpace.getVmById(toVmId);

        if (cl == null || toVm == null) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v3] ",
                    "aborting move, unresolved cloudlet or destination VM");
            return false;
        }

        List<Cloudlet> existing = actionSpace.getVmCloudletList(toVm);
        long backlogMi = actionSpace.getRemainingLength(cl);
        if (existing != null) {
            for (Cloudlet other : existing) {
                backlogMi += actionSpace.getRemainingLength(other);
            }
        }

        double throughput = actionSpace.getVmEffectiveThroughput(toVm);
        double estimatedSeconds = throughput > 0 ? (backlogMi / throughput) : Double.MAX_VALUE;

        if (estimatedSeconds > MAX_BACKLOG_SECONDS) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v3] ",
                    "rejecting move to VM " + toVmId + ", estimated backlog " + estimatedSeconds
                            + "s exceeds cap of " + MAX_BACKLOG_SECONDS + "s");
            return false;
        }

        actionSpace.moveCloudlet(cloudletId, fromVmId, toVmId);

        Log.printlnConcat(actionSpace.getNow(), ": [executor_v3] ",
                "headroom check passed, issued moveCloudlet cloudlet=" + cloudletId + " to=" + toVmId
                        + " estimated backlog=" + estimatedSeconds + "s");
        return true;
    }

    @Override
    public String inputSemantic() {
        return "Move a cloudlet from one VM to another";
    }

    @Override
    public int inputGuid() {
        return 3001;
    }
}
