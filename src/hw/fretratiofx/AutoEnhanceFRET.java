package hw.fretratiofx;

import ij.ImagePlus;
import ij.process.ImageProcessor;


public class AutoEnhanceFRET {
	
	public AutoEnhanceFRET(){
		
		
	}
	
	public void enhance(ImagePlus imp){
		//imp.setSlice(1);

		ImagePlus roi_imp = imp.duplicate();

		int t = imp.getNFrames();
		int c = imp.getNChannels();
		int z = imp.getNSlices();
		int b = imp.getBitDepth();
		
		double first_mean_value_1ch = 0.0;
		double first_mean_value_2ch = 0.0;
		
		for(int ct = 0; ct < t; ct++){
			imp.setT(ct + 1);
			roi_imp.setT(ct + 1);
			for(int cz = 0; cz < z; cz++){
				roi_imp.setZ(cz + 1);
				for(int cc = 0; cc < c; cc++){
					roi_imp.setC(cc + 1);
					ImageProcessor buff_imp = roi_imp.getProcessor();
					ImagePlus buff_img = new ImagePlus();
					buff_img.setProcessor(buff_imp);
					double mean_value = buff_imp.getStatistics().mean;
					
					if((cc == 0)&&(cz == 0)&&(ct == 0)){
						first_mean_value_1ch = mean_value;
					}else if((cc == 1)&&(cz == 0)&&(ct == 0)){
						first_mean_value_2ch = mean_value;
					}

					int index = roi_imp.getCurrentSlice();
					ImageProcessor imp_p = imp.getStack().getProcessor(index);
					
					double m_value = 0.0;
					if(cc == 0){
						m_value = 1 / (mean_value / first_mean_value_1ch);
						imp_p.multiply(m_value);
					}else if(cc == 1){
						m_value = 1 / (mean_value / first_mean_value_2ch);
						imp_p.multiply(m_value);
					}
					System.out.println("index, mean, m_value:" + index +"," + mean_value + "," + m_value);
				}
			}
			
			
		}
		
		
	}
	
	
}