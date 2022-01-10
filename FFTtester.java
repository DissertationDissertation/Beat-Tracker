package fourier;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartFrame;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jtransforms.fft.DoubleFFT_1D;

public class FFTtester {

	static final int nSamples = 10000; // number of samples in the input window
	static final int sampleRate = 160; // the sample rate of the input window in Hz

	// the next two arrays describe the input waves to be added and must always be
	// of the same size
	static final double[] inputFreq = new double[] {3, 5, 6, 3, 4, 1}; // the input signals of sin wave to be created in Hz
	static final double[] inputAmp = new double[] { 18, 16, 3, 8, 4, 6, 1 }; // the amplitudes of the sin waves respectively
	static final double[] inputPha = new double[] { 0.5 , 0.1, 0, 0.5, 0.5, 0.2, 0.4}; // the phases of the sin waves respectively

	static double[] inputSignal = new double[nSamples];
	// create an input with a size equal to the amount of samples there are

	static double[] outputFFT = new double[nSamples];
	// create an output for the FFT with a size equal to the amount of samples there are

	static double[] phasesFFT = new double[nSamples];
	// create an output for the phases of the FFT with a size equal to the amount of samples there are


	static public void main(String[] args) {

		SignalProcessor signalProcess = new SignalProcessor(inputSignal.length);

		int loopNumber = 1;	// amount of times to test the FFT
		double averageTime = 0;	// store the average time it is taking to do an FFT

		for (int i = 0; i < loopNumber ; ++i) {

			double ampAccumulator = 0;
			// use this variable to accumulate the sum of the input signals amplitude for each sample
			// it must be reset to 0 for each new sample

			// create the signal specified
			for (int s = 0; s < nSamples; ++s) {

				for (int f = 0; f < inputFreq.length; ++f) {

					ampAccumulator += inputAmp[f] *  Math.cos( 2 * Math.PI * inputPha[f] + (Math.PI * 2 * inputFreq[f] * s) / sampleRate);
					// iterating through each input signal, accumulate their effect at each point

				}

				inputSignal[s] = ampAccumulator;
				// add the Y coordinates (signal's amplitude) to the final signal input

				ampAccumulator = 0;
				// reset the amp accumulator for the next sample
			}


			DoubleFFT_1D jTransformFFT = new DoubleFFT_1D(inputSignal.length);

			double[] complexInputSignal = new double[inputSignal.length * 2];

			
			for (int j = 0; j < inputSignal.length; ++j) {
				complexInputSignal[j * 2] = inputSignal[j];
				complexInputSignal[j * 2 + 1] = 0;
			}

			long startTime = System.nanoTime();

			jTransformFFT.complexForward(complexInputSignal);
			for (int j = 0; j < inputSignal.length; ++j)
				outputFFT[j] = Math.pow(complexInputSignal[j*2], 2) + Math.pow(complexInputSignal[j*2+1], 2);



//			 outputFFT = signalProcess.FFT(inputSignal);



			//phasesFFT = signalProcess.getPhases(0.01);



			long endTime = System.nanoTime();

			averageTime += ((endTime - startTime) - averageTime) / (i + 1);

		}


		System.out.println(loopNumber + " FFt(s) took and average of " + (double) averageTime / 1000000 + " milliseconds to compute");
		System.out.println("We want it to take a maximum of " + (1000 * (double)nSamples / 44100) / 2 + " milliseconds seconds to compute");





		// DISPLAYING THE DATA

		// create a data series to store the signal input
		XYSeries inputSeries = new XYSeries("Final Signal", false);

		// create a data series to store the fft output
		XYSeries outputSeries = new XYSeries("Frequency Response", false);

		// create a data series to store the pha
		XYSeries phaseSeries = new XYSeries("Phase Reponse", false);

		// load up the data series
		for (int s = 0; s < nSamples; ++s) {
			outputSeries.add((double) (s * sampleRate)/ nSamples, outputFFT[s]);	
			inputSeries.add(s, inputSignal[s]);
			phaseSeries.add((double) (s * sampleRate)/ nSamples, phasesFFT[s]);
		}


		//display the input signal series
		XYDataset inputDataset = new XYSeriesCollection(inputSeries);
		JFreeChart chartSignal = ChartFactory.createXYLineChart("Generated Signal Data", "samples", "Amplitude", inputDataset);
		ChartFrame frame1 = new ChartFrame("Signal Input Data", chartSignal);
		frame1.setVisible(true);
		frame1.setSize(1500, 500);


		// display the FFT data series
		XYDataset outputDataset = new XYSeriesCollection(outputSeries);
		JFreeChart chartFourier = ChartFactory.createXYLineChart("Fourier Data", "frames", "Power", outputDataset);
		ChartFrame frame2 = new ChartFrame("Fourier Output Data", chartFourier);
		frame2.setVisible(true);
		frame2.setSize(1500, 500);

		// display the phases
		XYDataset phaseDataset = new XYSeriesCollection(phaseSeries);
		JFreeChart chartPhase = ChartFactory.createXYLineChart("Phase Data", "frames", "Angle / 2pi", phaseDataset);
		ChartFrame frame3 = new ChartFrame("Fourier Output Data", chartPhase);
		frame3.setVisible(false);
		frame3.setSize(1500, 500);


	}

}
