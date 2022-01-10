package fourier;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartFrame;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

public class MatrixDFTOrigional {

	static final int nO_samples = 2048 * 2 * 2 * 2; // number of samples in the input window
	static final int sample_rate = 30; // the sample rate of the input window in Hz

	// the next two arrays describe the input waves to be added and must always be
	// of the same size
	static final int[] input_freq = new int[] { 1, 2, 3, 4, 5, 6, 7 }; // the input signals of sin wave to be created in
	// Hz
	static final double[] input_amp = new double[] { 16, 8, 4, 2, 1, 0.5, 0.25 }; // the amplitude of the sin waves
	// respectively

	// an array to store the final signal (created to hold the input signal for
	// frequency analysis)
	static final double[] final_input_y = new double[nO_samples];

	// create a matrix to store the fourier transfrom matrix
	static final double[][][] fourier_transfrom_matrix = new double[nO_samples/2][nO_samples][2];

	// create a matrix to store the complex result of the fourier transform
	static final double[][] fourier_output = new double[nO_samples][2];

	public static void main(String[] args) {

		// create a data series to store the input signal data
		XYSeries inputDataSeries = new XYSeries("Input Signal", false);

		// create a data series to store the frequency response data
		// XYSeries freqRespSeries = new XYSeries("Frequency Response", false);

		// use this variable to accumulate the sum of the input signals amplitude for
		// each sample
		// it must be reset to 0 for each new sample
		double amp_accumulator = 0;

		for (int s = 0; s < nO_samples; ++s) {
			for (int f = 0; f < input_freq.length; ++f) {
				// iterating through each input signal, accumulate their effect at each point
				amp_accumulator += input_amp[f] * Math.sin((Math.PI * 2 * input_freq[f] * s) / sample_rate);
			}

			// include a Hamming window on data
			amp_accumulator = amp_accumulator * (0.54 - 0.46 * Math.cos((2 * Math.PI * s) / nO_samples));

			// add the X and Y coordinates to the series to create the graph
			inputDataSeries.add(s / (double) sample_rate, amp_accumulator);

			// add the Y coordinates (signal's amplitude) to the final signal array
			final_input_y[s] = amp_accumulator;

			// reset the amp accumulator for the next sample
			amp_accumulator = 0;
		}

		// create the data set
		XYDataset xyDataset = new XYSeriesCollection(inputDataSeries);
		JFreeChart chart = ChartFactory.createXYLineChart("Final Input Data", "Time", "Amplitude", xyDataset);
		// create and show the chart
		ChartFrame frame1 = new ChartFrame("Final Input Data", chart);
		frame1.setVisible(true);
		frame1.setSize(1500, 500);

		
		int loopNumber = 1;
		double averageTime = 0;
		

		for (int i = 0; i < loopNumber; ++i) {

			long startTime = System.nanoTime();

			// fill the fourier transform table
			for (int k = 0; k < nO_samples / 2; ++k) {
				for (int s = 0; s < nO_samples; ++s) {
					fourier_transfrom_matrix[k][s][0] = Math.cos(2 * Math.PI * k * s / nO_samples);
					fourier_transfrom_matrix[k][s][1] = Math.sin(2 * Math.PI * k * s / nO_samples);
				}
			}

			// multiply the fourier matrix by the data vector
			double real_accumulator = 0;
			double complex_accumulator = 0;

			for (int k = 0; k < nO_samples / 2; ++k) {
				for (int s = 0; s < nO_samples; ++s) {
					real_accumulator += final_input_y[s] * fourier_transfrom_matrix[k][s][0];
					complex_accumulator += final_input_y[s] * fourier_transfrom_matrix[k][s][1];
				}

				fourier_output[k][0] = real_accumulator;
				fourier_output[k][1] = complex_accumulator;

				real_accumulator = 0;
				complex_accumulator = 0;
			}

			long endTime = System.nanoTime();

			averageTime += ((endTime - startTime) - averageTime) / (i + 1);

		}

		System.out.println(loopNumber + " FFt(s) took and average of " + (double) averageTime / 1000000 + " milliseconds to compute");

		// create a data series to store the input signal data
		XYSeries fourierDataSeries = new XYSeries("Fourier Signal", false);

		for (int k = 0; k < nO_samples / 2; ++k) {
			fourierDataSeries.add((double)k * sample_rate / nO_samples, Math.sqrt(Math.pow(fourier_output[k][0], 2) + Math.pow(fourier_output[k][1], 2)) / nO_samples);
		}

		// create the data set
		XYDataset xyFourierDataset = new XYSeriesCollection(fourierDataSeries);
		JFreeChart chartFourier = ChartFactory.createXYLineChart("Fourier Data", "Time", "Amplitude", xyFourierDataset);
		// create and show the chart
		ChartFrame frame2 = new ChartFrame("Fourier Input Data", chartFourier);
		frame2.setVisible(true);
		frame2.setSize(1500, 500);

	}

}
