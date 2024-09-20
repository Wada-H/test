package hw.fretratiofx;

import ij.*;
import ij.io.FileInfo;
import ij.plugin.CompositeConverter;
import ij.plugin.LutLoader;
import ij.plugin.frame.PlugInFrame;
import ij.process.LUT;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.Arrays;
import java.util.Properties;


//20141202~
//bio-formatで読まれたImagePlus画像よりmetadataを解釈してLutを決定、変更するプログラム（またはプラグイン）とする
//
//20240905バージョンに変更
//

public class LutSelectorFRET extends PlugInFrame implements MouseListener, WindowListener, ImageListener, ItemListener {
	private static final String[] DEFAULT_LUTS = {"Blue", "Green", "Magenta", "Grays", "Grays", "Grays", "Grays", "Grays", "Grays", "Grays"};
	private static final String[] DEFAULT_RGB = {"Red", "Green", "Blue"};

	private ImagePlus imp = null;
	private CompositeImage cImp = null;
	private Properties properties = null;
	private int ch = 0;
	private boolean rgb = false;

	private String[] luts = null;
	private JPanel basePanel;
	private JPanel[] selectorPanel;
	private JComboBox<String>[] channelLuts;
	private JLabel[] channelLabel;

	public LutSelectorFRET() {
		super("Channel&Luts");

		luts = IJ.getLuts();
		imp = WindowManager.getCurrentImage();

		if (imp.getBitDepth() == 24) {//FRETratio用にRGBの場合は何もしないようにしてみる
			ImagePlus convImg = CompositeConverter.makeComposite(imp);

			imp.close();
			imp = new CompositeImage(convImg, IJ.COMPOSITE);
			imp.show();
			rgb = true;

		}else {

			ch = imp.getNChannels();
			int x = imp.getWindow().getX();
			int y = imp.getWindow().getY();

			if (ch > 0) {
				initializePanel();
				this.pack();
				this.setResizable(false);
				this.setLocation(x, (y - this.getHeight()));
				this.setVisible(true);
				imp.setC(1);
			}
		}
	}

	private void initializePanel() {
		basePanel = new JPanel();
		basePanel.setLayout(new GridLayout(ch, 1));
		basePanel.setAlignmentX(CENTER_ALIGNMENT);
		basePanel.setBorder(new EmptyBorder(10, 10, 10, 10));

		channelLuts = new JComboBox[ch];
		channelLabel = new JLabel[ch];
		selectorPanel = new JPanel[ch];

		for (int i = 0; i < ch; i++) {
			selectorPanel[i] = new JPanel();
			selectorPanel[i].setLayout(new BoxLayout(selectorPanel[i], BoxLayout.X_AXIS));
			imp.setC(i + 1);
			channelLuts[i] = new JComboBox<>();
			channelLuts[i].addItemListener(this);
			channelLabel[i] = new JLabel("Channel " + (i + 1));
			channelLabel[i].setAlignmentX(0.5f);

			for (String lut : luts) {
				channelLuts[i].addItem(lut);
			}

			setInitialLUT(i);

			selectorPanel[i].add(channelLabel[i]);
			selectorPanel[i].add(channelLuts[i]);
			basePanel.add(selectorPanel[i]);
		}

		this.add(basePanel);
	}

	private void setInitialLUT(int channelIndex) {
		if (rgb) {
			channelLuts[channelIndex].setSelectedItem(DEFAULT_RGB[channelIndex]);
		} else {
			if (ch == 1) {

				LUT[] buffLUT = imp.getLuts();
				String lutName = getLutName(buffLUT[channelIndex]);
				channelLuts[channelIndex].setSelectedItem(lutName);

			} else if (ch >= 2) {
				LUT[] buffLUT = ((CompositeImage)imp).getLuts();
				String lutName = getLutName(buffLUT[channelIndex]); //ここで"3-3-2 RGB.lut" が読めないと出る, 次は16 Colors.lutと。保存されてるStringとファイル名に齟齬があるようだ。

				channelLuts[channelIndex].setSelectedItem(lutName);

			}
		}
	}

	public String getLutName(LUT lut){

		String lutName = "";
		byte[] reds1 = new byte[256];
		byte[] greens1 = new byte[256];
		byte[] blues1 = new byte[256];

		lut.getReds(reds1);
		lut.getGreens(greens1);
		lut.getBlues(blues1);

		for (int i = 0; i < luts.length; i++) {
			LUT buff = getSelectedLut(luts[i]);
			byte[] reds2 = new byte[256];
			byte[] greens2 = new byte[256];
			byte[] blues2 = new byte[256];

			buff.getReds(reds2);
			buff.getGreens(greens2);
			buff.getBlues(blues2);

			if (Arrays.equals(reds1, reds2) && Arrays.equals(greens1, greens2) && Arrays.equals(blues1, blues2)) {
				lutName = luts[i];
				break;
			}

		}
		//System.out.println("LUT name = " + lutName);
		return lutName;
	}

