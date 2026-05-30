package org.example;

/**
 * Benchmark instances used in the paper:
 *
 * <p>"RevLex Ordering and Upper Off-Diagonal Connectivity Constraints:
 * A Synergistic Approach for Connected Non-Isomorphic Graph Enumeration"
 *
 * <p>All instances are d-regular graphs K_n(d) (every vertex has degree d).
 * The benchmark is restricted to d &lt; n/2; graph complementation gives a
 * one-to-one correspondence between K_n(d) and K_n(n−1−d), so the regime
 * d ≥ n/2 requires no separate treatment.
 *
 * <p>Naming convention: K_n(d) → {@code "Kn_d"}, e.g. K_8(3) → {@code "K8_3"}.
 *
 * <p>The instances match exactly the rows of Table 1 in the paper, grouped
 * by degree family.
 */
public class PaperBenchmarkData {

    /** All benchmark instances, in paper-table order. */
    public static final TestCase[] GRAPH_SAMPLES = {

            // =====================================================================
            // d = 2  —  2-regular graphs (disjoint unions of cycles)
            // =====================================================================
            new TestCase("K5_2",
                    new int[]{2, 2, 2, 2, 2},
                    "K_5(2): 2-regular on 5 vertices"),

            new TestCase("K6_2",
                    new int[]{2, 2, 2, 2, 2, 2},
                    "K_6(2): 2-regular on 6 vertices"),

            new TestCase("K7_2",
                    new int[]{2, 2, 2, 2, 2, 2, 2 },
                    "K_7(2): 2-regular on 7 vertices"),

            new TestCase("K8_2",
                    new int[]{2, 2, 2, 2, 2, 2, 2, 2},
                    "K_8(2): 2-regular on 8 vertices"),

            new TestCase("K9_2",
                    new int[]{2, 2, 2, 2, 2, 2, 2, 2, 2},
                    "K_9(2): 2-regular on 9 vertices"),

            new TestCase("K10_2",
                    new int[]{2, 2, 2, 2, 2, 2, 2, 2, 2, 2},
                    "K_10(2): 2-regular on 10 vertices"),

            new TestCase("K11_2",
                    new int[]{2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2},
                    "K_11(2): 2-regular on 11 vertices"),

            new TestCase("K12_2",
                    new int[]{2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2},
                    "K_12(2): 2-regular on 12 vertices"),

            new TestCase("K13_2",
                    new int[]{2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2},
                    "K_13(2): 2-regular on 13 vertices"),

            new TestCase("K14_2",
                    new int[]{2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2},
                    "K_14(2): 2-regular on 14 vertices"),

            new TestCase("K15_2",
                    new int[]{2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2},
                    "K_15(2): 2-regular on 15 vertices"),

            new TestCase("K16_2",
                    new int[]{2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2},
                    "K_16(2): 2-regular on 16 vertices"),
            new TestCase("K17_2",
                    new int[]{2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2 },
                    "K_17(2): 2-regular on 17 vertices"),

            new TestCase("K18_2",
                    new int[]{2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2},
                    "K_18(2): 2-regular on 18 vertices"),

            new TestCase("K19_2",
                    new int[]{2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2},
                    "K_19(2): 2-regular on 19 vertices"),

            new TestCase("K20_2",
                    new int[]{2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2},
                    "K_20 (2): 2-regular on 20 vertices"),

            // =====================================================================
            // d = 3  —  cubic (3-regular) graphs
            // Note: 3-regular requires n even (n·d must be even).
            // =====================================================================
            new TestCase("K4_3",
                    new int[]{ 3, 3, 3, 3},
                    "K_4(3): 3-regular on 4 vertices  [hybrid boundary: 2d==n, d odd → Lex]"),

            new TestCase("K6_3",
                    new int[]{3, 3, 3, 3, 3, 3},
                    "K_6(3): 3-regular on 6 vertices  [hybrid boundary: 2d==n, d odd → Lex]"),

            new TestCase("K8_3",
                    new int[]{3, 3, 3, 3, 3, 3, 3, 3},
                    "K_8(3): 3-regular on 8 vertices"),

            new TestCase("K10_3",
                    new int[]{3, 3, 3, 3, 3, 3, 3, 3, 3, 3},
                    "K_10(3): 3-regular on 10 vertices (Petersen family)"),

            new TestCase("K12_3",
                    new int[]{3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3},
                    "K_12(3): 3-regular on 12 vertices"),

            new TestCase("K14_3",
                    new int[]{3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3},
                    "K_14(3): 3-regular on 14 vertices (timeout expected for some configs)"),

            // =====================================================================
            // d = 4  —  4-regular graphs
            // =====================================================================
            new TestCase("K6_4",
                    new int[]{4, 4, 4, 4, 4, 4},
                    "K_6(4): 4-regular on 6 vertices  [hybrid boundary: 2d==n, d even → RevLex]"),
            new TestCase("K7_4",
                    new int[]{4, 4, 4, 4, 4, 4, 4},
                    "K_7(4): 4-regular on 7 vertices  [hybrid boundary: 2d==n, d even → RevLex]"),

            new TestCase("K8_4",
                    new int[]{4, 4, 4, 4, 4, 4, 4, 4},
                    "K_8(4): 4-regular on 8 vertices  [hybrid boundary: 2d==n, d even → RevLex]"),

            new TestCase("K9_4",
                    new int[]{4, 4, 4, 4, 4, 4, 4, 4, 4},
                    "K_9(4): 4-regular on 9 vertices"),

            new TestCase("K10_4",
                    new int[]{4, 4, 4, 4, 4, 4, 4, 4, 4, 4},
                    "K_10(4): 4-regular on 10 vertices"),

            new TestCase("K11_4",
                    new int[]{4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4},
                    "K_11(4): 4-regular on 11 vertices"),

            new TestCase("K12_4",
                    new int[]{4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4},
                    "K_12(4): 4-regular on 12 vertices"),

            new TestCase("K13_4",
                    new int[]{4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4},
                    "K_13(4): 4-regular on 13 vertices"),

            new TestCase("K14_4",
                    new int[]{4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4},
                    "K_14(4): 4-regular on 14 vertices"),

            // =====================================================================
            // d = 5  —  5-regular graphs
            // Note: 5-regular requires n even.
            // =====================================================================
            new TestCase("K6_5",
                    new int[]{5, 5, 5, 5, 5, 5},
                    "K_6(5): 5-regular on 6 vertices"),

            new TestCase("K8_5",
                    new int[]{5, 5, 5, 5, 5, 5, 5, 5},
                    "K_8(5): 5-regular on 8 vertices"),

            new TestCase("K10_5",
                    new int[]{5, 5, 5, 5, 5, 5, 5, 5, 5, 5},
                    "K_10(5): 5-regular on 10 vertices"),

            new TestCase("K12_5",
                    new int[]{5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5},
                    "K_12(5): 5-regular on 12 vertices"),


            // =====================================================================
            // d = 6  —  6-regular graphs
            // Note: 6-regular requires n even.
            // =====================================================================
            new TestCase("K7_6",
                    new int[]{6,6,6,6,6,6,6},
                    "K_7(6): 6-regular on 7 vertices"),
            new TestCase("K8_6",
                    new int[]{6,6,6,6,6,6,6,6},
                    "K_8(6): 6-regular on 8 vertices"),
            new TestCase("K9_6",
                    new int[]{6,6,6,6,6,6,6,6,6},
                    "K_9(6): 6-regular on 9 vertices"),
            new TestCase("K10_6",
                    new int[]{6,6,6,6,6,6,6,6,6,6},
                    "K_10(6): 6-regular on 10 vertices"),
            new TestCase("K11_6",
                    new int[]{6,6,6,6,6,6,6,6,6,6,6},
                    "K_11(6): 6-regular on 11 vertices"),
            new TestCase("K12_6",
                    new int[]{6,6,6,6,6,6,6,6,6,6,6,6},
                    "K_12(6): 6-regular on 12 vertices"),
            new TestCase("K13_6",
                    new int[]{6,6,6,6,6,6,6,6,6,6,6,6,6},
                    "K_13(6): 6-regular on 13 vertices"),
            new TestCase("K14_6",
                    new int[]{6,6,6,6,6,6,6,6,6,6,6,6,6,6},
                    "K_14(6): 6-regular on 14 vertices"),


            // =====================================================================
            // d = 7  —  7-regular graphs
            // Note: 7-regular requires n even.
            // =====================================================================
            new TestCase("K8_7",
                    new int[]{7,7,7,7,7,7,7,7},
                    "K_8(7): 7-regular on 8 vertices"),
            new TestCase("K10_7",
                    new int[]{7,7,7,7,7,7,7,7,7,7},
                    "K_10(7): 7-regular on 10 vertices"),
            new TestCase("K12_7",
                    new int[]{7,7,7,7,7,7,7,7,7,7,7,7},
                    "K_12(7): 7-regular on 12 vertices"),
            new TestCase("K14_7",
                    new int[]{7,7,7,7,7,7,7,7,7,7,7,7,7,7},
                    "K_14(7): 7-regular on 14 vertices"),


    };
}