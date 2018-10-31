package behavior.gui;

import java.awt.BorderLayout;

import javax.swing.JFrame;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;

public class HCGraph implements Runnable {
	JFrame frame;
	JFreeChart chart;
	ChartPanel cpanel;
	DefaultCategoryDataset dataset;
	
	public HCGraph(int cage, int allCage, boolean isHC1) {
		frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		if (allCage == 4) {
			frame.setBounds(1100, 20+250*cage, 300, 250);
		}else {
			if (cage < 4)
				frame.setBounds(1150, 20+250*cage, 280, 250);
			else
				frame.setBounds(1430, 20+250*(cage-4), 280, 250);
		}
		
		dataset = new DefaultCategoryDataset();
		chart = ChartFactory.createBarChart("Cage" + (cage+1), "time(hour)", isHC1 ? "distance" : "mean particle", dataset, PlotOrientation.VERTICAL, true, false, false);
		cpanel = new ChartPanel(chart);
		
		frame.add(cpanel, BorderLayout.CENTER);
		frame.setVisible(true);
	}
	
	
	@Override
	public void run() {
		
		//‰Šú‰»
		for (int i = 0; i < 12; i++) {
			dataset.setValue(null, "", String.valueOf(i+1));
		}
		
		while (true) {
			
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
	}
	
	public void setData(double value, int time) {
		dataset.addValue(value, "", String.valueOf(time));
	}
	
	public void clearData() {
		dataset.clear();
	}
	
	public void setVisible(boolean b) {
		frame.setVisible(b);
	}
	
	
	
	

}
