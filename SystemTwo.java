import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

import static java.lang.System.exit;

public class SystemTwo {

    private static final int PREDICTION_NOT_TAKEN = 0;
    private static final int PREDICTION_SEMI_NOT_TAKEN = 1;
    private static final int PREDICTION_SEMI_TAKEN = 2;
    private static final int PREDICTION_TAKEN = 3;

    private static final int BTB_VALID_BIT_INDEX = 0;
    private static final int BTB_TAG_INDEX = 1;
    private static final int BTB_BTA_INDEX = 2;

    private String mContent;
    private int mNumBTBMisses;
    private int mNumBTBAccesses;
    private int mNumMisPredictions;
    private int mNumBranches;
    private int[] mPredictionBuffer;
    private int[][] mBranchTargetBuffer;


    public SystemTwo(String filePath) {
        try {
            mContent = new String(Files.readAllBytes(Paths.get(filePath)));
        } catch (IOException e) {
            e.printStackTrace();
            exit(1);
        }
    }

    private void startDynamicPrediction(final int N, final int M, boolean verboseModeEnabled) {
        String[] tokens = mContent.split("\\s+");
        mPredictionBuffer = new int[N];
        Arrays.fill(mPredictionBuffer, PREDICTION_SEMI_NOT_TAKEN);
        mBranchTargetBuffer = new int[M][3];
        int orderCount = 0;
        int debugPrintPreviousPrediction = PREDICTION_SEMI_NOT_TAKEN;
        int debugPrintCurrentPrediction;

        for (int i = 0; i < tokens.length; i+=4) {

            // Discard all branches that are not of type 1
            if (!tokens[i+1].equals("1")) {
                continue;
            } else {
                mNumBranches++;
            }

            final String currentBinaryAddress = hexToBinary(tokens[i]);
            final int currentTag = getBtbTag(currentBinaryAddress, M);
            final int targetAddress = Integer.parseInt(tokens[i + 2], 16);
            final int currentBtbIndex = getNBitNumber(currentBinaryAddress, M);
            final int currentPrediction = Integer.parseInt(tokens[i + 3]);
            final int predictorIndex = getNBitNumber(currentBinaryAddress, N);

            // Check if index is in the prediction buffer
            if (predictorIndex >= 0 && predictorIndex < N) {
                int previousPrediction = mPredictionBuffer[predictorIndex];
                debugPrintPreviousPrediction = previousPrediction;

                // Check BTB entry for miss if the branch is taken
                if (previousPrediction >= PREDICTION_SEMI_TAKEN && currentBtbIndex >= 0 && currentBtbIndex < M) {
                    final int[] bufferList =  mBranchTargetBuffer[currentBtbIndex];
                    mNumBTBAccesses++;

                    // Check for BTB miss
                    if (bufferList[BTB_VALID_BIT_INDEX] != 1 || bufferList[BTB_TAG_INDEX] != currentTag) {
                        mNumBTBMisses++;
                    }
                }

                // Check if Branch prediction is incorrect
                if (currentPrediction == 1 && previousPrediction < PREDICTION_SEMI_TAKEN ||
                        currentPrediction == 0 && previousPrediction > PREDICTION_SEMI_NOT_TAKEN) {
                    mNumMisPredictions++;
                }

                // Update prediction and get newest prediction result back
                debugPrintCurrentPrediction = updatePrediction(previousPrediction, currentPrediction, predictorIndex);

                // Update BTB if action is to take the branch, otherwise, ignore
                if (currentPrediction == 1) {
                    mBranchTargetBuffer[currentBtbIndex] = constructBTBLIST(currentTag, targetAddress);
                }
            } else {
                // Init new BTB buffer entry and new prediction entry
                mBranchTargetBuffer[currentBtbIndex] = constructBTBLIST(currentTag, targetAddress);
                debugPrintCurrentPrediction = updatePrediction(PREDICTION_SEMI_NOT_TAKEN, currentPrediction, predictorIndex);
            }


            if (verboseModeEnabled) {
                System.out.print("\n" + orderCount);
                System.out.print(" " + predictorIndex);
                System.out.print(" " + debugPrintPreviousPrediction);
                System.out.print(" " + debugPrintCurrentPrediction);
                System.out.print(" " + currentBtbIndex);
                System.out.print(" " + Integer.toString(currentTag, 16));
                System.out.print(" " + mNumBTBAccesses);
                System.out.print(" " + mNumBTBMisses);
                orderCount++;
            }
        }
        System.out.println("\n");
    }