	public void restart(){
		this.close();
		new LutSelectorFRET();
	}

	@Override
	public void imageOpened(ImagePlus imp) {}

	@Override
	public void imageClosed(ImagePlus imp) {}

	@Override
	public void imageUpdated(ImagePlus imp) {}

	@Override
	public void windowActivated(WindowEvent e) {
		imp = WindowManager.getCurrentImage();
		if(ch != imp.getNChannels()){
			ch = imp.getNChannels();
			restart();
		}else {

			if (imp.isComposite()) {
				cImp = (CompositeImage) imp;
				LUT[] luts = new LUT[cImp.getNChannels()];

				for (int c = 0; c < luts.length; c++) {
					String selectedLut = channelLuts[c].getSelectedItem().toString().replace(" ", "_");
					luts[c] = getSelectedLut(selectedLut);
				}
				cImp.setLuts(luts);
				cImp.updateAndDraw();
			} else {
				String selectedLut = channelLuts[0].getSelectedItem().toString().replace(" ", "_");
				imp.setLut(getSelectedLut(selectedLut));
				imp.updateAndDraw();
			}
		}
	}

	@Override
	public void windowClosed(WindowEvent e) {}

	@Override
	public void windowClosing(WindowEvent e) {
		dispose();
	}

	@Override
	public void windowDeactivated(WindowEvent e) {}

	@Override
	public void windowDeiconified(WindowEvent e) {}

	@Override
	public void windowIconified(WindowEvent e) {}

	@Override
	public void windowOpened(WindowEvent e) {}

	@Override
	public void mouseClicked(MouseEvent e) {}

	@Override
	public void mouseEntered(MouseEvent e) {}

	@Override
	public void mouseExited(MouseEvent e) {}

	@Override
	public void mousePressed(MouseEvent e) {}

	@Override
	public void mouseReleased(MouseEvent e) {}

	@Override
	public void itemStateChanged(ItemEvent e) {
		imp = WindowManager.getCurrentImage();

		for (int c = 0; c < ch; c++) {
			if (e.getSource().equals(channelLuts[c])) {
				imp.setC(c + 1);
				String selectedLut = channelLuts[c].getSelectedItem().toString();
				IJ.doCommand(selectedLut);
			}
		}
	}

	private LUT getSelectedLut(String lutName) {
		FileInfo fi = getBuiltInLut(lutName);
		LUT selectedLut = new LUT(fi.reds, fi.greens, fi.blues);
		if (fi.fileName == null) {
			String lutPath = IJ.getDir("luts") + lutName.replace(" ","_") + ".lut";
			selectedLut = LutLoader.openLut(lutPath);
		}
		return selectedLut;
	}

	private FileInfo getBuiltInLut(String name) {
		FileInfo fi = new FileInfo();
		fi.reds = new byte[256];
		fi.greens = new byte[256];
		fi.blues = new byte[256];
		fi.lutSize = 256;
		fi.fileName = null;
		if (name == null) return fi;

		int nColors = 0;
		switch (name.toLowerCase()) {
			case "fire":
				nColors = fire(fi.reds, fi.greens, fi.blues);
				break;
			case "grays":
				nColors = grays(fi.reds, fi.greens, fi.blues);
				break;
			case "ice":
				nColors = ice(fi.reds, fi.greens, fi.blues);
				break;
			case "spectrum":
				nColors = spectrum(fi.reds, fi.greens, fi.blues);
				break;
			case "3-3-2_rgb":
				nColors = rgb332(fi.reds, fi.greens, fi.blues);
				break;
			case "3-3-2 rgb":
				nColors = rgb332(fi.reds, fi.greens, fi.blues);
				break;
			case "red":
				nColors = primaryColor(4, fi.reds, fi.greens, fi.blues);
				break;
			case "green":
				nColors = primaryColor(2, fi.reds, fi.greens, fi.blues);
				break;
			case "blue":
				nColors = primaryColor(1, fi.reds, fi.greens, fi.blues);
				break;
			case "cyan":
				nColors = primaryColor(3, fi.reds, fi.greens, fi.blues);
				break;
			case "magenta":
				nColors = primaryColor(5, fi.reds, fi.greens, fi.blues);
				break;
			case "yellow":
				nColors = primaryColor(6, fi.reds, fi.greens, fi.blues);
				break;
			case "redgreen":
			case "red/green":
				nColors = redGreen(fi.reds, fi.greens, fi.blues);
				break;
		}

		if (nColors > 0) {
			if (nColors < 256) interpolate(fi.reds, fi.greens, fi.blues, nColors);
			fi.fileName = name;
		}
		return fi;
	}

