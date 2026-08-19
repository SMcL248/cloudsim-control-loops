package org.cloudbus.cloudsim.examples;// always include

// Import whats needed
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;
import java.util.List;

// Strategy: conservative idle-host power-down for energy conservation.
// Deliberately narrow: only ever targets an UNDERLOADED host that is already carrying zero
// guests, so the destructive power-down action can never strand a live VM or Cloudlet. Among
// eligible empty hosts, picks the one currently drawing the most power for maximum savings.
public class planner_v2 implements Planner<LoadState[], int[]> {

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {
        List<HostEntity> hosts = readSpace.getAllHosts();

        HostEntity bestCandidate = null;
        double highestDrain = -1.0;

        // Only ever target hosts flagged UNDERLOADED that are already carrying zero guests,
        // so powering them down cannot destroy any live VM or Cloudlet work.
        for (int i = 0; i < diagnosis.length && i < hosts.size(); i++) {
            if (diagnosis[i] != LoadState.UNDERLOADED) continue;
            HostEntity host = hosts.get(i);

            if (readSpace.isHostFailed(host)) continue;
            if (readSpace.isHostPermanentlyDead(host)) continue;
            if (readSpace.isHostPoweredDown(host)) continue;
            if (readSpace.isHostPoweringUp(host)) continue;

            List<GuestEntity> guests = readSpace.getVmListForHost(host);
            if (!guests.isEmpty()) continue;

            double drain = readSpace.getHostPower(host);
            if (drain > highestDrain) {
                highestDrain = drain;
                bestCandidate = host;
            }
        }

        if (bestCandidate == null) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v2] ", "no empty idle host available to power down");
            return new int[0];
        }

        Log.printlnConcat(readSpace.getNow(), ": [planner_v2] planning power-down of idle empty host ", readSpace.getId(bestCandidate));

        return new int[] { readSpace.getId(bestCandidate) };
    }

    @Override
    public String inputSemantic() {
        return "host-loadstate-idle-signal";
    }

    @Override
    public String outputSemantic() {
        return "requestHostPowerDown";
    }

    @Override
    public int inputGuid() {
        return 2200;
    }

    @Override
    public int outputGuid() {
        return 3010;
    }
}
