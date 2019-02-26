package hw.fretratiofx;

import ij.ImagePlus;
import ij.gui.Roi;
import ij.process.ImageProcessor;

import java.util.stream.IntStream;

public class AutoCut {

    ImagePlus imp;
    Roi roi;
    boolean selfSlice = true;

    public AutoCut(ImagePlus imageplus){
        imp = imageplus;
        roi = imageplus.getRoi();
        imp.killRoi();
    }


    public void autocut(int selected_method_id, double ext_num){
        int current_c = imp.getC();
        int current_z = imp.getZ();
        int current_t = imp.getT();
        int c = imp.getNChannels();
        int z = imp.getNSlices();


        int stackIndex = (current_t - 1) * c * z + ((current_c - 1) * z) + (current_z);

        ImagePlus currentSliceImage = this.duplicate1slice(imp, stackIndex);

        //* 並び順が違う場合もok?
        final int peak_value;
        if(!selfSlice){
            peak_value = getSubtractionValue(currentSliceImage.getProcessor(), selected_method_id);
        }else{
            peak_value = 0;
        }


        ImagePlus new_img = imp.duplicate();
        imp.setRoi(roi);

        IntStream i_steram = IntStream.range(0, imp.getStackSize());

        i_steram.parallel().forEach(i ->{
            int p_value = peak_value;

            if (selfSlice) {
                ImagePlus buffImage = this.duplicate1slice(imp, (i + 1));
                p_value = getSubtractionValue(buffImage.getProcessor(), selected_method_id);
            } else {
                //何もしない？
            }

            double subtract_value = p_value * ext_num;
            //System.out.println("subtract:" + subtract_value);
            ImageProcessor ip = new_img.getStack().getProcessor(i + 1);
            ip.subtract(subtract_value);
        });
        new_img.setFileInfo(imp.getOriginalFileInfo());
        imp.setImage(new_img);
    }

    public void setSelfSlice(boolean b){
        selfSlice = b;
    }

    private int getSubtractionValue(ImageProcessor ip, int option){
        //option = 0 : average, 1 : mode
        int return_value = 0;

        int width = ip.getWidth();
        int height = ip.getHeight();
        int depth = ip.getBitDepth();

        if(option == 0){ //平均値
            float buff_value = 0;
            for(int y = 0; y < height; y++){
                for(int x = 0; x < width; x++){
                    int value = ip.getPixel(x, y);
                    buff_value = buff_value + (value/(float)(width * height));
                }
            }

            return_value = (int)Math.round(buff_value);
        }else if(option == 1){ //最頻出値


            int[] hist_array = new int[65536];
            int max_value = 0;

            for(int y = 0; y < height; y++){
                for(int x = 0; x < width; x++){
                    int value = ip.getPixel(x, y);
                    hist_array[value] += 1;
                }
            }

            //int limit = (hist_array.length / depth) - 1; // backgroundとして認める最大値 ->これにすると輝度が高い方にシフトしたような場合に対応できない。
            int limit = hist_array.length;

            for(int i = 1; i < limit; i++){ //0を無視するため i = 1から
                int value = hist_array[i];
                if(max_value < value){
                    max_value = value;
                    return_value = i;
                }
            }
        }
        //System.out.println("getp:" + return_value);
        return return_value;
    }

    private ImagePlus duplicate1slice(ImagePlus img, int stackIndex){
        ImageProcessor buffip = img.getStack().getProcessor(stackIndex);
        ImagePlus resultimg = new ImagePlus();
        resultimg.setProcessor(buffip);
        resultimg.setRoi(roi);
        return resultimg.duplicate();
    }


}
