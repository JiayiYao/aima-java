package aima.core.search.csp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import aima.core.util.datastructure.Pair;

public class ImprovedBacktrackingStrategy extends BacktrackingStrategy {
	protected Selection selectionStrategy = Selection.DEFAULT_ORDER;
	protected Inference inferenceStrategy = Inference.NONE;
	protected boolean isLCVHeuristicEnabled;

	public void setVariableSelection(Selection sStrategy) {
		selectionStrategy = sStrategy;
	}

	public void setInference(Inference iStrategy) {
		inferenceStrategy = iStrategy;
	}

	public void enableLCV(boolean state) {
		isLCVHeuristicEnabled = state;
	}

	/**
	 * Primitive operation, selecting a not yet assigned variable. This default
	 * implementation just selects the first in the ordered list of variables
	 * provided by the CSP.
	 */
	@Override
	protected Variable selectUnassignedVariable(Assignment assignment, CSP csp) {
		switch (selectionStrategy) {
		case MRV:
			return applyMRVHeuristic(csp, assignment).get(0);
		case MRV_DEG:
			List<Variable> vars = applyMRVHeuristic(csp, assignment);
			return applyDegreeHeuristic(vars, assignment, csp).get(0);
		default:
			for (Variable var : csp.getVariables()) {
				if (!(assignment.hasAssignmentFor(var)))
					return var;
			}
		}
		return null;
	}

	/**
	 * Primitive operation, ordering the domain values of the specified
	 * variable. This default implementation just takes the default order
	 * provided by the CSP.
	 */
	@Override
	protected Iterable<?> orderDomainValues(Variable var,
			Assignment assignment, CSP csp) {
		if (!isLCVHeuristicEnabled) {
			return csp.getDomain(var);
		} else {
			return applyLeastConstrainingValueHeuristic(var, csp);
		}
	}

	/**
	 * Primitive operation, which tries to prune out values from the CSP which
	 * are not possible anymore when extending the given assignment to a
	 * solution. This default implementation just returns the original CSP.
	 * 
	 * @return A reduced copy of the original CSP or null denoting failure
	 *         (assignment cannot be extended to a solution).
	 */
	@Override
	protected DomainRestoreInfo inference(Variable var, Assignment assignment, CSP csp) {
		switch (inferenceStrategy) {
		case FORWARD_CHECKING:
			return doForwardChecking(var, assignment, csp);
		case AC3:
			return new AC3Strategy().reduceDomains(var, assignment
					.getAssignment(var), csp);
		default:
			return new DomainRestoreInfo().compactify();
		}
	}

	// //////////////////////////////////////////////////////////////
	// heuristics for selecting the next unassigned variable and domain ordering

	/** Implements the minimum-remaining-values heuristic. */
	private List<Variable> applyMRVHeuristic(CSP csp, Assignment assignment) {
		List<Variable> result = new ArrayList<Variable>();
		int mrv = Integer.MAX_VALUE;
		for (Variable var : csp.getVariables()) {
			if (!assignment.hasAssignmentFor(var)) {
				int num = csp.getDomain(var).size();
				if (num <= mrv) {
					if (num < mrv) {
						result.clear();
						mrv = num;
					}
					result.add(var);
				}
			}
		}
		return result;
	}

	/** Implements the degree heuristic. */
	private List<Variable> applyDegreeHeuristic(List<Variable> vars,
			Assignment assignment, CSP csp) {
		List<Variable> result = new ArrayList<Variable>();
		int maxDegree = Integer.MIN_VALUE;
		for (Variable var : vars) {
			int degree = 0;
			for (Constraint constraint : csp.getConstraints(var)) {
				Variable neighbor = csp.getNeighbor(var, constraint);
				if (!assignment.hasAssignmentFor(neighbor)
						&& csp.getDomain(neighbor).size() > 1)
					++degree;
			}
			if (degree >= maxDegree) {
				if (degree > maxDegree) {
					result.clear();
					maxDegree = degree;
				}
				result.add(var);
			}
		}
		return result;
	}

	private List<Object> applyLeastConstrainingValueHeuristic(Variable var, CSP csp) {
		List<Pair<Object, Integer>> pairs = new ArrayList<Pair<Object, Integer>>();
		for (Object value : csp.getDomain(var)) {
			int num = countLostValues(var, value, csp);
			pairs.add(new Pair<Object, Integer>(value, num));
		}
		Collections.sort(pairs, new Comparator<Pair<Object, Integer>>() {
			@Override
			public int compare(Pair<Object, Integer> o1,
					Pair<Object, Integer> o2) {
				return o1.getSecond() < o2.getSecond() ? -1 :
					o1.getSecond() > o2.getSecond() ? 1 : 0;
			}
		});
		List<Object> result = new ArrayList<Object>();
		for (Pair<Object, Integer> pair : pairs)
			result.add(pair.getFirst());
		return result;
	}

	private int countLostValues(Variable var, Object value, CSP csp) {
		int result = 0;
		Assignment assignment = new Assignment();
		assignment.setAssignment(var, value);
		for (Constraint constraint : csp.getConstraints(var)) {
			Variable neighbor = csp.getNeighbor(var, constraint);
			for (Object nValue : csp.getDomain(neighbor)) {
				assignment.setAssignment(neighbor, nValue);
				if (!constraint.isSatisfiedWith(assignment)) {
					++result;
				}
			}
		}
		return result;
	}

	
	// //////////////////////////////////////////////////////////////
	// inference algorithms

	private DomainRestoreInfo doForwardChecking(Variable var, Assignment assignment, CSP csp) {
		DomainRestoreInfo result = new DomainRestoreInfo();
		for (Constraint constraint : csp.getConstraints(var)) {
			List<Variable> scope = constraint.getScope();
			if (scope.size() == 2) {
				for (Variable neighbor : constraint.getScope()) {
					if (!assignment.hasAssignmentFor(neighbor)) {
						if (revise(neighbor, constraint, assignment, csp, result)) {
							if (csp.getDomain(neighbor).isEmpty()) {
								result.setEmptyDomainFound(true);
								return result;
							}
						}
					}
				}
			}
		}
		return result;
	}

	private boolean revise(Variable var, Constraint constraint,
			Assignment assignment, CSP csp, DomainRestoreInfo info) {
		
		boolean revised = false;
		for (Object value : csp.getDomain(var)) {
			assignment.setAssignment(var, value);
			if (!constraint.isSatisfiedWith(assignment)) {
				info.storeDomainFor(var, csp.getDomain(var));
				csp.removeValueFromDomain(var, value);
				revised = true;
			}
			assignment.removeAssignment(var);
		}
		return revised;
	}

	// //////////////////////////////////////////////////////////////
	// two enumerations

	public enum Selection {
		DEFAULT_ORDER, MRV, MRV_DEG
	}

	public enum Inference {
		NONE, FORWARD_CHECKING, AC3
	}
}
