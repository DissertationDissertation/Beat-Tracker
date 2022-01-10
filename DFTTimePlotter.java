package fourier;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartFrame;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

public class DFTTimePlotter {

	public static void main(String args[]) {

		int maxNumberofSamples = 3000;
		int stepSize = 1;

		double[] totalTimes = new double[maxNumberofSamples / stepSize];

		for (int i = 0; i < maxNumberofSamples; i+=stepSize) {

			int nO_samples = i;

			double[] data = new double[nO_samples];
			double[] output = new double[nO_samples];


			// fills the data with random values
			for (int j = 0; j < nO_samples; ++j) {
				data[j] = Math.random();
			}


			double real_accumulator = 0;
			double complex_accumulator = 0;

			// start timer
			long startTime = System.nanoTime();

			// do the DFT
			for (int k = 0; k < nO_samples / 2; ++k) {
				for (int s = 0; s < nO_samples; ++s) {
					real_accumulator += data[s] * Math.cos(2 * Math.PI * k * s / nO_samples);
					complex_accumulator += data[s] * Math.sin(2 * Math.PI * k * s / nO_samples);
				}

				output[k] = Math.pow(real_accumulator, 2) + Math.pow(complex_accumulator, 2);

				real_accumulator = 0;
				complex_accumulator = 0;
			}

			// stop timer
			long endTime = System.nanoTime();

			totalTimes[i] = (double)((endTime - startTime) / 1000000.0);

			System.out.println(i);

		}

		// create an xy series
		XYSeries series1 = new XYSeries("Simple DFT calculation time", false);

		// load up the data into the series
		for (int i = 0; i < totalTimes.length; ++i) {
			series1.add(i * stepSize, totalTimes[i]);
		}

		// display the xy series
		XYSeriesCollection outputDataset = new XYSeriesCollection();
		outputDataset.addSeries(series1);
		JFreeChart chart = ChartFactory.createXYLineChart("Time Needed to Take A Simple DFT of Signals with Varying Lengths", "Number of Samples in Signal", "Time (millisecnds)", outputDataset);
		ChartFrame frame2 = new ChartFrame("This title doesn't matter", chart);
		frame2.setVisible(true);
		frame2.setSize(1500, 500);

	}

}
