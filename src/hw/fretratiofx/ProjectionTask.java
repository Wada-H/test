package hw.fretratiofx;

import ij.ImagePlus;
import ij.plugin.ZProjector;
import javafx.concurrent.Task;

public class ProjectionTask extends Task<ImagePlus> {
    ImagePlus imp;

    public ProjectionTask(ImagePlus image){
        imp = image;
    }

    @Override
    protected ImagePlus call() throws Exception {

        ImagePlus pro_max;

        int nZ = imp.getNSlices();

        ZProjector zp = new ZProjector(imp);
        int projMethod = 1; //0:ave, 1:max, 2:min, 3:sum, 4:sd, 5:median

        zp.setStartSlice(1);
        zp.setStopSlice(nZ);
        zp.setMethod(projMethod);
        zp.doHyperStackProjection(true);
        pro_max = zp.getProjection();
        //imp.setImage(pro_max);

        updateValue(pro_max);
        return pro_max;
    }


}
