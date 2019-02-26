/********************************************************/
/*           FRET画像の比を求める       */
/********************************************************/

/*
FRETratio_.java

概要
顕微鏡からのFRET画像をRatio画像に変換するためのplugin
defaultがcztのはずなので、そのつもりで
panelに計算方法を変更できるような仕掛け(commbobox等？)
panelにbackguroundを差し引く用のcheckbox(基本的にはONで)
元画像とは別に画像を開く
projection機能も(max and mean)
Calcボタンを押すたびに元画像と、式をもとに新しい画像を作る

更新履歴
20150113 project start
20180921 refactoring start, UIをJavaFxに変更予定
20181107 UIの変更ほぼ完成, これに伴うrefactoringを行う予定
20190226 AutoCut部分にMethodの追加とModの修正

@author    hwada

*/





import hw.fretratiofx.FRETratioFxUI;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.ImageCanvas;
import ij.gui.Toolbar;
import ij.plugin.frame.PlugInFrame;
import javafx.embed.swing.JFXPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

public class FRETratioFx_ extends PlugInFrame implements WindowListener {

    static String version = "20190226";
    FRETratioFxUI ui;

    ImagePlus mainImage; // 元画像 または選択中の画像


    ImageCanvas ic;
    String uiFileName;


    private int chSize;
    private int zSize;
    private int tSize;
    private int imgDepth;

    private int imgWidth;
    private int imgHeight;

    // Panel //
    Point ij_location;


    public FRETratioFx_() {
        super("FRETratioFx ver." + version);


        if(checkImage()){
            this.getBasicInformation(mainImage);

            this.createPanelFx();

        }else{
            IJ.noImage();
            return;
        }
    }


    public boolean checkImage(){
        boolean b = false;


        ImagePlus checkImage = WindowManager.getCurrentImage();
        if(checkImage == null){
            b = false;
        }else{
            if(checkImage.isHyperStack() == false){
                IJ.run(checkImage, "Stack to Hyperstack...", ""); //なんかgetWindow().toFront()でエラー出る
            }
            mainImage = WindowManager.getCurrentImage();
            ic = mainImage.getCanvas();
            this.setListener();
            b = true;
        }


        return b;
    }

    public void getBasicInformation(ImagePlus img){
        chSize = img.getNChannels();
        zSize = img.getNSlices();
        tSize = img.getNFrames();
        imgWidth = img.getWidth();
        imgHeight = img.getHeight();
        imgDepth = img.getBitDepth();

    }



    private void createPanelFx(){

        uiFileName = "ui.fxml"; //ここを画像のタイプで変更すればよい？

        ui = new FRETratioFxUI(mainImage, uiFileName);
        //System.out.println(mainImage.getWindow().getWindowListeners().length);

        ui.linkWindowListener(mainImage.getWindow().getWindowListeners());
        JFXPanel jfxPanel = ui.getFXML();
        IJ.setTool(Toolbar.LINE);
        this.setPanelPosition();
        this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        this.setBounds(100,100,jfxPanel.getWidth(),jfxPanel.getHeight());
        this.add(jfxPanel);
        this.pack(); //推奨サイズのｗindow
        this.setVisible(true);//thisの表示

    }

    private void setPanelPosition(){
        ij_location = IJ.getInstance().getLocation(); //imagejのtoolboxの開始座標
        int ij_height = IJ.getInstance().getHeight();
        this.setLocation(ij_location.x, ij_location.y + ij_height);
    }

    private void setListener(){
        mainImage.getWindow().addWindowListener(this);

    }

    private void removeListener(){
        if(mainImage.isVisible()) {
            mainImage.getWindow().removeWindowListener(this);
        }
    }


    @Override
    public void windowOpened(WindowEvent e) {

    }

    @Override
    public void windowClosing(WindowEvent e) {
        ui.close();
        this.removeListener();
        this.close();

    }

    @Override
    public void windowClosed(WindowEvent e) {

    }
}
