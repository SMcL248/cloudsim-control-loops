package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;
import java.util.*;

public class analyser_v40 implements Analyser<double[], LoadState[]> {

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        double now = readSpace.getNow();
        int n = metrics.length;
        LoadState[] result = new LoadState[n];
        if (n == 0) {
            return result;
        }

        List<GuestEntity> vms = readSpace.getVmList();
        List<HostEntity> hosts = readSpace.getAllHosts();
        Map<Integer, Integer> hostIndexForVmId = new HashMap<>();
        for (int h = 0; h < hosts.size(); h++) {
            HostEntity host = hosts.get(h);
            List<GuestEntity> hostedVms = readSpace.getVmListForHost(host);
            for (GuestEntity hv : hostedVms) {
                hostIndexForVmId.put(readSpace.getId(hv), h);
            }
        }
        double[] hostSum = new double[hosts.size()];
        int[] hostCount = new int[hosts.size()];
        for (int i = 0; i < n; i++) {
            int vmId = readSpace.getId(vms.get(i));
            Integer hIdx = hostIndexForVmId.get(vmId);
            if (hIdx == null) continue;
            hostSum[hIdx] += metrics[i];
            hostCount[hIdx]++;
        }
        int overCount = 0, underCount = 0;
        for (int i = 0; i < n; i++) {
            int vmId = readSpace.getId(vms.get(i));
            Integer hIdx = hostIndexForVmId.get(vmId);
            if (hIdx == null || hostCount[hIdx] <= 1) { result[i] = LoadState.BALANCED; continue; }
            double peerMean = (hostSum[hIdx] - metrics[i]) / (hostCount[hIdx] - 1);
            double ratio = (peerMean > 1e-9) ? metrics[i] / peerMean : 1.0;
            if (ratio > 1.5) { result[i] = LoadState.OVERLOADED; overCount++; }
            else if (ratio < 0.5) { result[i] = LoadState.UNDERLOADED; underCount++; }
            else result[i] = LoadState.BALANCED;
        }
        Log.printlnConcat(now, ": [analyser_v40] classified ", n, " vms via peer-group (co-located) relative comparison (over=", overCount, ", under=", underCount, ").");
        return result;
    }

    @Override
    public String inputSemantic() {
        return "vm-cpu-utilization-instant";
    }

    @Override
    public String outputSemantic() {
        return "vm-load-state-peer-group-relative";
    }

    @Override
    public int inputGuid() {
        return 1300;
    }

    @Override
    public int outputGuid() {
        return 2300;
    }
}
