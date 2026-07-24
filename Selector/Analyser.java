package org.cloudbus.cloudsim.examples;


public interface Analyser<M, D> {

    D analyse(M metrics, ReadSpace readSpace);
    String inputSemantic(); // e.g. {"etc"}
    String outputSemantic();
    int inputGuid();
    int outputGuid();
    
}
