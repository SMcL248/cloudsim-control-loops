package org.cloudbus.cloudsim.examples;

import java.util.function.Predicate;

public class Controller<M,D,A> implements ControlUnit {

    private final String name;
    private final Monitor<M> monitor;
    private final Analyser<M,D> analyser;
    private final Planner<D,A> planner;
    private final Executor<A> executor;

    private final Predicate<D> imbalancePredicate;
    private final Predicate<D> opportunityPredicate;
    private final Predicate<A> actionProposedPredicate;

    private D diagnosis;
    private boolean eligible;

    private int imbalanceCycles = 0;
    private int opportunityCycles = 0;
    private int actionsProposed = 0;
    private int actionsExecuted = 0;

    public Controller(String name, Monitor<M> monitor, Analyser<M,D> analyser,
                       Planner<D,A> planner, Executor<A> executor,
                       Predicate<D> imbalancePredicate, Predicate<D> opportunityPredicate,
                       Predicate<A> actionProposedPredicate) {
        this.name = name;
        this.monitor = monitor;
        this.analyser = analyser;
        this.planner = planner;
        this.executor = executor;
        this.imbalancePredicate = (imbalancePredicate != null) ? imbalancePredicate : d -> false;
        this.opportunityPredicate = (opportunityPredicate != null) ? opportunityPredicate : d -> false;
        this.actionProposedPredicate = (actionProposedPredicate != null) ? actionProposedPredicate : a -> false;
    }

    @Override
    public void observeAndAct(ActionSpace actionSpace) {

        M metrics = monitor.observe(actionSpace);// Shall only access ReadSpace
        diagnosis = analyser.analyse(metrics, actionSpace);// Shall only access ReadSpace

        eligible = imbalancePredicate.test(diagnosis);
        if (eligible) imbalanceCycles++;
        if (opportunityPredicate.test(diagnosis)) opportunityCycles++;

        A actions = planner.plan(diagnosis, actionSpace);// Shall only access ReadSpace
        if (actionProposedPredicate.test(actions)) actionsProposed++;

        boolean success = executor.execute(actions, actionSpace);// Access to both read and action space
        if (success) actionsExecuted++;

    }

    @Override public String getName() { return name; }
    @Override public int getImbalanceCycles()   { return imbalanceCycles; }
    @Override public int getOpportunityCycles() { return opportunityCycles; }
    @Override public int getActionsProposed()   { return actionsProposed; }
    @Override public int getActionsExecuted()   { return actionsExecuted; }

}