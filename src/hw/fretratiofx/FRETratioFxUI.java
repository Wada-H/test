package hw.fretratiofx;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.plugin.HyperStackConverter;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.embed.swing.JFXPanel;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;

import java.awt.event.WindowListener;
import java.io.IOException;
import java.util.ArrayList;

public class FRETratioFxUI extends AnchorPane implements WindowListener {

    int chSize;
    int zSize;
    int tSize;
    int imgWidth;
    int imgHeight;
    int imgDepth;

    WindowListener[] windowListeners;

    String fileName;
    ImagePlus mainImage;
    ImagePlus processingImage;

    ImagePlus fixed_imp; //クリア用の元画像
    ImagePlus imp_max; // projection用
    ImagePlus imp_buffer; //projection前の画像保持用

    ImagePlus fret_imp;

    String imageFileDir;
    String imageFileName;

    Scene scene;
    JFXPanel jfxPanel;
    FXMLLoader loader;



    @FXML public Button bCalc; //計算用ボタン
    @FXML public Button bProjection; //projection用ボタン
    @FXML public Button bClear; //画像初期化用ボタン
    @FXML public CheckBox cbAutoCutBackground; //バックグランド差し引きようcheckbox
    @FXML public ChoiceBox<String> cbMethods;
    ObservableList<String> cbMethodsList = FXCollections.observableArrayList("Ave","Mod");
    @FXML public TextField tfAutoCut; //AutoCut用　拡張値入力領域
    @FXML public CheckBox  cbAutoEnhance; // AutoEnhance用checkbox
    @FXML public CheckBox  cbSubtract; //background差し引き方

    @FXML private ChoiceBox<String> cbFormula;
    ObservableList<String> cbFormulaList = FXCollections.observableArrayList("Obtain(divide 100 <=) then ch2 / (ch1 + ch2) * bitMaxValue.","ch2 / ch1 * 100 * exValue","Like IMD(should set Lut 'LIMD')");

    @FXML public Pane calc1Pane;
    @FXML public Pane calc2Pane;
    @FXML public Pane calc3Pane;

    @FXML public TextField extendValueField;
    @FXML public TextField minRatioField;
    @FXML public TextField maxRatioField;


    public FRETratioFxUI(ImagePlus img, String file_name){
        fileName = file_name;
        mainImage = img;
        fixed_imp = mainImage.duplicate();

        if(mainImage.getOriginalFileInfo() != null){
            imageFileName = mainImage.getOriginalFileInfo().fileName;
            imageFileDir = mainImage.getOriginalFileInfo().directory;

        }else{
            imageFileName = mainImage.getTitle();
            imageFileDir = System.getProperty("user.home");

        }

        // 何かの拍子でnullになると保存ができなくなるので保険 //
        if(imageFileName == null) {
            imageFileName = "NewData";
        }
        if(imageFileDir == null) {
            imageFileDir = "./";
        }
        //


        this.getBasicInformation();

    }


    public void getBasicInformation(){
        chSize = mainImage.getNChannels();
        zSize = mainImage.getNSlices();
        tSize = mainImage.getNFrames();
        imgWidth = mainImage.getWidth();
        imgHeight = mainImage.getHeight();
        imgDepth = mainImage.getBitDepth();

    }


    public void linkWindowListener(WindowListener[] wls){
        windowListeners = wls;
    }

    public void setWindowListeners(){
        for (WindowListener windowListener : windowListeners) {
            mainImage.getWindow().addWindowListener(windowListener);
        }
    }

    public void removeListener(){
        for (WindowListener windowListener : windowListeners) {
            mainImage.getWindow().removeWindowListener(windowListener);
        }
    }

    @FXML
    private void initialize(){
        ArrayList<Pane> paneList = new ArrayList<>();
        paneList.add(calc1Pane);
        paneList.add(calc2Pane);
        paneList.add(calc3Pane);

        cbFormula.setItems(cbFormulaList);
        cbFormula.setValue(cbFormulaList.get(0));
        cbFormula.getSelectionModel().selectedIndexProperty().addListener((index, oldValue, newValue) ->{
            paneList.get(oldValue.intValue()).setVisible(false);
            paneList.get(newValue.intValue()).setVisible(true);

        });

        cbMethods.setItems(cbMethodsList);
        cbMethods.setValue(cbMethodsList.get(1));

    }


