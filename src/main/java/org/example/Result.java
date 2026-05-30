package org.example;

public class Result {
    public int    count;
    public double cpu;
    public long   fails;
    public long   branches;
    public long   choicePoints;
    public long   constraints;

    public Result(int count, double cpu,
                  long fails, long branches,
                  long choicePoints, long constraints) {
        this.count        = count;
        this.cpu          = cpu;
        this.fails        = fails;
        this.branches     = branches;
        this.choicePoints = choicePoints;
        this.constraints  = constraints;
    }

    // Backward-compatible constructor (no stats) — keeps old call sites compiling
    public Result(int count, double cpu) {
        this(count, cpu, -1, -1, -1, -1);
    }

    public Result() {}

    /**
     * Print a side-by-side comparison between this result and another.
     * Example: lexResult.compare(revLexResult, "Lex", "RevLex");
     */
    public String compare(Result other, String myLabel, String otherLabel) {
        return String.format(
                "%-10s | solutions=%-6d fails=%-8d branches=%-10d choicePoints=%-10d time=%.1fms%n" +
                        "%-10s | solutions=%-6d fails=%-8d branches=%-10d choicePoints=%-10d time=%.1fms%n" +
                        "ratio      | fails=%.2fx  branches=%.2fx  time=%.2fx",
                myLabel,    count,       fails,       branches,       choicePoints,       cpu,
                otherLabel, other.count, other.fails, other.branches, other.choicePoints, other.cpu,
                other.fails    == 0 ? Double.NaN : (double) fails    / other.fails,
                other.branches == 0 ? Double.NaN : (double) branches / other.branches,
                other.cpu      == 0 ? Double.NaN :          cpu      / other.cpu
        );
    }

    @Override
    public String toString() {
        return String.format(
                "Result{count=%d, cpu=%.1fms, fails=%d, branches=%d, choicePoints=%d, constraints=%d}",
                count, cpu, fails, branches, choicePoints, constraints
        );
    }



}