package fourier;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartFrame;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

public class FourierTransform {

	static final int nO_samples = 1024;	// number of samples in the input window
	static final int sample_rate = 20;	// the sample rate of the input window in Hz

	// the next two arrays describe the input waves to be added and must always be of the same size
	static final int[] input_freq = new int[] {1, 2, 3, 4, 5, 6, 7}; // the input signals of sin wave to be created in Hz
	static final double[] input_amp = new double[] {16, 8, 4, 2, 1, 0.5, 0.25};	 // the amplitude of the sin waves respectively

	// an array to store the final signal (created to hold the input signal for frequency analysis)
	static final double [] final_input_y = new double[nO_samples];

	// this is what the deltaAngle variable is increased by in each iteration of winding around the polar graph
	static final double delta_angle_step = 0.0001;
	static final double max_delta_angle = Math.PI;


	public static void main(String[] args) {


		// create a data series to store the input signal data
		XYSeries inputDataSeries = new XYSeries("Input Signal", false);

		// create a data series to store the frequency response data
		XYSeries freqRespSeries = new XYSeries("Frequency Response", false);

		// use this variable to accumulate the sum of the input signals amplitude for each sample
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
			inputDataSeries.add(s / (double)nO_samples, amp_accumulator);

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


		// create a deltaAngle which represents the small angular distance between samples in the polar graph
		double delta_angle = 0;
		// create variables to store the cumulation of the x and y coordinates of every sample at each winding step
		double cum_x = 0;	// to be reset to zero after each wind
		double cum_y = 0;	// to be reset to zero after each wind

		// to hold the magnitude of the 'centre of mass' of the polar graph for each wind
		double centre_mass_mag = 0;	// to be set to zero after each wind

		// whilst delta angle is less than the defined max amount, continue winding and calculating freq response
		while (delta_angle < max_delta_angle) {
			for (int s = 0; s < nO_samples; ++s) {
				cum_x = cum_x + final_input_y[s] * Math.cos(delta_angle * s);
				cum_y = cum_y + final_input_y[s] * Math.sin(delta_angle * s);
			}

			// find the magnitude of the distance of the 'centre of mass' from the the origin
			centre_mass_mag = Math.sqrt((Math.pow(cum_x, 2) + Math.pow(cum_y, 2)) / Math.pow(nO_samples, 2));

			// add the X and Y coordinates to the series to create the frequency response graph
			freqRespSeries.add(delta_angle * sample_rate / (2 * Math.PI), centre_mass_mag);
			
			// increase the delta angle by the defined step amount to increase the wind slightly for next loop
			delta_angle = delta_angle + delta_angle_step;

			// reset the cumulative variables ready for the next wind
			cum_x = 0;
			cum_y = 0;
			// reset the centre of mass variable ready for the next wind
			centre_mass_mag = 0;
			
		}
		
		// create the data set
				XYDataset xyFreqRespData = new XYSeriesCollection(freqRespSeries);
				JFreeChart chart2 = ChartFactory.createXYLineChart("Frequency Response", "Frequency", "Amplitude", xyFreqRespData);
				// create and show the chart
				ChartFrame frame2 = new ChartFrame("Frequency Response Data", chart2);
				frame2.setVisible(true);
				frame2.setSize(1500, 500);

	}


}
