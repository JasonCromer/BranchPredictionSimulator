# Compiliation and Execution
NOTE: These programs use Java 8 (1.8), and cannot be compiled/executed without this version. The trace files required to execute these programs are also necessary, as both systems assume the trace files are SPARC trace files.

To compile, simply run:
```javac SystemOne.java SystemTwo.java```

To execute System one, run the following command:
```java SystemOne.java [path to trace file]```

To execute System two, state the path file, in addition to how many branch-predictor entries (N), and how many branch-target-buffer entries (M).
```java SystemTwo.java [path to trace file] [N] [M]```

Optionally, you can add `-v` to the end of each execution command for a verbose output