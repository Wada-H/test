package hw.fretratiofx;


import ij.ImagePlus;
import ij.plugin.Duplicator;
import ij.process.ImageProcessor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.stream.IntStream;

// stack画像全体の最大値、最小値を見つける //
public class SearchMaxMin {

    ImagePlus mainImage;
    ImagePlus acceptorImage;
    ImagePlus donorImage;

    double min = Double.MAX_VALUE;
    double max = Double.MIN_VALUE;

    public SearchMaxMin(ImagePlus img){
        Duplicator duplicator = new Duplicator();
        mainImage = img;


        donorImage = duplicator.run(mainImage, 1,1,1,mainImage.getNSlices(), 1, mainImage.getNFrames());
        acceptorImage = duplicator.run(mainImage, 2,2,1,mainImage.getNSlices(), 1, mainImage.getNFrames());

    }


    public void searchMaxMin(){

        IntStream intStream = IntStream.range(0, donorImage.getStackSize());

        ArrayList<Double> maxValue = new ArrayList<>();
        ArrayList<Double> minValue = new ArrayList<>();

        intStream.parallel().forEach(i ->{
            double[] minmax = this.getMinMax(donorImage.getStack().getProcessor(i+1), acceptorImage.getStack().getProcessor(i+1));
            minValue.add(minmax[0]);
            maxValue.add(minmax[1]);

        });

        min = minValue.parallelStream().parallel().min(Comparator.naturalOrder()).get().doubleValue();
        max = maxValue.parallelStream().parallel().max(Comparator.naturalOrder()).get().doubleValue();
    }

    //独自に並列にしたほうがいいのか？ものとmethod使うならmin と maxで2回同じ計算をしている。
    public double[] getMinMax(ImageProcessor donor, ImageProcessor acceptor){

        ArrayList<Double> data = new ArrayList<>();
        for(int x = 0; x < donor.getWidth(); x++) {
            for (int y = 0; y < donor.getHeight(); y++) {
                double dValue = donor.getf(x, y);
                double aValue = acceptor.getf(x, y);

                if(dValue == 0){
                    aValue = 0;
                }


                if(aValue > max){
                    max = aValue;
                }

                if(aValue < min){
                    min = aValue;
                }
            }
        }

        double[] minmax = {min, max};


        //double[] minmax = {ip.getMin(), ip.getMax()};
        return minmax;
    }

    public double getMin(){
        return min;
    }

    public double getMax(){
        return max;
    }

}