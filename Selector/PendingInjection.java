package org.cloudbus.cloudsim.examples;

import java.util.List;

import org.cloudbus.cloudsim.Cloudlet;


public class PendingInjection {

    private final double time;
    private final List<Cloudlet> batch;

    public PendingInjection(double time, List<Cloudlet> batch) {
        this.time = time;
        this.batch = batch;
    }

    public double getTime(){
        return time;
    }

    public List<Cloudlet> getBatch(){
        return batch;
    }

}
