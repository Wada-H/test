package hw.fretratiofx;

import ij.IJ;
import ij.ImageListener;
import ij.ImagePlus;
import ij.WindowManager;
import ij.plugin.frame.Channels;
import ij.plugin.frame.PlugInDialog;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.Properties;


//20141202~
//bio-formatで読まれたImagePlus画像よりmetadataを解釈してLutを決定、変更するプログラム（またはプラグイン）とする
//

public class LutSelectorFRET extends PlugInDialog implements MouseListener, WindowListener, ImageListener,ItemListener{
	ImagePlus imp = null;
	Properties properties = null;
	int ch = 0;
	byte[][] lut_byte = null;
	
	String[] luts = null;
	String[] default_luts = {"Blue","Green","Magenta","Grays","Grays","Grays","Grays","Grays","Grays","Grays"};
	String[] original_luts = null;
	
	JPanel basePanel;	
	JPanel[] selector_panel;
	JComboBox[] channel_luts;
	JCheckBox[] channel_checkbox;
	JLabel[] channel_label;
	
	public LutSelectorFRET(){
		super("Channel&Luts");
		luts = IJ.getLuts();
		WindowManager.addWindow(this);

				
	}



	public void setImagePlus(ImagePlus ip){
		imp = ip;
		ch = imp.getNChannels();

		
		if(imp.getOriginalFileInfo() != null){
			lut_byte = imp.getOriginalFileInfo().channelLuts;
		}
		
		System.out.println("ch:" + ch);

		
	}

	public void test(){
		//System.out.println(imp.getProperties());
		
		String[] lut = IJ.getLuts(); //lutsの種類を取得
		//System.out.println(lut[0]);
		
		
	}
	
	public void close() {
		super.close();

	}
	
	public void createPanel(){ //GUIの構築
		int x = imp.getWindow().getX();
		int y = imp.getWindow().getY();
	
		
		if(ch > 0){ //channelが指定されていればしょりする。

			
			basePanel = new JPanel();
			basePanel.setLayout(new GridLayout(ch, 1));
			basePanel.setAlignmentX(CENTER_ALIGNMENT);
			basePanel.setBorder(new EmptyBorder(10,10,10,10));

			channel_luts = new JComboBox[ch];
			channel_label = new JLabel[ch];
			selector_panel = new JPanel[ch];
			
			for(int i = 0; i < ch; i++){
				selector_panel[i] = new JPanel();
				selector_panel[i].setLayout(new BoxLayout(selector_panel[i], BoxLayout.X_AXIS)); 
				imp.setC(i+1);
				channel_luts[i] = new JComboBox();
				channel_luts[i].addItemListener(this);
				channel_label[i] = new JLabel("Channel " + (i+1));
				channel_label[i].setAlignmentX(0.5f);

				for(int n = 0; n < luts.length; n++){
					channel_luts[i].addItem(luts[n]);
				}
				

				if(ch == 1){
					channel_luts[i].setSelectedItem("Grays");
					IJ.doCommand("Grays");

				}else if(ch == 2){
					channel_luts[i].setSelectedItem(default_luts[i+1]);
					IJ.doCommand(default_luts[i+1]);

				}else if(ch >= 3){
					channel_luts[i].setSelectedItem(default_luts[i]);
					IJ.doCommand(default_luts[i]);
	
				}
				
				selector_panel[i].add(channel_label[i]);
				selector_panel[i].add(channel_luts[i]);
				
				basePanel.add(selector_panel[i]);
			}
			this.add(basePanel);

			//this.setBounds(100, 100, (channel_luts[0].getWidth() + channel_label[0].getWidth()), (50 * ch + 10));
			this.pack();
			this.setResizable(false);
			this.setLocation(x, (y - this.getHeight()));
			this.setVisible(true);
			imp.setC(1);
		}else{
			return;
		}
		
	}
	
	public void combine(Channels channelsTool){
		if(ch > 0){ //channelが指定されていればしょりする。
			
			basePanel = new JPanel();
			basePanel.setPreferredSize(new Dimension(200, 100));
			basePanel.setLayout(new GridLayout(2, ch));

			channel_luts = new JComboBox[ch];
			channel_checkbox = new JCheckBox[ch];
			
			for(int i = 0; i < ch; i++){
				channel_luts[i] = new JComboBox();
				channel_checkbox[i] = new JCheckBox(i + "ch");;
				
				channel_checkbox[i].setText(i + "ch");
				channel_checkbox[i].setSelected(true);
				channel_luts[i].addItem("");
				
				basePanel.add(channel_checkbox[i]);
				basePanel.add(channel_luts[i]);
			}
			channelsTool.add(basePanel);
			
		}else{
			return;
		}
		
	}
	

	
	public void setDefault(){
		for(int i = 0; i > ch; i++){
			if(ch == 1){
				IJ.doCommand("Grays");
			}else if(ch == 2){
				IJ.doCommand(default_luts[i+1]);

			}else if(ch >=3){
				IJ.doCommand(default_luts[i]);
				
			}
		}
	}
	
	public void setOriginal(){
		for(int i = 0; i > ch; i++){
			if(ch == 1){
				IJ.doCommand("Grays");
			}else if(ch == 2){
				IJ.doCommand(original_luts[i+1]);

			}else if(ch >=3){
				IJ.doCommand(original_luts[i]);
				
			}
		}	
	}
	
	
	public void changeImage(ImagePlus ip){ //反映させるImagePlusを変更させたいとき
		imp = ip;
	}




	@Override
	public void imageOpened(ImagePlus imp) {
		// TODO Auto-generated method stub
		
	}



	@Override
	public void imageClosed(ImagePlus imp) {
		// TODO Auto-generated method stub
		
	}



	@Override
	public void imageUpdated(ImagePlus imp) {
		// TODO Auto-generated method stub
		
	}



	@Override
	public void windowActivated(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}



	@Override
	public void windowClosed(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}



	@Override
	public void windowClosing(WindowEvent e) {
		dispose();
		// TODO Auto-generated method stub
		
	}



	@Override
	public void windowDeactivated(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}



	@Override
	public void windowDeiconified(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}



	@Override
	public void windowIconified(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}



	@Override
	public void windowOpened(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}




	@Override
	public void mouseClicked(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}



	@Override
	public void mouseEntered(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}



	@Override
	public void mouseExited(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}



	@Override
	public void mousePressed(MouseEvent e) {
		
		// TODO Auto-generated method stub
		
	}



	@Override
	public void mouseReleased(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}




	@Override
	public void itemStateChanged(ItemEvent e) {
		for(int c = 0; c < ch; c++){ //こうかくならItemListenerでもいけるが、、、
			
			if(e.getSource().equals(channel_luts[c])){

				imp.setC(c+1);

				String selectedlut = channel_luts[c].getSelectedItem().toString();
				//System.out.println(selectedlut);

				/* IJ.doCommand以外で変更できないものか？

				Color color = Color.getColor(selectedlut);

				String c_name = imp.getLuts().toString();
				c_name = selectedlut;
				System.out.println(c_name);
				
				//LUT lut = LUT.createLutFromColor(color);
				
				//imp.getProcessor().setLut(lut);

				*/
				IJ.doCommand(selectedlut);
			}
		}		
	}



	
}