import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Perceptron {

    private double[] weights;
    private double learningRate;
    private int epochs;

    public List<Double> epochLoss     = new ArrayList<>();
    public List<Double> epochAccuracy = new ArrayList<>();

    public Perceptron(double learningRate, int epochs) {
        this.learningRate = learningRate;
        this.epochs = epochs;
        weights = new double[]{0.0, 0.0, 0.0, 0.0};
    }

    private int activate(double net) {
        return net >= 0.0 ? 1 : 0;
    }

    private double[] normalize(double[] features) {
        return new double[]{
                features[0] / 100.0,
                features[1] / 48.0,
                features[2] / 2.0
        };
    }

    public int predict(double[] features) {
        double[] norm = normalize(features);
        double net = weights[0]
                + weights[1] * norm[0]
                + weights[2] * norm[1]
                + weights[3] * norm[2];
        return activate(net);
    }

    public double train(double[][] X, int[] y) {
        epochLoss.clear();
        epochAccuracy.clear();

        for (int e = 0; e < epochs; e++) {
            int errors = 0;
            double totalLoss = 0;

            for (int i = 0; i < X.length; i++) {
                int pred  = predict(X[i]);
                int error = y[i] - pred;
                if (error != 0) errors++;
                totalLoss += Math.abs(error);

                double[] norm = normalize(X[i]);
                weights[0] += learningRate * error;
                weights[1] += learningRate * error * norm[0];
                weights[2] += learningRate * error * norm[1];
                weights[3] += learningRate * error * norm[2];
            }

            double acc = 1.0 - (double) errors / X.length;
            epochLoss.add(totalLoss / X.length);
            epochAccuracy.add(acc);
            if (errors <= 3) {
                System.out.println("Early Stopping at epoch: " + e);
                break;
            }
        }

        return epochAccuracy.get(epochAccuracy.size() - 1);
    }
    public double trainWithValidation(double[][] X, int[] y, double trainRatio) {
        epochLoss.clear();
        epochAccuracy.clear();

        int n = X.length;
        Random rand = new Random();

        double[][] X_shuffled = new double[n][];
        int[] y_shuffled = new int[n];
        for (int i = 0; i < n; i++) {
            X_shuffled[i] = X[i];
            y_shuffled[i] = y[i];
        }

        for (int i = n - 1; i > 0; i--) {
            int j = rand.nextInt(i + 1);

            double[] tempX = X_shuffled[i];
            X_shuffled[i] = X_shuffled[j];
            X_shuffled[j] = tempX;

            int tempY = y_shuffled[i];
            y_shuffled[i] = y_shuffled[j];
            y_shuffled[j] = tempY;
        }

        int trainSize = (int) (n * 0.8);
        int testSize = n - trainSize;

        double[][] Xtrain = new double[trainSize][];
        int[] Ytrain = new int[trainSize];
        double[][] Xtest = new double[testSize][];
        int[] Ytest = new int[testSize];

        for (int i = 0; i < trainSize; i++) {
            Xtrain[i] = X_shuffled[i];
            Ytrain[i] = y_shuffled[i];
        }
        for (int i = 0; i < testSize; i++) {
            Xtest[i] = X_shuffled[trainSize + i];
            Ytest[i] = y_shuffled[trainSize + i];
        }

        for (int e = 0; e < epochs; e++) {
            int errors = 0;
            double totalLoss = 0;

            for (int i = 0; i < Xtrain.length; i++) {
                int pred = predict(Xtrain[i]);
                int error = Ytrain[i] - pred;

                if (error != 0) {
                    errors++;
                    double[] norm = normalize(Xtrain[i]);
                    weights[0] += learningRate * error;
                    weights[1] += learningRate * error * norm[0];
                    weights[2] += learningRate * error * norm[1];
                    weights[3] += learningRate * error * norm[2];
                }
                totalLoss += Math.abs(error);
            }

            double acc = 1.0 - (double) errors / Xtrain.length;
            epochLoss.add(totalLoss / Xtrain.length);
            epochAccuracy.add(acc);
        }

        int correct = 0;
        for (int i = 0; i < Xtest.length; i++) {
            if (predict(Xtest[i]) == Ytest[i]) {
                correct++;
            }
        }

        return (double) correct / testSize;
    }

    public void predictAll(List<Plant> plants) {
        for (Plant p : plants) {
            p.setNeedsWater(predict(p.getFeatures()));
        }
    }

    public double[] getWeights() { return weights; }

    public String getWeightsSummary() {
        return String.format("Bias=%.4f  Moisture=%.4f  LastWatered=%.4f  PlantType=%.4f",
                weights[0], weights[1], weights[2], weights[3]);
    }
}