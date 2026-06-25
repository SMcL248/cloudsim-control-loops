package org.cloudbus.cloudsim.examples;


public interface Analyser<M, D> {

    D analyse(M metrics, ReadSpace readSpace);
    int getActionableCycles();
    String inputGuid(); // e.g. {"etc"}
    String outputGuid();
    
}
