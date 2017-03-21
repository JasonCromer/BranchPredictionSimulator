import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static java.lang.System.exit;

public class SystemOne {

    private String mContent;
    private int mNumBranches, mNumForwardBranches, mNumBackwardBranches, mNumForwardTakenBranches, mNumBackwardTakenBranches;
    private int mNumMisPredictions;

    public SystemOne(String filePath, boolean enableVerboseMode) {
        try {
            mContent = new String(Files.readAllBytes(Paths.get(filePath)));
        } catch (IOException e) {
            e.printStackTrace();
            exit(1);
        }

        if (enableVerboseMode) {
            System.out.println(mContent);
        }
    }

    public void startStaticPrediction() {
        String[] tokens = mContent.split("\\s+");

        for (int i = 0; i < tokens.length; i+=4) {

            // Discard all branches that are not of type 1
            if (!tokens[i+1].equals("1")) {
                continue;
            } else {
                mNumBranches++;
            }

            final long currentAddress = Long.parseLong(tokens[i], 16);
            final long targetAddress = Long.parseLong(tokens[i + 2], 16);
            final int prediction = Integer.parseInt(tokens[i + 3]);

            if (currentAddress < targetAddress) {
                mNumForwardBranches++;

                if (prediction == 1) {
                    mNumForwardTakenBranches++;
                    mNumMisPredictions++;
                }
            } else {
                mNumBackwardBranches++;

                if (prediction == 1) {
                    mNumBackwardTakenBranches++;
                } else {
                    mNumMisPredictions++;
                }
            }
        }
    }

    public void printResults(boolean showMisPredictions) {
        System.out.println("Number of branches = " + mNumBranches);
        System.out.println("Number of forward branches = " + mNumForwardBranches);
        System.out.println("Number of forward taken branches = " + mNumForwardTakenBranches);
        System.out.println("Number of backward branches = " + mNumBackwardBranches);
        System.out.println("Number of backward taken branches = " + mNumBackwardTakenBranches);

        if (showMisPredictions) {
            System.out.println("Number of mispredictions = " + mNumMisPredictions + " = " + (float) mNumMisPredictions / mNumBranches);
        }
    }

    public static void main(String args[]) {
        boolean verboseModeEnabled = false;

        if (args.length < 1) {
            System.out.println("Please provide input file as first argument");
            return;
        } else if(args.length > 1) {
            verboseModeEnabled = args[1].equals("-v");
        }

        String filePath = args[0];
        SystemOne systemOne = new SystemOne(filePath, verboseModeEnabled);
        systemOne.startStaticPrediction();
        systemOne.printResults(true);
    }
}