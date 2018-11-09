package hw.fretratiofx;
import ij.ImagePlus;
import ij.plugin.ZProjector;

public class ProjectionFRET {

	ImagePlus imp = null;

	public ProjectionFRET(ImagePlus p){
		imp = p;
	}
	
	public ImagePlus projectionHyper(){ //projection画像を作るだけ
		ImagePlus pro_max = null;
		
	    int nZ = imp.getNSlices();
	    
		ZProjector zp = new ZProjector(imp);
	    int projMethod = 1; //0:ave, 1:max, 2:min, 3:sum, 4:sd, 5:median

	    zp.setStartSlice(1);
	    zp.setStopSlice(nZ);
	    zp.setMethod(projMethod);
	    zp.doHyperStackProjection(true);
	    pro_max = zp.getProjection();
	    return pro_max;
	}

}