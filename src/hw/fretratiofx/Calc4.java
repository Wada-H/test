package hw.fretratiofx;

import ij.ImagePlus;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

/*
 * 20190717
 * 吉田くんの撮影画像において、Ratioの幅が広い場合
 * 8bit + LUTの限界によりRGB画像でさらに細かい表現ができないか模索する
 * HSB color使用？ IMDwithHSB
 *
 * Yの輝度をもとにBの値を決定する
 * Y/CのratioでHの値を決定する
 * Sは？ ->とりあえずratioを使用 -> intensityを利用するほうがよさそう
 *
 *
 * 任意の分割が可能。ただし、8分割としてもIMDと若干異なる結果が出る。
 */

public class Calc4 extends Calc {

    double limitRatio = 0.0; //あまりにもRatioが大きい場合の足切り用 -> 保留

    double detectedMaxRatio;
    float heuRange = 270.0f / 360;
    float initialHeuRange = 1.0f - heuRange;

    int intensityMod = 0; //0:one slice, 1:use input number


    double maxIntensity = Double.MAX_VALUE;
    double minIntensity = Double.MIN_VALUE;

    int dividingNum = 0;
    ArrayList<Double> dividingLimits;
    @Override
    public String getTitle(){
        if(mod == 1){
            title = "_min" + min_ratio + "_max" + max_ratio;
        }else{
            title = "_min" + 0.0 + "_max-Auto";
        }
        return title;
    }


    @Override
    public ImageProcessor getProcessor(ImageProcessor donor, ImageProcessor acceptor){

        ColorProcessor result = new ColorProcessor(donor.getWidth(), donor.getHeight());

        ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, Double>> ratioMap = new ConcurrentHashMap<>();
        ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, Double>> intensityAMap = new ConcurrentHashMap<>();
        ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, Double>> intensityDMap = new ConcurrentHashMap<>();


        IntStream intStream = IntStream.range(0, donor.getWidth());
        intStream.parallel().forEach(x ->{
            ConcurrentHashMap<Integer, Double> buffR = new ConcurrentHashMap<>();
            ConcurrentHashMap<Integer, Double> buffI = new ConcurrentHashMap<>();
            //ConcurrentHashMap<Integer, Double> buffD = new ConcurrentHashMap<>();

            for(int y = 0; y < donor.getHeight(); y++){
                double d_value = (double)donor.getPixel(x, y);
                double a_value = (double)acceptor.getPixel(x, y);

                double value = 0.0;
                //ratio　計算
                if(d_value == 0){ //donor = 0の時の対処。ドナー側が0である場合アクセプター側の輝点はnoiseと考える
                    d_value = 1;
                    a_value = 0;
                }

                double ratio = a_value / d_value;

                if(mod == 1) {
                    if (ratio > max_ratio) {
                        ratio = max_ratio;
                        a_value = d_value * max_ratio; //raoitoは1, 加えてintensityを下げておくことでBの値をカットできる?
                    }

                    //ratio = ratio - min_ratio;
                    //if (ratio < 0) {
                    //    ratio = 0.0;
                    //    //a_value = 0.0;
                    //}

                    if(ratio < min_ratio){
                        ratio = min_ratio;
                    }
                    ratio = ratio - min_ratio;

                }


                buffR.put(y, ratio);
                buffI.put(y, a_value);
                //buffD.put(y, d_value);

            }
            ratioMap.put(x, buffR);
            intensityAMap.put(x, buffI);
            //ntensityDMap.put(x, buffD);
        });

        float maxRatio = (float) this.getMaxAndMinValue(ratioMap)[0];

        //結局こうやって輝度を揃えてもちらつく。。意味があるのか？20191114//
        if(intensityMod == 0) {
            double[] intensityMM = this.getMaxAndMinValue(intensityAMap);
            maxIntensity = intensityMM[0];
            minIntensity = intensityMM[1];
        }
        //System.out.println("Calc4 intensity min max : " + minIntensity + ", " + maxIntensity);
        //float maxIntensityD = (float) this.getMaxValue(intensityDMap); //ドナー側の輝度でSを調整できたら面白いかも

        detectedMaxRatio = maxRatio;

        //System.out.println("maxRatio : " + maxRatio);
        for(int x = 0; x < acceptor.getWidth(); x++){
            for(int y = 0; y < acceptor.getHeight(); y++){
                float iRatio = (float)(intensityAMap.get(x).get(y) / maxIntensity);
                double initializedRatio = heuRange * (ratioMap.get(x).get(y).floatValue() / (max_ratio - min_ratio)) + initialHeuRange;

                if(dividingNum > 1) {
                    initializedRatio = this.convertLimitRatio(initializedRatio);
                }

                //float initializedRatioD = (intensityDMap.get(x).get(y).floatValue() / maxIntensityD);

                if(ratioMap.get(x).get(y).floatValue() < min_ratio){
                    iRatio = iRatio * (maxRatio/acceptor.getWidth()* x)/ (float)min_ratio;
                }

                Color c = Color.getHSBColor(1.0f - (float)initializedRatio, iRatio, iRatio);
                //Color c = Color.getHSBColor(initializedRatio, initializedRatioD, iRatio);

                result.putPixel(x, y, c.getRGB());
            }
        }

        return result;
    }

