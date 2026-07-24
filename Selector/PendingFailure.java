package org.cloudbus.cloudsim.examples;

public class PendingFailure {

    double time;
    int hostId;

    PendingFailure(double time, int hostId) {
        this.time = time; 
        this.hostId = hostId; 
    }

    public double getTime(){return time;}
    public int getHostId(){return hostId;}

}