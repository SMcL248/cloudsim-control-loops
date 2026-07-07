package org.cloudbus.cloudsim.examples;

import java.util.List;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.HostEntity;

public class Analyser9 implements Analyser<double[], LoadState[]>{

    private static final double FULL = 1.0;
    private static final double OFF = 0.0;

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {

        double now = readSpace.getNow();

        LoadState[] classification = new LoadState[metrics.length];

        List<HostEntity> hosts = readSpace.getAllHosts();

        int i = 0;
        // Log.enable();
        // Iterate by Host
        for (HostEntity host : hosts) {

            // Detemine load level
            if (metrics[i] == FULL) {
                classification[i] = LoadState.OVERLOADED;
                Log.printlnConcat(now, ": Host #", host.getId(), " is at full capacity.");
            } else if (metrics[i] == OFF) {
                classification[i] = LoadState.UNDERLOADED;
                Log.printlnConcat(now, ": Host #", host.getId(), " is empty (OFF).");
            } else {
                classification[i] = LoadState.BALANCED;
                Log.printlnConcat(now, ": Host #", host.getId(), " is neither FULL or OFF.");
            }

            i++;
        }
        // Log.disable();

        return classification;

    }

    @Override
    public String inputGuid() {
        return "host-demand";
    }

    @Override
    public String outputGuid() {
        return "host-demand-loadstate";
    }

}