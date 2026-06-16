package org.cloudbus.cloudsim.examples;

import java.util.List;

public interface Planner<D,A> {
    A plan(D diagnosis);
}