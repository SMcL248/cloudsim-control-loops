package org.cloudbus.cloudsim.examples;

public interface Planner<D,A> {
    A plan(D diagnosis, ReadSpace readSpace);
    String inputGuid();
    String outputGuid();
}