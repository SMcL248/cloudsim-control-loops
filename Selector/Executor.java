package org.cloudbus.cloudsim.examples;

public interface Executor<A> {
    boolean execute(A actions, ActionSpace actionSpace);
    String inputSemantic();
    int inputGuid();

    // Opt-in: cumulative count of actions this Executor fired via ActionSpace's
    // boolean-returning methods that actually succeeded. execute()'s own boolean
    // (fired-at-all) stays the authoritative signal for actionable-to-action
    // conversion rate -- this is a separate, finer, optional signal on top of it.
    // Defaults to 0 for Executors that don't track it.
    default int getSuccessfulActionCount() {
        return 0;
    }
    
}