	private int fire(byte[] reds, byte[] greens, byte[] blues) {
		int[] r = {0, 0, 1, 25, 49, 73, 98, 122, 146, 162, 173, 184, 195, 207, 217, 229, 240, 252, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255};
		int[] g = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 14, 35, 57, 79, 101, 117, 133, 147, 161, 175, 190, 205, 219, 234, 248, 255, 255, 255, 255};
		int[] b = {0, 61, 96, 130, 165, 192, 220, 227, 210, 181, 151, 122, 93, 64, 35, 5, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 35, 98, 160, 223, 255};
		return setColors(reds, greens, blues, r, g, b);
	}

	private int grays(byte[] reds, byte[] greens, byte[] blues) {
		for (int i = 0; i < 256; i++) {
			reds[i] = (byte) i;
			greens[i] = (byte) i;
			blues[i] = (byte) i;
		}
		return 256;
	}

	private int primaryColor(int color, byte[] reds, byte[] greens, byte[] blues) {
		for (int i = 0; i < 256; i++) {
			if ((color & 4) != 0) reds[i] = (byte) i;
			if ((color & 2) != 0) greens[i] = (byte) i;
			if ((color & 1) != 0) blues[i] = (byte) i;
		}
		return 256;
	}

	private int ice(byte[] reds, byte[] greens, byte[] blues) {
		int[] r = {0, 0, 0, 0, 0, 0, 19, 29, 50, 48, 79, 112, 134, 158, 186, 201, 217, 229, 242, 250, 250, 250, 250, 251, 250, 250, 250, 250, 251, 251, 243, 230};
		int[] g = {156, 165, 176, 184, 190, 196, 193, 184, 171, 162, 146, 125, 107, 93, 81, 87, 92, 97, 95, 93, 93, 90, 85, 69, 64, 54, 47, 35, 19, 0, 4, 0};
		int[] b = {140, 147, 158, 166, 170, 176, 209, 220, 234, 225, 236, 246, 250, 251, 250, 250, 245, 230, 230, 222, 202, 180, 163, 142, 123, 114, 106, 94, 84, 64, 26, 27};
		return setColors(reds, greens, blues, r, g, b);
	}

	private int spectrum(byte[] reds, byte[] greens, byte[] blues) {
		for (int i = 0; i < 256; i++) {
			Color c = Color.getHSBColor(i / 255f, 1f, 1f);
			reds[i] = (byte) c.getRed();
			greens[i] = (byte) c.getGreen();
			blues[i] = (byte) c.getBlue();
		}
		return 256;
	}

	private int rgb332(byte[] reds, byte[] greens, byte[] blues) {
		for (int i = 0; i < 256; i++) {
			reds[i] = (byte) (i & 0xe0);
			greens[i] = (byte) ((i << 3) & 0xe0);
			blues[i] = (byte) ((i << 6) & 0xc0);
		}
		return 256;
	}

	private int redGreen(byte[] reds, byte[] greens, byte[] blues) {
		for (int i = 0; i < 128; i++) {
			reds[i] = (byte) (i * 2);
			greens[i] = (byte) 0;
			blues[i] = (byte) 0;
		}
		for (int i = 128; i < 256; i++) {
			reds[i] = (byte) 0;
			greens[i] = (byte) (i * 2);
			blues[i] = (byte) 0;
		}
		return 256;
	}

	private void interpolate(byte[] reds, byte[] greens, byte[] blues, int nColors) {
		byte[] r = new byte[nColors];
		byte[] g = new byte[nColors];
		byte[] b = new byte[nColors];
		System.arraycopy(reds, 0, r, 0, nColors);
		System.arraycopy(greens, 0, g, 0, nColors);
		System.arraycopy(blues, 0, b, 0, nColors);
		double scale = nColors / 256.0;
		int i1, i2;
		double fraction;
		for (int i = 0; i < 256; i++) {
			i1 = (int) (i * scale);
			i2 = i1 + 1;
			if (i2 == nColors) i2 = nColors - 1;
			fraction = i * scale - i1;
			reds[i] = (byte) ((1.0 - fraction) * (r[i1] & 255) + fraction * (r[i2] & 255));
			greens[i] = (byte) ((1.0 - fraction) * (g[i1] & 255) + fraction * (g[i2] & 255));
			blues[i] = (byte) ((1.0 - fraction) * (b[i1] & 255) + fraction * (b[i2] & 255));
		}
	}

	private int setColors(byte[] reds, byte[] greens, byte[] blues, int[] r, int[] g, int[] b) {
		for (int i = 0; i < r.length; i++) {
			reds[i] = (byte) r[i];
			greens[i] = (byte) g[i];
			blues[i] = (byte) b[i];
		}
		return r.length;
	}
}