    /**
     * Returns a decimal value of a number, of the first log(n) bits, minus the first 2 bits, i.e. :
     * 1100 1000 1010 1000, where n = 16 would return: 1100 1000 10|10 10|00, or 1010
     *
     */
    private int getNBitNumber(String binaryNumber, int n) {
        String predictorNBitValue = binaryNumber.substring(binaryNumber.length() - logBaseTwo(n) - 2, binaryNumber.length() - 2);
        return Integer.parseInt(predictorNBitValue, 2);
    }

    /**
     * Returns a a BTB tag, which is the rest of the binary address left over from excluding the first log2(m) bits
     * minus the 2 discarded bits. i.e. :
     * 1100 1000 1010 1000, where m = 4 would return: |1100 1000 1010| 1000, or 1100 1000 1010
     */
    private int getBtbTag(String binaryNumber, int m) {
        String tag = binaryNumber.substring(0, binaryNumber.length() - logBaseTwo(m) - 2);
        return Integer.parseInt(tag, 2);
    }

    private int logBaseTwo(int x) {
        return (int) (Math.log(x) / Math.log(2));
    }

    private String hexToBinary(String hex) {
        int intHex = Integer.parseInt(hex, 16);
        return Integer.toBinaryString(intHex);
    }

    /**
     * Constructs a list for creating/updating a BTB entry
     */
    private int[] constructBTBLIST(int tag, int targetAddress) {
        int[] bufferList = new int[3];
        bufferList[BTB_VALID_BIT_INDEX] = 1;
        bufferList[BTB_TAG_INDEX] = tag;
        bufferList[BTB_BTA_INDEX] = targetAddress;

        return bufferList;
    }

    /**
     * Updates prediction map based on previous/current prediction, and returns the new prediction.
     * Predictions have 4 values:
     * PREDICTION_TAKEN = 3, PREDICTION_SEMI_TAKEN = 2, PREDICTION_SEMI_NOT_TOKEN = 1, PREDICTION_NOT_TAKEN = 0
     *
     * If a current prediction's action is to take (1), then state of the current prediction is increased by 1 until,
     * it hits PREDICTION_TAKEN, in which case it has reached its maximum value.
     * If a current prediction's action is to not take (0), then the state of the current prediction is decreased by 1,
     * until it hits PREDICTION_NOT_TAKEN, in which case it has reached its minimum value
     */
    private int updatePrediction(int previousPrediction, int currentPrediction, int predictorIndex) {
        if (currentPrediction == 1 && previousPrediction < PREDICTION_TAKEN) {
            mPredictionBuffer[predictorIndex] = previousPrediction + 1;
            return previousPrediction + 1;
        } else if (currentPrediction == 0 && previousPrediction > PREDICTION_NOT_TAKEN) {
            mPredictionBuffer[predictorIndex] = previousPrediction - 1;
            return previousPrediction - 1;
        }

        return previousPrediction;
    }

    private void printResults() {
        System.out.println("Number of mispredictions = " + mNumMisPredictions + " = " + (float) mNumMisPredictions / mNumBranches);
        System.out.println("Number of BTB misses = " + mNumBTBMisses + " = " + (float) mNumBTBMisses / mNumBTBAccesses);
    }

    public static void main(String args[]) {
        boolean verboseModeEnabled;
        int M, N;

        if (args.length < 1) {
            System.out.println("Please provide input file as first argument");
            return;
        } else if (args.length > 2 && args.length <= 4) {
            N = Integer.parseInt(args[1]);
            M = Integer.parseInt(args[2]);
            verboseModeEnabled = args.length == 4;

        } else {
            System.out.println("Please specify only 3 or 4 arguments. The filePath, the # of entries in prediction buffer, " +
                    "the # of entries in the BTB, and optionally -v for verbose mode");
            return;
        }

        String filePath = args[0];

        // Run System Two
        SystemTwo systemTwo = new SystemTwo(filePath);
        systemTwo.startDynamicPrediction(N, M, verboseModeEnabled);

        // Print System One info
        SystemOne systemOne = new SystemOne(filePath, false);
        systemOne.startStaticPrediction();
        systemOne.printResults(false);

        // Print System Two Results (Placement here is to match the PDF output requirements)
        systemTwo.printResults();
    }
}
