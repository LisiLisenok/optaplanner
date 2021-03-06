/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.optaplanner.core.impl.solver;

import java.util.Iterator;
import java.util.List;

import org.optaplanner.core.api.domain.solution.PlanningSolution;
import org.optaplanner.core.api.solver.Solver;
import org.optaplanner.core.api.solver.event.SolverEventListener;
import org.optaplanner.core.impl.partitionedsearch.PartitionSolver;
import org.optaplanner.core.impl.phase.Phase;
import org.optaplanner.core.impl.phase.event.PhaseLifecycleListener;
import org.optaplanner.core.impl.phase.event.PhaseLifecycleSupport;
import org.optaplanner.core.impl.phase.scope.AbstractPhaseScope;
import org.optaplanner.core.impl.phase.scope.AbstractStepScope;
import org.optaplanner.core.impl.solver.event.SolverEventSupport;
import org.optaplanner.core.impl.solver.recaller.BestSolutionRecaller;
import org.optaplanner.core.impl.solver.scope.DefaultSolverScope;
import org.optaplanner.core.impl.solver.termination.Termination;

/**
 * Common code between {@link DefaultSolver} and child solvers (such as {@link PartitionSolver}.
 * <p>
 * Do not create a new child {@link Solver} to implement a new heuristic or metaheuristic,
 * just use a new {@link Phase} for that.
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 * @see Solver
 * @see DefaultSolver
 */
public abstract class AbstractSolver<Solution_> implements Solver<Solution_> {

    protected final SolverEventSupport<Solution_> solverEventSupport = new SolverEventSupport<>(this);
    protected final PhaseLifecycleSupport<Solution_> phaseLifecycleSupport = new PhaseLifecycleSupport<>();

    // Note that the DefaultSolver.basicPlumbingTermination is a component of this termination
    protected final Termination termination;
    protected final BestSolutionRecaller<Solution_> bestSolutionRecaller;
    protected final List<Phase<Solution_>> phaseList;

    // ************************************************************************
    // Constructors and simple getters/setters
    // ************************************************************************

    public AbstractSolver(Termination termination, BestSolutionRecaller<Solution_> bestSolutionRecaller,
            List<Phase<Solution_>> phaseList) {
        this.termination = termination;
        this.bestSolutionRecaller = bestSolutionRecaller;
        bestSolutionRecaller.setSolverEventSupport(solverEventSupport);
        this.phaseList = phaseList;
        for (Phase<Solution_> phase : phaseList) {
            phase.setSolverPhaseLifecycleSupport(phaseLifecycleSupport);
        }
    }

    // ************************************************************************
    // Lifecycle methods
    // ************************************************************************

    public void solvingStarted(DefaultSolverScope<Solution_> solverScope) {
        solverScope.setWorkingSolutionFromBestSolution();
        bestSolutionRecaller.solvingStarted(solverScope);
        phaseLifecycleSupport.fireSolvingStarted(solverScope);
        for (Phase<Solution_> phase : phaseList) {
            phase.solvingStarted(solverScope);
        }
    }

    protected void runPhases(DefaultSolverScope<Solution_> solverScope) {
        Iterator<Phase<Solution_>> it = phaseList.iterator();
        while (!termination.isSolverTerminated(solverScope) && it.hasNext()) {
            Phase<Solution_> phase = it.next();
            phase.solve(solverScope);
            if (it.hasNext()) {
                solverScope.setWorkingSolutionFromBestSolution();
            }
        }
        // TODO support doing round-robin of phases (only non-construction heuristics)
    }

    public void solvingEnded(DefaultSolverScope<Solution_> solverScope) {
        for (Phase<Solution_> phase : phaseList) {
            phase.solvingEnded(solverScope);
        }
        phaseLifecycleSupport.fireSolvingEnded(solverScope);
        bestSolutionRecaller.solvingEnded(solverScope);
    }

    // ************************************************************************
    // Event listeners
    // ************************************************************************

    @Override
    public void addEventListener(SolverEventListener<Solution_> eventListener) {
        solverEventSupport.addEventListener(eventListener);
    }

    @Override
    public void removeEventListener(SolverEventListener<Solution_> eventListener) {
        solverEventSupport.removeEventListener(eventListener);
    }

    /**
     * Add a {@link PhaseLifecycleListener} that is notified
     * of {@link PhaseLifecycleListener#solvingStarted(DefaultSolverScope)} solving} events
     * and also of the {@link PhaseLifecycleListener#phaseStarted(AbstractPhaseScope) phase}
     * and the {@link PhaseLifecycleListener#stepStarted(AbstractStepScope)} step} starting/ending events of all phases.
     * <p>
     * To get notified for only 1 phase, use {@link Phase#addPhaseLifecycleListener(PhaseLifecycleListener)} instead.
     * @param phaseLifecycleListener never null
     */
    public void addPhaseLifecycleListener(PhaseLifecycleListener<Solution_> phaseLifecycleListener) {
        phaseLifecycleSupport.addEventListener(phaseLifecycleListener);
    }

    /**
     * @param phaseLifecycleListener never null
     * @see #addPhaseLifecycleListener(PhaseLifecycleListener)
     */
    public void removePhaseLifecycleListener(PhaseLifecycleListener<Solution_> phaseLifecycleListener) {
        phaseLifecycleSupport.removeEventListener(phaseLifecycleListener);
    }

}
