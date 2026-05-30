package org.example;

import ilog.concert.IloConstraint;
import ilog.concert.IloException;
import ilog.concert.IloIntExpr;
import ilog.concert.IloIntVar;
import ilog.cp.IloCP;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Seven benchmark configurations across two groups.
 *
 * <p>Paper: "RevLex Ordering and Upper Off-Diagonal Connectivity Constraints:
 * A Synergistic Approach for Connected Non-Isomorphic Graph Enumeration"
 *
 * <h2>Group A — Symmetry breaking (all graphs)</h2>
 * <ul>
 *   <li>{@link #testOptimizedLex}          – OptLex ordering (Codish 2018)</li>
 *   <li>{@link #testOptimizedRevLex}        – OptRevLex ordering (this paper)</li>
 * </ul>
 *
 * <h2>Group B — Connectivity (connected graphs only)</h2>
 * <ul>
 *   <li>{@link #testOptimizedLexCon}        – OptLex + path-based encoding</li>
 *   <li>{@link #testOptimizedRevLexCon}     – OptRevLex + path-based encoding</li>
 *   <li>{@link #testOptimizedRevLexConDiag} – OptRevLex + upper off-diagonal encoding
 *       (Theorem 2; no auxiliary variables; O(n) constraints)</li>
 * </ul>
 *
 * <h2>Hybrid switching rule ({@link #useRevLex(int, int)})</h2>
 * <pre>
 *   RevLex  when  2d &lt; n,  or  (2d == n AND d even)
 *   Lex     otherwise
 * </pre>
 * Equivalent to the original even/odd formulation without integer-division
 * truncation ambiguity.  On the current benchmark this selects OptRevLex for
 * every instance except K₆(3) (2d = n = 6, odd d → OptLex).
 *
 * <h2>Why no Hybrid + off-diagonal variant?</h2>
 * The upper off-diagonal encoding (Theorem 2) requires a full OptRevLex
 * canonical form.  When the hybrid selects OptLex the encoding is unsound —
 * K₆(3) returns 0 solutions empirically.  Off-diagonal connectivity is
 * therefore restricted to the pure OptRevLex configuration.
 * ordering direction and is used for the hybrid connectivity variant.
 */
public class OpLexVsOpRevLex {

    // =========================================================================
    //  SOLVER PARAMETER CONSTANTS
    // =========================================================================

    /** Shared solver configuration applied to every method. */
    private static void configureSolver(IloCP cp) throws IloException {
        cp.setParameter(IloCP.IntParam.LogVerbosity,         IloCP.ParameterValues.Quiet);
        cp.setParameter(IloCP.IntParam.SearchType,           IloCP.ParameterValues.DepthFirst);
        cp.setParameter(IloCP.IntParam.DefaultInferenceLevel, IloCP.ParameterValues.Low);
        cp.setParameter(IloCP.IntParam.MemoryDisplay,        0);
    }

    // =========================================================================
    //  HYBRID SWITCHING PREDICATE
    // =========================================================================

    /**
     * Returns {@code true} when the OptRevLex (reverse) ordering should be
     * used for a d-regular graph on n vertices, {@code false} for OptLex.
     *
     * <p>The condition is multiplication-based to avoid integer-division
     * truncation issues:
     * <pre>
     *   RevLex  iff  2d &lt; n  OR  (2d == n AND d is even)
     * </pre>
     *
     * <p>Equivalently (matches the original even/odd formulation exactly):
     * <pre>
     *   even d : RevLex if d &lt;= n/2
     *   odd  d : RevLex if d &lt;  n/2
     * </pre>
     *
     * <p>Boundary analysis:
     * <ul>
     *   <li>2d == n with <em>odd</em> d is impossible (2d even, n would be even
     *       ⇒ d = n/2 ⇒ d even — contradiction), so the even guard is only
     *       reachable for even d. The two branches therefore never disagree at
     *       the boundary, and the even/odd split in the original code is
     *       logically redundant (dead code for the boundary case).</li>
     * </ul>
     *
     * @param d degree (same for all vertices in a d-regular graph)
     * @param n number of vertices
     * @return {@code true} → use OptRevLex; {@code false} → use OptLex
     */
    static boolean useRevLex(int d, int n) {
        return (2 * d < n) || (2 * d == n && d % 2 == 0);
    }

    // =========================================================================
    //  BASE-MODEL HELPERS  (shared by all methods)
    // =========================================================================

    /**
     * Creates the n×n binary adjacency-matrix variables and posts the three
     * structural constraints common to every configuration:
     * <ol>
     *   <li>Zero diagonal (no self-loops)</li>
     *   <li>Degree regularity</li>
     *   <li>Symmetry (undirected graph)</li>
     * </ol>
     *
     * @param cp     the CP solver instance
     * @param DEGREE degree sequence (all equal to d for a d-regular graph)
     * @return the matrix of decision variables
     */
    private static IloIntVar[][] buildBaseModel(IloCP cp, int[] DEGREE)
            throws IloException {
        int N = DEGREE.length;
        IloIntVar[][] M = new IloIntVar[N][];
        for (int i = 0; i < N; i++) {
            M[i] = cp.intVarArray(N, 0, 1);
        }

        // 1. Zero diagonal
        for (int i = 0; i < N; i++) {
            cp.add(cp.eq(M[i][i], 0));
        }

        // 2. Degree regularity
        for (int i = 0; i < N; i++) {
            cp.addEq(cp.sum(M[i]), DEGREE[i]);
        }

        // 3. Symmetry
        for (int i = 0; i < N; i++) {
            for (int j = i + 1; j < N; j++) {
                cp.add(cp.eq(M[i][j], M[j][i]));
            }
        }

        return M;
    }

    /**
     * Posts the path-based (z-variable) connectivity encoding.
     *
     * <p>Adds n auxiliary distance variables z[i] ∈ [0, n-1], fixes z[0] = 0,
     * enforces z[i] ≥ 1 for i > 0, and posts the BFS-level implication
     * constraints together with the |z[i] − z[j]| ≤ 1 tightening for
     * adjacent pairs. Together these guarantee that every generated graph is
     * connected and that each connected graph has a unique z-assignment
     * (Theorem 3 / Corollary 1 of the paper).
     */
    private static void addPathConnectivity(IloCP cp, IloIntVar[][] M)
            throws IloException {
        int N = M.length;
        IloIntVar[] z = new IloIntVar[N];
        for (int i = 0; i < N; i++) {
            z[i] = cp.intVar(0, N - 1);
        }

        cp.addEq(z[0], 0);
        for (int i = 1; i < N; i++) {
            cp.addGe(z[i], 1);
        }

        for (int i = 1; i < N; i++) {
            for (int k = 1; k < N; k++) {
                IloConstraint[] neighborConstraints = new IloConstraint[N - 1];
                int idx = 0;
                for (int j = 0; j < N; j++) {
                    if (j != i) {
                        neighborConstraints[idx++] = cp.and(new IloConstraint[]{
                                cp.neq(M[i][j], 0),
                                cp.eq(z[j], k - 1)
                        });
                    }
                }
                cp.add(cp.ifThen(cp.eq(z[i], k), cp.or(neighborConstraints)));
            }
        }

        // Tighten z-redundancy: adjacent vertices differ by at most 1 level.
        for (int i = 0; i < N; i++) {
            for (int j = i + 1; j < N; j++) {
                cp.add(cp.ifThen(
                        cp.neq(M[i][j], 0),
                        cp.le(cp.abs(cp.diff(z[i], z[j])), 1)
                ));
            }
        }
    }

    /**
     * Posts the upper off-diagonal connectivity encoding (Theorem 2).
     *
     * <p>Adds n-1 constraints of the form sum(M[i][i+1..n-1]) ≥ 1, which
     * under OptRevLex ordering is provably equivalent to connectivity for
     * d-regular graphs. Requires no auxiliary variables.
     */
    private static void addOffDiagConnectivity(IloCP cp, IloIntVar[][] M)
            throws IloException {
        int N = M.length;
        for (int i = 0; i < N - 1; i++) {
            IloIntVar[] upper = Arrays.copyOfRange(M[i], i + 1, N);
            cp.add(cp.gt(cp.sum(upper), 0));
        }
    }

    // =========================================================================
    //  SYMMETRY-BREAKING HELPERS
    // =========================================================================

    /** Posts OptLex row-ordering constraints (optimized variant, Codish 2018). */
    private static void addOptLex(IloCP cp, IloIntVar[][] M, int[] DEGREE)
            throws IloException {
        int N = M.length;
        for (int i = 0; i < N - 1; i++) {
            for (int j = i + 1; j < N; j++) {
                if (DEGREE[i] == DEGREE[j]) {
                    cp.add(cp.lexicographic(
                            arrayNew(M[i], i, j),
                            arrayNew(M[j], i, j)));
                }
            }
        }
    }

    /** Posts OptRevLex row-ordering constraints (this paper). */
    private static void addOptRevLex(IloCP cp, IloIntVar[][] M, int[] DEGREE)
            throws IloException {
        int N = M.length;
        for (int i = 0; i < N - 1; i++) {
            for (int j = i + 1; j < N; j++) {
                if (DEGREE[i] == DEGREE[j]) {
                    cp.add(cp.lexicographic(
                            reverseArrayNew(M[i], i, j),
                            reverseArrayNew(M[j], i, j)));
                }
            }
        }
    }

    /**
     * Posts hybrid row-ordering constraints: selects OptRevLex or OptLex
     * per row pair according to {@link #useRevLex(int, int)}.
     *
     * <p>For d-regular instances all rows share the same degree, so a single
     * call to {@code useRevLex(d, n)} determines the ordering for the entire
     * matrix. The per-pair dispatch is kept to support future heterogeneous
     * degree sequences.
     */
  /*  private static void addHybrid(IloCP cp, IloIntVar[][] M, int[] DEGREE)
            throws IloException {
        int N = M.length;
        for (int i = 0; i < N - 1; i++) {
            for (int j = i + 1; j < N; j++) {
                if (DEGREE[i] != DEGREE[j]) continue;

                int d = DEGREE[i];
                IloIntExpr[] arrI, arrJ;

                if (useRevLex(d, N)) {
                    arrI = reverseArrayNew(M[i], i, j);
                    arrJ = reverseArrayNew(M[j], i, j);
                } else {
                    arrI = arrayNew(M[i], i, j);
                    arrJ = arrayNew(M[j], i, j);
                }
                cp.add(cp.lexicographic(arrI, arrJ));
            }
        }
    }
*/
    // =========================================================================
    //  SOLUTION COLLECTION
    // =========================================================================

    /**
     * Runs the solver to exhaustion and returns a {@link Result}.
     * Solutions are counted but not written to disk (no I/O overhead in
     * benchmark mode). Pass {@code writer != null} to record matrices.
     */
    private static Result collectResults(IloCP cp, IloIntVar[][] M,
                                         PrintWriter writer)
            throws IloException {
        int N = M.length;
        long start = System.currentTimeMillis();
        cp.startNewSearch();
        int count = 0;
        while (cp.next()) {
            count++;
            if (writer != null) {
                for (int i = 0; i < N; i++) {
                    for (int j = 0; j < N; j++) {
                        writer.print(" " + (int) cp.getValue(M[i][j]));
                    }
                    writer.println();
                }
                writer.println();
            }
        }
        cp.endSearch();
        long elapsed = System.currentTimeMillis() - start;

        return new Result(
                count, elapsed,
                cp.getInfo(IloCP.IntInfo.NumberOfFails),
                cp.getInfo(IloCP.IntInfo.NumberOfBranches),
                cp.getInfo(IloCP.IntInfo.NumberOfChoicePoints),
                cp.getInfo(IloCP.IntInfo.NumberOfConstraints));
    }

    // =========================================================================
    //  RevLex MODEL — DIAGONAL ENCODING DETAIL
    //
    //  The original OptRevLex methods use a non-standard "1 on the diagonal"
    //  trick: MATRIX[i][i] = 1 and the degree constraint is sum(row) = d+1.
    //  This is an encoding artefact; the produced graphs are identical to the
    //  zero-diagonal encoding after masking the diagonal.  The methods below
    //  preserve this behaviour exactly so that solution counts remain
    //  comparable with existing paper results.
    // =========================================================================

    /**
     * Builds the base model with the RevLex diagonal convention:
     * diagonal entries are fixed to 1 and the degree constraint is
     * {@code sum(row) = d + 1}.
     */
    private static IloIntVar[][] buildRevLexBaseModel(IloCP cp, int[] DEGREE)
            throws IloException {
        int N = DEGREE.length;
        IloIntVar[][] M = new IloIntVar[N][];
        for (int i = 0; i < N; i++) {
            M[i] = cp.intVarArray(N, 0, 1);
        }

        // Diagonal = 1
        for (int i = 0; i < N; i++) {
            cp.add(cp.eq(M[i][i], 1));
        }

        // Degree: sum(row) - diagonal = d  →  sum(row) = d + 1
        for (int i = 0; i < N; i++) {
            cp.addEq(cp.diff(cp.sum(M[i]), M[i][i]), DEGREE[i]);
        }

        // Symmetry
        for (int i = 0; i < N; i++) {
            for (int j = i + 1; j < N; j++) {
                cp.add(cp.eq(M[i][j], M[j][i]));
            }
        }
        return M;
    }

    // =========================================================================
    //  GROUP A — SYMMETRY-BREAKING CONFIGURATIONS  (all graphs)
    // =========================================================================

    /** All graphs — OptLex ordering (Codish 2018). */
    public static Result testOptimizedLex(int[] DEGREE) {
        try {
            IloCP cp = new IloCP();
            IloIntVar[][] M = buildBaseModel(cp, DEGREE);
            addOptLex(cp, M, DEGREE);
            configureSolver(cp);
            try (PrintWriter w = new PrintWriter(new FileWriter("output_testOptimizedLex.txt"))) {
                return collectResults(cp, M, w);
            }
        } catch (IloException | IOException e) { throw new RuntimeException(e); }
    }

    /** All graphs — OptRevLex ordering (this paper). */
    public static Result testOptimizedRevLex(int[] DEGREE) {
        try {
            IloCP cp = new IloCP();
            IloIntVar[][] M = buildRevLexBaseModel(cp, DEGREE);
            addOptRevLex(cp, M, DEGREE);
            configureSolver(cp);
            try (PrintWriter w = new PrintWriter(new FileWriter("output_testOptimizedRevLex.txt"))) {
                return collectResults(cp, M, w);
            }
        } catch (IloException | IOException e) { throw new RuntimeException(e); }
    }

    // =========================================================================
    //  GROUP B — CONNECTIVITY CONFIGURATIONS  (connected graphs)
    // =========================================================================

    /** Connected graphs — OptLex + path-based connectivity. */
    public static Result testOptimizedLexCon(int[] DEGREE) {
        try {
            IloCP cp = new IloCP();
            IloIntVar[][] M = buildBaseModel(cp, DEGREE);
            addOptLex(cp, M, DEGREE);
            addPathConnectivity(cp, M);
            configureSolver(cp);
            try (PrintWriter w = new PrintWriter(new FileWriter("output_testOptimizedLexCon.txt"))) {
                return collectResults(cp, M, w);
            }
        } catch (IloException | IOException e) { throw new RuntimeException(e); }
    }

    /** Connected graphs — OptRevLex + path-based connectivity. */
    public static Result testOptimizedRevLexCon(int[] DEGREE) {
        try {
            IloCP cp = new IloCP();
            IloIntVar[][] M = buildRevLexBaseModel(cp, DEGREE);
            addOptRevLex(cp, M, DEGREE);
            addPathConnectivity(cp, M);
            configureSolver(cp);
            try (PrintWriter w = new PrintWriter(new FileWriter("output_testOptimizedRevLexCon.txt"))) {
                return collectResults(cp, M, w);
            }
        } catch (IloException | IOException e) { throw new RuntimeException(e); }
    }

    /** Connected graphs — OptRevLex + upper off-diagonal encoding (Theorem 2).
     * No auxiliary variables; O(n) additional constraints.
     */
    public static Result testOptimizedRevLexConDiag(int[] DEGREE) {
        try {
            IloCP cp = new IloCP();
            IloIntVar[][] M = buildRevLexBaseModel(cp, DEGREE);
            addOptRevLex(cp, M, DEGREE);
            addOffDiagConnectivity(cp, M);
            configureSolver(cp);
            try (PrintWriter w = new PrintWriter(new FileWriter("output_testOptimizedRevLexConDiag.txt"))) {
                return collectResults(cp, M, null);
            }
        } catch (IloException | IOException e) { throw new RuntimeException(e); }
    }



    // =========================================================================
    //  LEGACY METHODS  (kept for backward-compatibility; not used in paper table)
    // =========================================================================

    /** Non-optimized Lex (baseline). */
    public static Result testLex(int[] DEGREE) {
        try {
            int N = DEGREE.length;
            IloCP cp = new IloCP();
            IloIntVar[][] M = buildBaseModel(cp, DEGREE);

            for (int i = 0; i < N - 1; i++) {
                for (int j = i + 1; j < N; j++) {
                    if (DEGREE[i] == DEGREE[j]) {
                        cp.add(cp.lexicographic(M[i], M[j]));
                    }
                }
            }

            configureSolver(cp);
            try (PrintWriter w = new PrintWriter(new FileWriter("output_testLex.txt"))) {
                return collectResults(cp, M, w);
            }
        } catch (IloException | IOException e) { throw new RuntimeException(e); }
    }

    /** Non-optimized RevLex (baseline). */
    public static Result testRevLex(int[] DEGREE) {
        try {
            IloCP cp = new IloCP();
            IloIntVar[][] M = buildRevLexBaseModel(cp, DEGREE);

            int N = DEGREE.length;
            for (int i = 0; i < N - 1; i++) {
                for (int j = i + 1; j < N; j++) {
                    if (DEGREE[i] == DEGREE[j]) {
                        cp.add(cp.lexicographic(
                                reverseArray(M[i]),
                                reverseArray(M[j])));
                    }
                }
            }

            configureSolver(cp);
            try (PrintWriter w = new PrintWriter(new FileWriter("output_testRevLex.txt"))) {
                return collectResults(cp, M, w);
            }
        } catch (IloException | IOException e) { throw new RuntimeException(e); }
    }

    // =========================================================================
    //  ARRAY UTILITIES
    // =========================================================================

    /**
     * Returns a reversed copy of {@code array} (all elements included).
     * Used only by the legacy non-optimized RevLex.
     */
    public static IloIntExpr[] reverseArray(IloIntExpr[] array) {
        int len = array.length;
        IloIntExpr[] rev = new IloIntExpr[len];
        for (int i = 0; i < len; i++) {
            rev[i] = array[len - 1 - i];
        }
        return rev;
    }

    /**
     * Returns the row array in <em>reverse</em> order with columns
     * {@code exclude1} and {@code exclude2} removed.
     *
     * <p>Used by OptRevLex and the hybrid (when RevLex is selected).
     */
    public static IloIntExpr[] reverseArrayNew(IloIntExpr[] array,
                                               int exclude1, int exclude2) {
        List<IloIntExpr> result = new ArrayList<>(array.length - 2);
        for (int i = array.length - 1; i >= 0; i--) {
            if (i != exclude1 && i != exclude2) {
                result.add(array[i]);
            }
        }
        return result.toArray(new IloIntExpr[0]);
    }

    /**
     * Returns the row array in <em>forward</em> order with columns
     * {@code exclude1} and {@code exclude2} removed.
     *
     * <p>Used by OptLex and the hybrid (when Lex is selected).
     */
    public static IloIntExpr[] arrayNew(IloIntExpr[] array,
                                        int exclude1, int exclude2) {
        if (array == null) throw new IllegalArgumentException("array must not be null");
        int len = array.length;
        if (exclude1 < 0 || exclude1 >= len || exclude2 < 0 || exclude2 >= len) {
            throw new IllegalArgumentException(
                    "Exclude indices must be in [0, " + len + ")");
        }
        int size = (exclude1 == exclude2) ? len - 1 : len - 2;
        IloIntExpr[] result = new IloIntExpr[size];
        int idx = 0;
        for (int i = 0; i < len; i++) {
            if (i != exclude1 && i != exclude2) {
                result[idx++] = array[i];
            }
        }
        return result;
    }
}