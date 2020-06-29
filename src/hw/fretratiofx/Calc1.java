package hw.fretratiofx;

import ij.process.ImageProcessor;

public class Calc1 extends Calc{

    @Override
    public ImageProcessor getProcessor(ImageProcessor a, ImageProcessor b){

        int bit_depth = a.getBitDepth();
        int width = a.getWidth();
        int height = a.getHeight();

        ImageProcessor return_imp = a.duplicate();


        double bitMaxValue = Math.pow(2.0, (double)bit_depth);


        int[] projection_pixel_array_i = new int[width * height];
        short[] projection_pixel_array_s = new short[width * height];
        byte[] projection_pixel_array_b = new byte[width * height];

        for(int x = 0; x < width; x++){
            double value = 0.0;
            for(int y = 0; y < height; y++){

                int idx = (width * y) + x;
                double a_value = (double)a.getPixel(x, y);
                double b_value = (double)b.getPixel(x, y);

                //FRET　計算
                if(a_value == 0){
                    a_value = 1;
                    b_value = 0;
                }

                value = ((double)b_value / (double)(a_value + b_value)); //FrameRatio2同様の計算

                if(((a_value < bitMaxValue / 100) && (b_value < bitMaxValue / 100))){
                    value = 0;
                }

                //int buff_value_i = (int)(value * 16777215);

                int i_value = (int)(value * 16777215); //RGB(24)
                projection_pixel_array_i[idx] = i_value;

                short s_value = (short)(value * 65535);//16bit
                projection_pixel_array_s[idx] = s_value;

                byte byte_value = (byte)(value * 255);//8bit
                projection_pixel_array_b[idx] = byte_value;
            }

        }
        if(a.getBitDepth() == 24){
            return_imp.setPixels(projection_pixel_array_i);
            return_imp.setMinAndMax(0.0,16777215);
        }else if(a.getBitDepth() == 16){
            return_imp.setPixels(projection_pixel_array_s);
            return_imp.setMinAndMax(0.0,65535);

        }else if(a.getBitDepth() == 8){
            return_imp.setPixels(projection_pixel_array_b);
            return_imp.setMinAndMax(0.0,255);

        }

        //System.out.println("bit:" + return_imp.getMax());
        return return_imp;

    }
}
