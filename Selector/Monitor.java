package org.cloudbus.cloudsim.examples;

public interface Monitor<M> {

    M observe(ReadSpace readSpace); 
    String outputSemantic(); // e.g. {"etc"}, or {"ram_util", "etc"}
    int outputGuid();

}