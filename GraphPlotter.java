package graphs;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartFrame;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

public class GraphPlotter {

	public static void main(String[] args) {

		double[] yData1 = {1.6716185999998717E-4, 7.026652599999341E-4, 0.0031332485999999863, 0.012781342000000378, 0.05396797399999982, 0.23525759049999975, 0.8331724344000014, 3.362337294, 13.212668119000014, 54.649355009999994, 213.43553301999998, 952.7786699, 3552.9625201999997, 14212.2324, 75151.678099};
		double[] xData1 = {4, 8, 16, 32, 64, 128, 256, 512, 1024, 2048, 4096, 8192, 16384, 32768, 65536};
		
		
		// create an xy series
		XYSeries series1 = new XYSeries("SMDFT calculation time", false);

		// load up the data into the series
		for (int i = 0; i < yData1.length; ++i) {
			if (xData1[i] == 0)
				series1.add(i, yData1[i]);
			else
				series1.add(xData1[i], yData1[i]);
		}

		
		// display the xy series
		XYSeriesCollection outputDataset = new XYSeriesCollection();
		outputDataset.addSeries(series1);
		JFreeChart chart = ChartFactory.createXYLineChart("Time for One DFT Calculation", "Number of Samples", "Time (millisecnds)", outputDataset);
		ChartFrame frame2 = new ChartFrame("This title doesn't matter", chart);
		frame2.setVisible(true);
		frame2.setSize(1500, 500);
		
	}

}
