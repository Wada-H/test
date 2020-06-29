package hw.fretratiofx;

import ij.process.ImageProcessor;

public class Calc2 extends Calc{

    int exValue = 1;

    @Override
    public void setExValue(int value){
        exValue = value;
    }

    @Override
    public String getTitle(){
        title = "_exv" + exValue;
        return title;
    }

    @Override
    public ImageProcessor getProcessor(ImageProcessor a, ImageProcessor b){


        int bit_depth = a.getBitDepth();
        int width = a.getWidth();
        int height = a.getHeight();

        double min_value = 1.0;
        double max_value = 0.0;

        ImageProcessor return_imp = a.duplicate();


        int[] projection_pixel_array_i = new int[width * height];
        short[] projection_pixel_array_s = new short[width * height];
        byte[] projection_pixel_array_b = new byte[width * height];

        //double[] value_array = new double[width * height];

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
                value = ((double)b_value / (double)a_value); //

                //value_array[idx]  = value;

                double buff_value = value * 100 * exValue;
                if(buff_value > Math.pow(2, bit_depth)){
                    buff_value = Math.pow(2, bit_depth) -1 ;
                }

                projection_pixel_array_i[idx] = (int)buff_value;
                projection_pixel_array_s[idx] = (short)projection_pixel_array_i[idx];
                projection_pixel_array_b[idx] = (byte)projection_pixel_array_s[idx];
            }

        }
/*
		for(int i = 0; i < value_array.length; i++){
			double buff = value_array[i];
			//double value = buff * exValue;

			int i_value = (int)(buff * exValue) ; //RGB(24)
			projection_pixel_array_i[i] = i_value;

			short s_value = (short)(i_value);//16bit
			projection_pixel_array_s[i] = s_value;

			byte byte_value = (byte)(s_value);//8bit
			projection_pixel_array_b[i] = byte_value;


			//projection_pixel_array_i[i] = (int)value;
			//projection_pixel_array_s[i] = (short)value;
			//projection_pixel_array_b[i] = (byte)value;

		}
*/

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

        return return_imp;
    }
}