    public JFXPanel getFXML(){
        Pane result; // = new Pane();
        jfxPanel = new JFXPanel();
        loader = new FXMLLoader();
        loader.setRoot(this);
        loader.setController(this);
        //loader.setController(new Test()); //こんな書き方でもいける。ただし、今回の場合は分離するほうが面倒
        try {
            //result = FXMLLoader.load(getClass().getResource(fileName));
            result = loader.load(getClass().getResourceAsStream(fileName));

            scene = new Scene(result,result.getPrefWidth(),result.getPrefHeight());
            jfxPanel.setScene(scene);

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return jfxPanel;
    }


    public ImagePlus fretCalc(int num){ //元はswingworker使用 -> Taskへ変更予定


        Service<ImagePlus> service  = new Service<ImagePlus>() {

            Task<ImagePlus> resultTask;
            @Override
            protected Task<ImagePlus> createTask() {
                resultTask = new CalcTask(processingImage, num);

                System.out.println("num : " + num);
                // ダイアログみないなのだして入力させる? -> IJ.getNumber()ではだめっぽい ->UI　panelを変更 してみるか//
                if(num == 1){
                    int exValue = Integer.valueOf(extendValueField.getText());
                    ((CalcTask) resultTask).setExValue(exValue);
                }else if(num == 2){
                    double min = Double.valueOf(minRatioField.getText());
                    double max = Double.valueOf(maxRatioField.getText());
                    ((CalcTask) resultTask).setRatio(min, max);
                }
                resultTask.run();

                return resultTask;
            };

        };

        service.start();
        ImagePlus return_imp = service.getValue();

        int z = mainImage.getNSlices();
        int t = mainImage.getNFrames();

        ImagePlus return_hyper;
        if((z == 1)&&(t == 1)){
            return_hyper = return_imp;
        }else if(return_imp.getBitDepth() == 24){ //RGBの場合どうする？20150316　未完

            return_hyper = HyperStackConverter.toHyperStack(return_imp, 3, z, t, "default", "color");

        }else{
            return_hyper = HyperStackConverter.toHyperStack(return_imp, 1, z, t, "default", "color");
        }

        return return_hyper;


    }



    @FXML
    private void calcButtonFunction(MouseEvent mouseEvent) throws IOException{
        Roi r = mainImage.getRoi();
        mainImage.killRoi();
        processingImage = mainImage.duplicate();
        mainImage.setRoi(r);
        processingImage.setRoi(r);

        if(mouseEvent.getClickCount() == 1) {

            ImagePlus buff = new ImagePlus();

            if (cbAutoCutBackground.isSelected() == true) {
                double ext_num = Double.valueOf(tfAutoCut.getText());

                AutoCut autoCut = new AutoCut(processingImage);
                autoCut.setSelfSlice(cbSubtract.isSelected());
                int methodOption = 0;
                if(cbMethods.getValue() == cbMethodsList.get(1)){
                    methodOption = 1;
                }
                autoCut.autocut(methodOption, ext_num);

                processingImage.setRoi(r);
            }

            if (cbAutoEnhance.isSelected() == true) {
                AutoEnhanceFRET autoEnhance = new AutoEnhanceFRET();
                autoEnhance.enhance(processingImage);
                processingImage.setRoi(r);
            }


            int selectedIndex = cbFormula.getSelectionModel().getSelectedIndex();
            fret_imp = fretCalc(selectedIndex);
            fret_imp.setCalibration(mainImage.getCalibration());
            fret_imp.setFileInfo(mainImage.getFileInfo());

            if (fret_imp.isVisible() == false) {
                fret_imp.show();
                fret_imp.setDisplayRange(0, fret_imp.getProcessor().getMax());

                System.out.println(fret_imp.isHyperStack());

                fret_imp.setRoi(r);
            }

            LutSelectorFRET lut_fret = new LutSelectorFRET();
            lut_fret.setImagePlus(fret_imp);
            lut_fret.createPanel();

        }else{
            System.out.println("Continuous click");
            mainImage.getWindow().requestFocus();
        }
    }

    @FXML
    private void projectionButtonFunction(MouseEvent mouseEvent) throws IOException{ //並列処理にすると、ボタン連打で処理が止まる。おそらくバッティングしてると思われる
        //連打防止策としてボタン不可にしてもできてしまう。やはりクリック数監視するべきか？
        //クリック監視しても起こる。どうやらダブルクリックになったときにmainImageがフォーカスが外れる。もしくはボタンそのものにフォーカスが行ってしまう。このあたりから改善できるかも。
        //mainImageにフォーカスを持っていくことで回避できた。

        int clickCount = mouseEvent.getClickCount();

        if(mouseEvent.getClickCount() == 1) {

            if (!mainImage.isVisible()) {
                mainImage = new ImagePlus();
                mainImage.setImage(fixed_imp);
                mainImage.show();
            }
            int cc = mainImage.getC();
            int ct = mainImage.getT();

            String bname = bProjection.getText();

            this.removeListener();
            if (bname.compareTo("Projection") == 0) {

                imp_buffer = mainImage.duplicate();
                //mainImage.setZ(mainImage.getNSlices()); //projection imageをmainImageにsetImageしたときに反映されない不具合があるため -> 1.52h21で解消
                imp_max = this.projectionHyper(mainImage);
                mainImage.setImage(imp_max);
                //this.projectionHyper(mainImage);
                mainImage.setC(cc);
                mainImage.setT(ct);

                bProjection.setText("Split-Z");

            } else if (bname.compareTo("Split-Z") == 0) {

                imp_max = mainImage.duplicate();

                mainImage.setImage(imp_buffer);


                if (mainImage.getNChannels() == 1) { //projection画像がc=1,z=1,t=n等の場合、ImageStackに変更されてしまうため、もう一度HyperStackとすることで対応する。
                    IJ.run(mainImage, "Stack to Hyperstack...", "display=Color");

                } else if (mainImage.getNSlices() == 1) {
                    int c = fixed_imp.getNChannels();
                    int z = fixed_imp.getNSlices();
                    int t = fixed_imp.getNFrames();
                    System.out.println(c);
                    mainImage = HyperStackConverter.toHyperStack(mainImage, c, z, t, "default", "color");

                }
                //mainImage.setZ(imp_buffer.getNSlices()); //mainImageにsetImageしたときに反映されない不具合があるため -> 1.52h21で解消
                mainImage.setC(cc);
                mainImage.setT(ct);

                bProjection.setText("Projection");

            } else {

            }

            mainImage.updateAndDraw();
            //this.setWindowListeners();
            mainImage.getWindow().addWindowListener(this);

        }else{
            System.out.println("Continuous click");
            mainImage.getWindow().requestFocus();
        }

    }

    private ImagePlus projectionHyper(ImagePlus imp){ //projection画像を作るだけ


        Service<ImagePlus> service  = new Service<ImagePlus>() {

            Task<ImagePlus> resultTask;
            @Override
            protected Task<ImagePlus> createTask() {
                resultTask = new ProjectionTask(imp);
                resultTask.run();

                return resultTask;
            };

        };

        service.start();
        return service.getValue();


        /* これでは動くが、別スレッドに感じないが、、、->連打で止まるので別スレッドと思われる。
        Task<ImagePlus> projectionTask = new ProjectionTask(imp);
        projectionTask.run();
        return projectionTask.getValue();
        */

        /*// これでもボタン連打で止まる。javafxの問題か？
        ProjectionFRET projectionFRET = new ProjectionFRET(imp);
        return projectionFRET.projectionHyper();
        */
    }




    @FXML
    public void clearButtonFunction(){


        IJ.showStatus("Clear the Image");

        ImagePlus b = fixed_imp.duplicate();
        mainImage.setImage(b); //dupulicationよりもとにもどしたい。

        if(imp_max != null){
            if(mainImage.getNChannels() == 1){ //projection画像がc=1,z=1,t=n等の場合、ImageStackに変更されてしまうため、もう一度HyperStackとすることで対応する。
                IJ.run(mainImage, "Stack to Hyperstack...", "display=Color");

            }else if(mainImage.getNSlices() == 1){
                int c = fixed_imp.getNChannels();
                int z = fixed_imp.getNSlices();
                int t = fixed_imp.getNFrames();
                System.out.println(c);
                mainImage = HyperStackConverter.toHyperStack(mainImage, c, z, t, "default", "color");

            }
        }

        imp_max = null;

        String bname = bProjection.getText();
        if(bname == "Split-Z"){
            setWindowListeners();
            bProjection.setText("Projection");
        }

    }


    public void close(){

        Platform.setImplicitExit(false);

    }


    @Override
    public void windowOpened(java.awt.event.WindowEvent e) {

    }

    @Override
    public void windowClosing(java.awt.event.WindowEvent e) {
        this.close();
        //this.removeListener();
        this.close();
    }

    @Override
    public void windowClosed(java.awt.event.WindowEvent e) {

    }

    @Override
    public void windowIconified(java.awt.event.WindowEvent e) {

    }

    @Override
    public void windowDeiconified(java.awt.event.WindowEvent e) {

    }

    @Override
    public void windowActivated(java.awt.event.WindowEvent e) {

    }

    @Override
    public void windowDeactivated(java.awt.event.WindowEvent e) {

    }
}
