package hw.fretratiofx;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;
import javafx.concurrent.Task;

import java.util.ArrayList;

public class CalcTask extends Task<ImagePlus> {

    ImagePlus imp;
    int calcNum;
    ArrayList<Calc> calcList;

    int width;
    int height;
    int c;
    int z;
    int t;

    String imageTitle;

    public CalcTask(ImagePlus img, int num){
        imp = img;
        calcNum = num;

        width = img.getWidth();
        height = img.getHeight();
        c = img.getNChannels();
        z = img.getNSlices();
        t = img.getNFrames();

        calcList = new ArrayList<>();
        calcList.add(new Calc1());
        calcList.add(new Calc2());
        calcList.add(new Calc3());
        imageTitle = "FRETratio_CalcNo" + (calcNum + 1);
    }

    public void setExValue(int value){
        calcList.get(1).setExValue(value);
        imageTitle = imageTitle + "_exv" + value;
    }

    public void setRatio(double min, double max){
        calcList.get(2).setRatio(min, max);
        imageTitle = imageTitle + "_min" + min + "_max" + max;
    }

    @Override
    protected ImagePlus call() throws Exception {

        //ImageStack buff_stack = new ImageStack(width, height); //並列を考えてArrayListに?
        //ArrayList<ImageProcessor> buffList = new ArrayList<ImageProcessor>(z * t);
        ImageStack buffS = new ImageStack(width, height);

        for(int ct = 0; ct < t; ct++){
            for(int cz = 0; cz < z; cz++){
                int indexA = imp.getStackIndex(1, cz+1, ct+1);
                int indexB = imp.getStackIndex(2, cz+1, ct+1);
                ImageProcessor buffP = calcList.get(calcNum).getProcessor(imp.getStack().getProcessor(indexA), imp.getStack().getProcessor(indexB));
                buffS.addSlice(buffP);
            }
        }
        ImagePlus result = new ImagePlus();
        result.setStack(buffS);
        result.setTitle(imageTitle);
        updateValue(result);
        return result;
    }


}
