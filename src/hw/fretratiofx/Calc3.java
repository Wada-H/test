package hw.fretratiofx;

import ij.process.ByteProcessor;
import ij.process.ImageProcessor;

public class Calc3 extends Calc {

    double max_ratio = 1.0;
    double min_ratio = 0.0;


    @Override
    public void setRatio(double min, double max){
        min_ratio = min;
        max_ratio = max;
    }

    @Override
    public ImageProcessor getProcessor(ImageProcessor a, ImageProcessor b){
        //like MetaMorph's IMD(Intensity Modulated Display mode) -> LUTもそれようにLIMD1,LIMD2を用意した。20150313
        //Ratioの下限上限を決めることで、32intensityの分配を決めている
        //8bit固定で。

        int bit_depth = a.getBitDepth();
        int width = a.getWidth();
        int height = a.getHeight();

        double bitMaxValue = Math.pow(2.0, (double)bit_depth);
        //double for_calc_bit_depth = Math.pow(2.0, (double)bit_depth); bit数が上がるとループ数が増えて、使い物にならない
        double for_calc_bit_depth = 256;

        double divide_value = 8.0; //8色
        double divide_intensity = for_calc_bit_depth / divide_value;

        double max_value_a = a.getMax();
        double min_value_a = a.getMin();

        double max_value_b = b.getMax();
        double min_value_b = b.getMin();

        double sum_max_value = max_value_a + max_value_b;

        double min_value = 1.0;
        double max_value = 0.0;



        //ImageProcessor return_imp = a.duplicate().convertToByte(true); //8bit画像に変換
        ImageProcessor return_imp = new ByteProcessor(width,height);


        int[] projection_pixel_array_i = new int[width * height];
        short[] projection_pixel_array_s = new short[width * height];
        byte[] projection_pixel_array_b = new byte[width * height];

        double[] value_array = new double[width * height];

        double ratio = 0.0;
        double sum_int = 0.0;


		/*
		double[] ratio_array = new double[width*height];
		for(int x = 0; x < width; x++){ //最大、最小ratioを求める。

			for(int y = 0; y < height; y++){
				int idx = (width * y) + x;
		 		double a_value = (double)a.getPixel(x, y);
		 		double b_value = (double)b.getPixel(x, y);

		 		//FRET　計算
		 		if(a_value == 0){ //a = 0の時の対処。
		 			a_value = 1;
		 			b_value = 0;
		 		}
			 	ratio_array[idx] = (b_value / a_value); //ratio
			}
		}

		Arrays.sort(ratio_array);
		*/
        //double max_ratio = ratio_array[width*height-1] + 0.2; //自動で計算させるには輝度のデータと照らし合わせて使う必要がありそう
        //double min_ratio = ratio_array[0] + 0.2;
        //double max_ratio = 1.2;
        //double min_ratio = 0.2;

        double between_max_and_min_ratio = max_ratio - min_ratio;
        double unit_ratio = between_max_and_min_ratio / divide_value;
        double unit_value = sum_max_value / divide_intensity;

        //System.out.println("min,max : " + min_ratio + "," + max_ratio);
        //System.out.println("unit_ratio:" + unit_ratio);

        for(int x = 0; x < width; x++){

            for(int y = 0; y < height; y++){

                int idx = (width * y) + x;
                double a_value = (double)a.getPixel(x, y);
                double b_value = (double)b.getPixel(x, y);

                double value = 0.0;
                //FRET　計算
                if(a_value == 0){ //a = 0の時の対処。ドナー側が0である場合アクセプター側の輝点はnoiseと考える
                    a_value = 1;
                    b_value = 0;
                }
                ratio = (b_value / a_value); //ratio
                sum_int = b_value + a_value; //輝度の合計値
                //System.out.println("ratio:" + ratio + ",sum_int:" + sum_int);


                //20150316 輝度を分割ではなくratioを分割して色分け？ ->IMDは輝度で色分けしている感じ
                //以下 合計輝度を元にdivide_value　分割
			 	/*
			 	for(int i = 0; i < divide_value; i++){

					if(((sum_max_value/divide_value * i) < sum_int)&&(sum_int <= (sum_max_value / divide_value * (i+1)))){

			 			for(int n  = 0; n < divide_intensity; n++){

			 				if(((min_ratio + unit_ratio * n)< ratio)&&(ratio <= (min_ratio + unit_ratio*(n+1)))){

			 					value = (i * divide_intensity) + n;

			 				//}else if(ratio > (min_ratio + unit_ratio*(n+1))){ //範囲以外は使用しない値として破棄
			 				//	value = 255.0;
			 				//}else if(ratio <= min_ratio){
			 				//	value = 0.0;
			 				}

			 			}
			 		}
			 	}
			 	*/

                ///* 20150317 ratio分割したが、いまいち。IMDとはかけ離れた表示になる。
                //20150401 やはりこっちが正解と思われる。
                for(int i = 0; i < divide_value; i++){ //8色

                    if(((min_ratio + unit_ratio * i) < ratio)&&(ratio <= (min_ratio +  unit_ratio * (i+1)))){
                        for(int n = 0; n < divide_intensity; n++){ //32intensity
                            if((unit_value * n < sum_int)&&(sum_int <= (unit_value * (n+1)))){
                                value = (i * divide_intensity) + n;
                            }

                        }

                    }
                }
                //*/

                value_array[idx]  = value;
            }

        }

        for(int i = 0; i < value_array.length; i++){
            byte value = (byte)value_array[i];
            //double value = buff;
            //projection_pixel_array_i[i] = (int)value;
            //projection_pixel_array_s[i] = (short)value;
            projection_pixel_array_b[i] = value;
        }


        return_imp.setPixels(projection_pixel_array_b);
        return_imp.setMinAndMax(0.0,255);

        return return_imp;
    }
}
