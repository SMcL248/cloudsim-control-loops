package org.cloudbus.cloudsim.examples;

import java.lang.reflect.ParameterizedType;
import java.util.function.Predicate;
import java.lang.reflect.Type;

import org.cloudbus.cloudsim.Log;

public class Controller<M,D,A> implements ControlUnit {

    private final String name;
    private final Monitor<M> monitor;
    private final Analyser<M,D> analyser;
    private final Planner<D,A> planner;
    private final Executor<A> executor;

    private final Predicate<D> imbalancePredicate;
    private final Predicate<D> opportunityPredicate;
    private final Predicate<A> actionProposedPredicate;

    private final Class<?> analyserExpectedClass;
    private final Class<?> plannerExpectedClass;
    private final Class<?> executorExpectedClass;

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
        this.analyserExpectedClass = resolveInputClass(analyser, Analyser.class);
        this.plannerExpectedClass  = resolveInputClass(planner, Planner.class);
        this.executorExpectedClass = resolveInputClass(executor, Executor.class);

    }

    @Override
    public void observeAndAct(ActionSpace actionSpace) {

        M metrics = monitor.observe(actionSpace);
        checkStructural("M-A", monitor.getClass().getSimpleName(), analyser.getClass().getSimpleName(), analyserExpectedClass, metrics);

        diagnosis = analyser.analyse(metrics, actionSpace);
        checkStructural("A-P", analyser.getClass().getSimpleName(), planner.getClass().getSimpleName(), plannerExpectedClass, diagnosis);

        eligible = imbalancePredicate.test(diagnosis);
        if (eligible) imbalanceCycles++;
        if (opportunityPredicate.test(diagnosis)) opportunityCycles++;

        A actions = planner.plan(diagnosis, actionSpace);
        checkStructural("P-E", planner.getClass().getSimpleName(), executor.getClass().getSimpleName(), executorExpectedClass, actions);
        if (actionProposedPredicate.test(actions)) actionsProposed++;

        boolean success = executor.execute(actions, actionSpace);
        if (success) actionsExecuted++;

    }

    @Override public String getName() { return name; }
    @Override public int getImbalanceCycles()   { return imbalanceCycles; }
    @Override public int getOpportunityCycles() { return opportunityCycles; }
    @Override public int getActionsProposed()   { return actionsProposed; }
    @Override public int getActionsExecuted()   { return actionsExecuted; }

    // Check that the expected input data type matches the data type of the input
    private static void checkStructural(String bridge, String sourceGuid, String targetGuid,
                                      Class<?> expected, Object payload) {
        if (!expected.equals(payload.getClass())) {
            throw new StructuralMismatchException(bridge, sourceGuid, targetGuid, expected, payload.getClass());
        }
    }

    // Retrieve expeceted input data type for the given module
    private static Class<?> resolveInputClass(Object module, Class<?> iface) {

        for (Type t : module.getClass().getGenericInterfaces()) {
            if (t instanceof ParameterizedType pt && pt.getRawType().equals(iface)) {
                return (Class<?>) pt.getActualTypeArguments()[0];
            }
        }
        throw new IllegalStateException("Cannot resolve generic input type for " + module.getClass());
    }

}