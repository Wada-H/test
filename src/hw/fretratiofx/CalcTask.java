package hw.fretratiofx;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;
import javafx.concurrent.Task;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

public class CalcTask extends Task<ImagePlus> {

    ImagePlus imp;
    int calcNum;

    int width;
    int height;
    int c;
    int z;
    int t;

    String imageTitle;

    double min, max;
    int mod;
    int num;
    int exValue;
    ImagePlus colorMapImage;

    double minIntensity, maxIntensity;


    public CalcTask(ImagePlus img, int num){
        imp = img;
        calcNum = num;

        width = img.getWidth();
        height = img.getHeight();
        c = img.getNChannels();
        z = img.getNSlices();
        t = img.getNFrames();


        imageTitle = "FRETratio_CalcNo" + (calcNum + 1);
    }

    public void setExValue(int value){
        exValue = value;
    }

    public void setRatio(double minValue, double maxValue){
        min = minValue;
        max = maxValue;
    }

    public void setMinMaxIntensity(double min, double max){
        minIntensity = min;
        maxIntensity = max;
    }

    public void setCalcHSB(double minValue, double maxValue, int modValue, int numValue){
        min = minValue;
        max = maxValue;
        num = numValue;
        mod = modValue;

    }


    //並列を意識すると、各段階でnewが必要であるため
    private ImageProcessor autoSelectMethod(ImageProcessor ipA, ImageProcessor ipB){
        ArrayList<Calc> calcList = new ArrayList<>();
        calcList.add(new Calc1());
        calcList.add(new Calc2());
        calcList.add(new Calc3());
        calcList.add(new Calc4());

        for(int i = 0; i < calcList.size(); i++){
            calcList.get(i).setMode(mod);
            calcList.get(i).setRatio(min, max);
            calcList.get(i).setExValue(exValue);
            if(i == 3) {
                ((Calc4) calcList.get(i)).setRatioGroupNum(num);
                ((Calc4) calcList.get(i)).setIntensityMod(0);
                ((Calc4) calcList.get(i)).setIntensity(minIntensity, maxIntensity);
            }
        }

        ImageProcessor result = calcList.get(calcNum).getProcessor(ipA, ipB);
        if((calcNum == 3)&&(colorMapImage == null)){
            colorMapImage = calcList.get(calcNum).viewColorMapImage(256, 256);
        }
        return result;
    }


    private String getSelectedMethodTitle(){
        ArrayList<Calc> calcList = new ArrayList<>();
        calcList.add(new Calc1());
        calcList.add(new Calc2());
        calcList.add(new Calc3());
        calcList.add(new Calc4());

        for(int i = 0; i < calcList.size(); i++){
            calcList.get(i).setMode(mod);
            calcList.get(i).setRatio(min, max);
            calcList.get(i).setExValue(exValue);
            if(i == 3) {
                ((Calc4) calcList.get(i)).setRatioGroupNum(num);
                ((Calc4) calcList.get(i)).setIntensityMod(1);
                ((Calc4) calcList.get(i)).setIntensity(minIntensity, maxIntensity);
            }
        }

        /* //この記述ではだめ
        calcList.forEach(calc ->{
            calc.setMode(mod);
            calc.setRatio(min, max);
            calc.setExValue(exValue);
            ((Calc4)calc).setRatioGroupNum(num);
        });
        */

        String result = "";
        String buff = calcList.get(calcNum).getTitle();
        if(buff != null){
            result = buff;
        }
        return result;
    }


    @Override
    protected ImagePlus call() throws Exception {

        ArrayList<ArrayList<ImageProcessor>> buffList = new ArrayList<>();

        for(int i = 0; i < t; i++){
            ArrayList<ImageProcessor> buff = new ArrayList<>();
            buffList.add(buff);
        }


        ImageStack buffS = new ImageStack(width, height);


        IntStream intStream = IntStream.range(0, t);
        intStream.parallel().forEach(ct ->{
            updateProgress(ct,t);

            for(int cz = 0; cz < z; cz++){
                int indexA = imp.getStackIndex(1, cz+1, ct+1);
                int indexB = imp.getStackIndex(2, cz+1, ct+1);
                ImageProcessor ipA = imp.getStack().getProcessor(indexA).duplicate();
                ImageProcessor ipB = imp.getStack().getProcessor(indexB).duplicate();

                ImageProcessor buffP = this.autoSelectMethod(ipA, ipB);

                buffList.get(ct).add(buffP);
            }

        });


        for(int ct = 0; ct < t; ct++){
            for(int cz = 0; cz < z; cz++){
                buffS.addSlice(buffList.get(ct).get(cz));
            }
        }


        imageTitle = imageTitle + this.getSelectedMethodTitle();
        if(calcNum == 3){
            colorMapImage.show();
        }

        ImagePlus result = new ImagePlus();
        result.setStack(buffS);
        result.setTitle(imageTitle);
        updateValue(result);
        return result;
    }


}
