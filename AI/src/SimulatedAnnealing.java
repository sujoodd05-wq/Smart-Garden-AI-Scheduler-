import java.util.*;

public class SimulatedAnnealing {
    private double temperature;
    private double coolingRate;
    private int maxIterations;
    private Random random = new Random();
    public List<Double> costHistory = new ArrayList<>();
    public List<int[]> orderHistory = new ArrayList<>();

    public SimulatedAnnealing(double temperature, double coolingRate, int maxIterations) {
        this.temperature = temperature;
        this.coolingRate = coolingRate;
        this.maxIterations = maxIterations;
    }

    public double calculateCost(int[] order, List<Plant> allPlants) {
        double distance = 0;
        int missed = 0;
        int extra = 0;

        for (int i = 0; i < allPlants.size(); i++) {
            if (allPlants.get(i).getNeedsWater() == 1) {
                boolean inSequence = false;
                for (int idx : order) if (idx == i) inSequence = true;
                if (!inSequence) missed++;
            }
        }
        for (int i = 0; i < order.length - 1; i++) {
            distance += allPlants.get(order[i]).distanceTo(allPlants.get(order[i + 1]));
        }
        for (int idx : order) {
            if (allPlants.get(idx).getNeedsWater() == 0) extra++;
        }

        return (double) (missed * 100) + distance + (extra * 20);
    }

    private int[] getNeighbor(int[] currentOrder, int totalInGarden) {
        int[] nextOrder = currentOrder.clone();

        if (random.nextBoolean() || nextOrder.length == totalInGarden) {
            int i = random.nextInt(nextOrder.length);
            int j = random.nextInt(nextOrder.length);
            int temp = nextOrder[i];
            nextOrder[i] = nextOrder[j];
            nextOrder[j] = temp;
        }
        else {
            int i = random.nextInt(nextOrder.length);
            int j = random.nextInt(totalInGarden);

            boolean alreadyExists = false;
            for(int val : nextOrder) if(val == j) alreadyExists = true;

            if(!alreadyExists) nextOrder[i] = j;
        }
        return nextOrder;
    }

    public int[] optimize(List<Plant> allPlants, int numToVisit) {
        costHistory.clear();
        orderHistory.clear();

        List<Integer> pool = new ArrayList<>();
        for (int i = 0; i < allPlants.size(); i++) pool.add(i);
        Collections.shuffle(pool);
        int[] xcurr = new int[numToVisit];
        for (int i = 0; i < numToVisit; i++) xcurr[i] = pool.get(i);

        double currentCost = calculateCost(xcurr, allPlants);

        int[] xbest = xcurr.clone();
        double bestCost = currentCost;

        double T = this.temperature;

        for (int i = 1; i <= maxIterations; i++) {

            T *= (1.0 - coolingRate);

            int[] xnext = getNeighbor(xcurr, allPlants.size());
            double nextCost = calculateCost(xnext, allPlants);

            double deltaE = currentCost - nextCost;

            if (deltaE > 0) {
                xcurr = xnext;
                currentCost = nextCost;

                if (nextCost < bestCost) {
                    xbest = xnext.clone();
                    bestCost = nextCost;
                }
            }
            else if (Math.exp(deltaE / T) > random.nextDouble()) {
                xcurr = xnext;
                currentCost = nextCost;
            }

            costHistory.add(currentCost);
            if (i % 50 == 0) orderHistory.add(xcurr.clone());
        }

        return xbest;
    }

    public double getFinalBestCost() {
        return costHistory.isEmpty() ? 0 : Collections.min(costHistory);
    }
}