public class Plant {
    private String name;
    private double x, y;
    private double soilMoisture;
    private double lastWatered;
    private int plantType;
    private int needsWater;

    public Plant(String name, double x, double y, double soilMoisture, double lastWatered, int plantType) {
        this.name = name;
        this.x = x;
        this.y = y;
        this.soilMoisture = soilMoisture;
        this.lastWatered = lastWatered;
        this.plantType = plantType;
        this.needsWater = -1;
    }

    public double distanceTo(Plant other) {
        double dx = this.x - other.x;
        double dy = this.y - other.y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    public double[] getFeatures() {
        return new double[]{soilMoisture, lastWatered, plantType};
    }

    public String getName()              { return name; }
    public double getX()                 { return x; }
    public double getY()                 { return y; }
    public double getSoilMoisture()      { return soilMoisture; }
    public double getLastWatered()       { return lastWatered; }
    public int getPlantType()            { return plantType; }
    public int getNeedsWater()           { return needsWater; }
    public void setNeedsWater(int v)     { this.needsWater = v; }
    public void setX(double x)           { this.x = x; }
    public void setY(double y)           { this.y = y; }
    public void setSoilMoisture(double v){ this.soilMoisture = v; }
    public void setLastWatered(double v) { this.lastWatered = v; }
    public void setPlantType(int v)      { this.plantType = v; }
    public void setName(String n)        { this.name = n; }

    @Override
    public String toString() {
        String type = plantType == 0 ? "Cactus" : plantType == 1 ? "Flower" : "Herb";
        return name + " [" + type + "] moisture=" + soilMoisture + " lastWatered=" + lastWatered + "h";
    }
}
