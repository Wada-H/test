package hw.fretratiofx;

import ij.ImagePlus;
import ij.process.ImageProcessor;

public abstract class Calc {
    double max_ratio;
    double min_ratio;
    String title;
    ImagePlus colorMap;

    int mod = 1; // 0:auto, 1:manual

    public Calc(){

    }

    public ImageProcessor getProcessor(ImageProcessor a, ImageProcessor b){
        return null;
    }

    public void setExValue(int value){

    }

    public void setRatio(double min, double max){
        max_ratio = max;
        min_ratio = min;
    }

    public void setMode(int v){
        mod = v;
    }

    public double getMinRatio(){
        return min_ratio;
    }

    public double getMaxRatio(){
        return max_ratio;
    }

    public String getTitle(){
        return title;
    }

    public ImagePlus viewColorMapImage(int ratioSize, int intensitySize) {
        return colorMap;
    }
}
