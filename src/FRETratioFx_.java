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
20190403 ImageJ 1.52nでのduplicate()仕様変更による修正 *AutoCut, FRETratioFxUI
20190819 HSB colorを用いたratio表示(IMD)を追加。できああがる画像はRGBとなる。
20191113 HSB colorの改良 brightness 0.1以上を用いて枚数を数え、これで各累計値を割る。ROIを設定することでその中の最大最小を用いてRatioを決める
20191114 T軸に対して並列に計算することで処理速度を改善。
    *問題点発覚 - stack全体としての最大輝度最小輝度をみる必要があるのではないかということ。
        ->色々試すも結局ちらつくので保留。あとはHSBprojection後に全体の輝度を揃えるようなやりかたか。(AutoEnhance的な)
20240905 AutoCutBのmod判定の方法改善と、Channel&Lutがcloseしない問題の解消。
20240913 Lutselectorの改良。ただし、ImageJ側でフォーカスの扱いがうまくいかない場合に変なことになる。これはかなり頻繁に起こる。pluginが原因の可能性もあるので注意

@author    hwada

*/


import hw.fretratiofx.FRETratioFxUI;
import hw.fretratiofx.LutSelectorFRET;
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

    static String version = "20240913";
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
            LutSelectorFRET lutl = new LutSelectorFRET();//起動はするが、channel違いの場合エラーが出る。当たり前だが、、、

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
