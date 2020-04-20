package ch.so.agi.grundstuecksinformation;

public class SearchResult {
    private String display;
    private String dataproductId;
    private int featureId;
    private String idFieldName;
    private String egrid;
    private double minX;
    private double minY;
    private double maxX;
    private double maxY;
    
    public SearchResult() {}
    
    public SearchResult(String display) {
        this.display = display;
    }

    public String getLabel() {
        return display;
    }

    public void setLabel(String label) {
        this.display = label;
    }

    public String getDisplay() {
        return display;
    }

    public void setDisplay(String display) {
        this.display = display;
    }

    public String getDataproductId() {
        return dataproductId;
    }

    public void setDataproductId(String dataproductId) {
        this.dataproductId = dataproductId;
    }

    public int getFeatureId() {
        return featureId;
    }

    public void setFeatureId(int featureId) {
        this.featureId = featureId;
    }

    public String getIdFieldName() {
        return idFieldName;
    }

    public void setIdFieldName(String idFieldName) {
        this.idFieldName = idFieldName;
    }

    public String getEgrid() {
        return egrid;
    }

    public void setEgrid(String egrid) {
        this.egrid = egrid;
    }

    public double getMinX() {
        return minX;
    }

    public void setMinX(double minX) {
        this.minX = minX;
    }

    public double getMinY() {
        return minY;
    }

    public void setMinY(double minY) {
        this.minY = minY;
    }

    public double getMaxX() {
        return maxX;
    }

    public void setMaxX(double maxX) {
        this.maxX = maxX;
    }

    public double getMaxY() {
        return maxY;
    }

    public void setMaxY(double maxY) {
        this.maxY = maxY;
    }
}