    public void setColorMode(int mode){ //modeの値によって使用するHの領域を決める？

    }

    public void setDirectionH(int d){ // 0: min -> max, 1: max -> min

    }

    public void setStartHvalue(double value){//Hの始める値

    }

    public void setColorRnage(double min, double max){ // 0.0 - 1.0の値で

    }

    public void setRatioGroupNum(int n){
        if(n == 1){
           dividingNum = 0;
        }else {
            dividingNum = n;
        }

        dividingLimits = new ArrayList<>();
        double stepSize = (1.0 - initialHeuRange) / dividingNum;

        for(int i = 0; i < dividingNum ; i++){
            double value = 1.0 - stepSize * i;
            //System.out.println("ratioGroupNum : " + value);
            dividingLimits.add(value);
        }
    }

    public double[] getMaxAndMinValue(ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, Double>> valueMap){
        ArrayList<Double> valueArrayMax = new ArrayList<>();
        valueMap.forEach((x, yMap) ->{
            valueArrayMax.add(yMap.values().stream().max(Comparator.naturalOrder()).get());
        });

        ArrayList<Double> valueArrayMin = new ArrayList<>();
        valueMap.forEach((x, yMap) ->{
            valueArrayMin.add(yMap.values().stream().min(Comparator.naturalOrder()).get());
        });

        double[] result = new double[2];
        result[0] = valueArrayMax.stream().max(Comparator.naturalOrder()).get().doubleValue();
        result[1] = valueArrayMin.stream().min(Comparator.naturalOrder()).get().doubleValue();
        return result;
    }

    public double getMedianValue(ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, Double>> valueMap){
        ArrayList<Double> valueArray = new ArrayList<>();
        valueMap.forEach((x, yMap) ->{
            yMap.forEach((y, value) ->{
                valueArray.add(value);
            });
        });

        return (double)valueArray.stream().sorted(Comparator.naturalOrder()).toArray()[valueArray.size()/2];
    }


    @Override
    public ImagePlus viewColorMapImage(int ratioSize, int intensitySize){ //セットした値でのテスト画像を表示,256(intensity)x256(ratio)
        //ratioは0.0 - maxで表示、intensityは0.0 - intensitySize(= 1)となるように表現

        ColorProcessor cpro = new ColorProcessor(ratioSize, intensitySize);

        double allRangeInterval = max_ratio / (ratioSize - 1);
        int positionOfmin = 0;
        for(int i = 0; i < ratioSize; i++){
            double buff = (allRangeInterval * i) / max_ratio;
            if(buff > (min_ratio / max_ratio)){
                positionOfmin = i -1;
                break;
            }
        }

        //System.out.println("positionOfmin : " + positionOfmin);
        double ratioInterval = (max_ratio - min_ratio) / (ratioSize - positionOfmin - 1);
        double intensityInterval = (maxIntensity - minIntensity) / (intensitySize - 1);
        double initialValue = min_ratio / max_ratio;

        double originalRatio = 0.0;
        for(int x = 0; x < ratioSize; x++){
            if(x > positionOfmin){
                originalRatio = originalRatio + ratioInterval;
            }

            float ratio = (float)(originalRatio / (max_ratio - min_ratio));
            //float ratio = (float)((ratioInterval * x) / (max_ratio - min_ratio));
            float buffRatio = (float)((allRangeInterval * x) / min_ratio);
            if(ratio > 1.0){
                ratio = 1.0f;
            }




            //System.out.println("x vs ratio : " + x + " vs " + ratio);
            float initializedRatio = heuRange * ratio + initialHeuRange;//Hue逆回転のため
            if(dividingNum > 1){
                initializedRatio = (float)this.convertLimitRatio(initializedRatio);
            }
            for(int y = 0; y < intensitySize; y++){
                float initializedIntensity = (float)((intensityInterval * y) / maxIntensity);
                if(initializedIntensity > 1.0){
                    initializedIntensity = 1.0f;
                }
                if(x < positionOfmin){
                    initializedIntensity = initializedIntensity * buffRatio;
                }
                Color c = Color.getHSBColor((1.0f - initializedRatio), initializedIntensity, initializedIntensity);
                cpro.putPixel(x, intensitySize - y -1, c.getRGB());
            }
        }

        ImagePlus result = new ImagePlus();
        result.setProcessor("ColorMap_" + "Rmin" + min_ratio + "_Rmax" + max_ratio + "_Imin" + minIntensity + "_Imax" + maxIntensity, cpro);
        return result;
    }


    public double convertLimitRatio(double ratio){
        final double checkingRatio = ratio;
        int clearNum = 0;
        for(int i = 0; i < dividingLimits.size(); i++){
            //System.out.println("num " + i);
            if(checkingRatio < dividingLimits.get(i)){
                clearNum = i;
            }else{
                break;
            }
        }
        //System.out.println("clearNum " + clearNum);
        double result = dividingLimits.get(clearNum).floatValue();
        //System.out.println("convertRatio : " + ratio + " vs " + result + "+" + clearNum);
        return result;
    }

    public void setIntensityMod(int num){
        intensityMod = num;
    }

    public void setIntensity(double min, double max){
        minIntensity = min;
        maxIntensity = max;
    }

}
