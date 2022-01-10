package fourier;

import java.util.Arrays;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartFrame;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

public class FFTtester2 {

	static int nSamples = 2048;

	static final double cyclesInWindow = 8.6;
	// this tells us how many cycles will appear within the window
	// that will then be sent to the FFT object

	static final double phase = 0.3;
	// this alters the phase of the data

	static final double amplitude = 5;
	// this tells us the amplitude of the peaks in the data

	// where the dft algorithm will take the 'maximum' sample to be
	static double dftSearch = 7.3;

	static double dftSearchStart = 5;
	static double dftSearchEnd = dftSearchStart * 2;

	// we will do 200 DFTs in between the search area
	static double[] dftSearchArea = new double[2000];

	// create a signal for the fft to process
	static double[] signal = new double[nSamples];

	// create an output for the FFT with a size equal to the amount of samples there are
	static double[] outputDFT = new double[dftSearchArea.length];

	static double[] outputHarmonicFFT;

	// create another output for the phases of the dft
	static double[] phasesDFT = new double[dftSearchArea.length];

	static double[] phasesFFT = new double[nSamples];
	// create an output for the phases of the FFT with a size equal to the amount of samples there are

	static int[] indexOfSpikes;
	// creates an array telling which the sample indexes the spikes should occur in the signal

	static SignalProcessor signalProcess = new SignalProcessor(nSamples);
	// create a signal processor for the FFT result


	static DFTObject dft = new DFTObject();

	static DFTSearch dftSearcher = new DFTSearch();

	static long startTime;
	static long endTime;
	static double averageTimeSimpleDFT;
	static double averageTimeDFT;
	static double averageTimeHarmonicFFT;


	static int numOfLoops = 1;

	public static void main(String[] args){

		double errorInPhaseWindowed = 0;
		double errorInPhaseNotWindowed = 0;
		double dFTseachError = 0;
		double harmonicSeachError = 0;

		for (int i = 0; i < numOfLoops; ++i) {

			// reset the signal
			for (int j = 0; j < signal.length; ++j) {
				signal[j] = 0;
			}

			// 	choices of spike placement:
			// regularSpikes();					// < 	 |	 |	 |	 |	 |	 |
			//	doubleBunchedSpikes();				// <	||	||	||	||	||	||
				tripleBunchedSpikes();				// <	|| ||| ||| ||| ||| |||

			//	choices of spike amplitudes:
			//	constantAmplitude();
			//	irregularAmplitude();				// < amplitudes varying from (0.5-- 1.5) * the amplitude
			 veryIrregularAmplitude();			// < amplitudes varying from (0 -- 1) * the amplitude

			//	choices of noises:
			noiseLevel1();						// < around 5 random small peaks per cycle, 1/10 the amplitude
			noiseLevel2();						// < around 5 random mid peaks per cycle, 1/8 the amplitude
			noiseLevel3();						// < around 1/4 random full peaks per cycles

			double dftWithWindow = dft.compute(dft.blackmanHarrisWinodow(signal), dftSearch)[1];
			double dftWithoutWindow = dft.compute(signal, dftSearch)[1];

			//find the errors in the phase values
			errorInPhaseWindowed += (Math.abs(phase - dftWithWindow) - errorInPhaseWindowed) / (i + 1);
			errorInPhaseNotWindowed += (Math.abs(phase - dftWithoutWindow) - errorInPhaseWindowed) / (i + 1);


			// Simple DFT Timing
			startTime = System.nanoTime();

			for(int j = 0; j < outputDFT.length; ++j) {
				outputDFT[j] = dft.compute(dft.blackmanHarrisWinodow(signal), dftSearchArea[j])[0];
			}
			endTime = System.nanoTime();

			averageTimeSimpleDFT = ((endTime - startTime) - averageTimeSimpleDFT) / (i + 1);


			
			
			
			// DFT Timing
			startTime = System.nanoTime();

			dFTseachError += Math.abs(dftSearcher.search(signal, 2000, 0, dftSearchStart, dftSearchEnd) - cyclesInWindow);
			//averageDFTseachBin += (dftBeatSearch.doSearch(signal, dftSearchStartEnd, 3) - averageDFTseachBin) / (i + 1);

			endTime = System.nanoTime();

			averageTimeDFT += ((endTime - startTime) - averageTimeDFT) / (i + 1);


			int doubled = 1;
			while(doubled < nSamples)
				doubled *= 2;

			double difference = doubled / (double)nSamples;

			int newBin = (int)(dftSearchStart * difference);




			// Harmonic FFT timing

			int numOfInputPoints = newBin;
			int startingIndex = newBin;
			int newArraySize = 256*2*2;

			double d = numOfInputPoints / (double)newArraySize;
			outputHarmonicFFT = new double[newArraySize];

			double constant = 0;

			startTime = System.nanoTime();

			double[] answer = signalProcess.blackmanHarrisFFT(signal);

			for (int j = 0; j < 6; j++) {
				for (int k = 0; k < newArraySize; ++k) {
					constant = startingIndex + d*k;
					outputHarmonicFFT[k] += ((answer[(int)constant + 1] - answer[(int)constant]) * (constant - (int)constant) +  answer[(int)constant]) * (j * 0.2 + 1);
				}
				startingIndex *= 2;
				numOfInputPoints *= 2;
				d = numOfInputPoints / (double) newArraySize;
			}

			double maxValue = 0;
			int maxValueIndex = 0;

			for (int j = 0; j < newArraySize; ++j) {
				if (outputHarmonicFFT[j] > maxValue){
					maxValue = outputHarmonicFFT[j];
					maxValueIndex = j;
				}
			}

			double ans = (maxValueIndex * (double)newBin / outputHarmonicFFT.length + newBin);

			ans /= difference;

			harmonicSeachError += Math.abs(cyclesInWindow - ans);

			endTime = System.nanoTime();

			averageTimeHarmonicFFT += ((endTime - startTime) - averageTimeHarmonicFFT) / (i + 1);

		}

		System.out.format("phase error averaged in windowed result = %.1f%%\n", errorInPhaseWindowed *100);
		System.out.format("phase error averaged in non-windowed result = %.1f%%\n", errorInPhaseNotWindowed *100);


		// create a DFT plot of the last signal created
		// first though, find the bins it will look for
		for(int i = 0; i < dftSearchArea.length; ++i) {
			dftSearchArea[i] = dftSearchStart + i * (dftSearchEnd - dftSearchStart) / dftSearchArea.length;
		}

		double maxPowerinDFT = 0;
		int maxPowerIndex = 0;

		for(int i = 0; i < outputDFT.length; ++i) {
			outputDFT[i] = dft.compute(dft.blackmanHarrisWinodow(signal), dftSearchArea[i])[0];
			phasesDFT[i] = dft.compute(dft.blackmanHarrisWinodow(signal), dftSearchArea[i])[1];
			if (outputDFT[i] > maxPowerinDFT) {
				maxPowerIndex = i;
				maxPowerinDFT = outputDFT[i];
			}
		}

		System.out.println("phase found = " + phasesDFT[maxPowerIndex]);
		System.out.format("freq bin = %.3f\n", dftSearchStart + maxPowerIndex * (dftSearchEnd - dftSearchStart) / dftSearchArea.length);


		System.out.format("\nSimple DFT search took on average %.3f milliseconds\n", averageTimeSimpleDFT / 1000000.0);
		
		System.out.println("\nDFT search overall error = " +  dFTseachError / numOfLoops);
		System.out.format("DFT search took on average %.3f milliseconds\n", averageTimeDFT / 1000000.0);

		System.out.println("\nFFT search overall error = " +  harmonicSeachError / numOfLoops);
		System.out.format("FFT search took on average %.3f milliseconds\n", averageTimeHarmonicFFT / 1000000.0);



		startTime = System.nanoTime();

		double[] answer = signalProcess.blackmanHarrisFFT(signal);

		phasesFFT = signalProcess.getPhases(50);



		// DISPLAYING THE DATA

		// create a data series to store the signal input
		XYSeries inputSeries = new XYSeries("Input Signal", false);

		// create a data series to store the fft output
		XYSeries outputFFtSeries = new XYSeries("FFT Algorithm", false);

		// create a data series to store the dft output
		XYSeries outputDFtSeries = new XYSeries("Simple DFT Algorithm", false);

		// create a data series to store the phase output
		XYSeries phaseFFtSeries = new XYSeries("Phase Reponse", false);

		// create a data series to store the dft output
		XYSeries phaseDFtSeries = new XYSeries("D Response", false);

		// create a data series to store the dft output
		XYSeries testSeries = new XYSeries("D Response", false);


		// load up the data series
		for (int s = 0; s < nSamples; ++s) {
			inputSeries.add(s, signal[s]);
			phaseFFtSeries.add(s, phasesFFT[s]);
		}

		// load only half of the FFT spectrum
		for (int s = 0; s < nSamples/2; ++s) {
			outputFFtSeries.add(s, answer[s]);
		}

		// load the DFT on its own, since it has a different
		// number of data points
		for (int i = 0; i < dftSearchArea.length; ++i) {
			outputDFtSeries.add(dftSearchArea[i], outputDFT[i]);
			phaseDFtSeries.add(dftSearchArea[i], phasesDFT[i]);
		}

		// load the test
		for (int i = 0; i < outputHarmonicFFT.length; ++i) {
			testSeries.add(i * (double)dftSearchStart/outputHarmonicFFT.length + dftSearchStart, outputHarmonicFFT[i]);
		}


		//display the input signal series
		XYDataset inputDataset = new XYSeriesCollection(inputSeries);
		JFreeChart chartSignal = ChartFactory.createXYLineChart("Generated Impulse Data", "Impulses", "Impulse Amplitude", inputDataset);
		ChartFrame frame1 = new ChartFrame("Signal Input Data", chartSignal);
		frame1.setVisible(true);
		frame1.setSize(1500, 500);

		// display the FFT data series
		XYSeriesCollection outputDataset = new XYSeriesCollection();
		outputDataset.addSeries(outputFFtSeries);
		outputDataset.addSeries(outputDFtSeries);
		//outputDataset.addSeries(testFFTSeries);
		JFreeChart chartFourier = ChartFactory.createXYLineChart("DFT of Generated Impulse Data", "Frequency Bins", "Power", outputDataset);
		ChartFrame frame2 = new ChartFrame("Fourier Output Data", chartFourier);
		frame2.setVisible(true);
		frame2.setSize(1500, 500);

		// display the DFT and data series
		XYSeriesCollection outputDFTDataset = new XYSeriesCollection();
		outputDFTDataset.addSeries(outputDFtSeries);
		JFreeChart chartDFTFourier = ChartFactory.createXYLineChart("Fourier DFT Data", "s", "Power", outputDFTDataset);
		ChartFrame frame3 = new ChartFrame("Fourier DFT Output Data", chartDFTFourier);
		frame3.setVisible(false);
		frame3.setSize(1500, 500);

		// display the DFT phases and data series
		XYSeriesCollection phaseDFTDataset = new XYSeriesCollection();
		phaseDFTDataset.addSeries(phaseDFtSeries);
		JFreeChart chartDFTPhase = ChartFactory.createXYLineChart("Fourier DFT Phase Data", "s", "Phase", phaseDFTDataset);
		ChartFrame frame4 = new ChartFrame("Fourier DFT Phase Data", chartDFTPhase);
		frame4.setVisible(false);
		frame4.setSize(1500, 500);

		// display the phases of FFT
		XYDataset phaseDataset = new XYSeriesCollection(phaseFFtSeries);
		JFreeChart chartPhase = ChartFactory.createXYLineChart("Phase Data", "s", "Angle / 2pi", phaseDataset);
		ChartFrame frame5 = new ChartFrame("Fourier Output Data", chartPhase);
		frame5.setVisible(false);
		frame5.setSize(1500, 500);

		// display the test
		XYDataset testDataset = new XYSeriesCollection(testSeries);
		JFreeChart testChart = ChartFactory.createXYLineChart("Test", "s", "Angle / 2pi", testDataset);
		ChartFrame frame6 = new ChartFrame("Fourier Output Data", testChart);
		frame6.setVisible(true);
		frame6.setSize(1500, 500);

	}


	//----------------------------------------------
	// INDEXES OF SPIKES
	//----------------------------------------------

	// fills the indexes of spikes array with regular spike indexes
	public static void regularSpikes() {

		// make an array large enough to fit every spike that could exist
		indexOfSpikes = new int[((int) cyclesInWindow) + 1];

		for (int i = 0; i < indexOfSpikes.length; ++i) {
			indexOfSpikes[i] = (int)((nSamples * phase / cyclesInWindow) + (i * nSamples) / cyclesInWindow);

			// if the index is lower than zero or higher than the number of sample there are,
			// set the index to -1 to indicate that the sample should not be plotted
			// in each method there should be an if statement to check if the index is -1
			// if it is, the index should be ignored
			if (indexOfSpikes[i] < 0 || indexOfSpikes[i] >= nSamples) {
				indexOfSpikes[i] = -1;
			}
		}
	}


	// fills the indexes of spikes array with spikes bunched together in doubles
	public static void doubleBunchedSpikes() {

		// make an array large enough to fit every spike that could exist
		indexOfSpikes = new int[((int) cyclesInWindow * 2) + 2];

		for (int i = 0; i < indexOfSpikes.length; i += 2) {
			indexOfSpikes[i] = (int)((nSamples * phase / cyclesInWindow) + ((i / 2) * nSamples) / cyclesInWindow);

			// add a second spike a quarter of a cycle behind the first (if we can)
			indexOfSpikes[i+1] = (int)((nSamples * (phase + 0.25) / cyclesInWindow) + ((i / 2) * nSamples) / cyclesInWindow);

			// if the index is lower than zero or higher than the number of sample there are,
			// set the index to -1 to indicate that the sample should not be plotted
			// in each method there should be an if statement to check if the index is -1
			// if it is, the index should be ignored
			if (indexOfSpikes[i] < 0 || indexOfSpikes[i] >= nSamples) {
				indexOfSpikes[i] = -1;
			}

			// do the same for the second spike
			if(indexOfSpikes[i+1] < 0 || indexOfSpikes[i+1] >= nSamples)
				indexOfSpikes[i+1] = -1;
		}
	}


	// fills the indexes of spikes array with spikes bunched together in doubles
	public static void tripleBunchedSpikes() {

		// make an array large enough to fit every spike that could exist
		indexOfSpikes = new int[((int) cyclesInWindow * 3) + 3];

		for (int i = 0; i < indexOfSpikes.length; i += 3) {
			indexOfSpikes[i] = (int)((nSamples * phase / cyclesInWindow) + ((i / 3) * nSamples) / cyclesInWindow);

			// add a second spike a quarter a cycle behind the first (if we can)
			indexOfSpikes[i+1] = (int)((nSamples * (phase + 0.25) / cyclesInWindow) + ((i / 3) * nSamples) / cyclesInWindow);

			// add a third spike half a cycle behind the first (if we can)
			indexOfSpikes[i+2] = (int)((nSamples * (phase + 0.75) / cyclesInWindow) + ((i / 3) * nSamples) / cyclesInWindow);

			// if the index is lower than zero or higher than the number of sample there are,
			// set the index to -1 to indicate that the sample should not be plotted
			// in each method there should be an if statement to check if the index is -1
			// if it is, the index should be ignored
			if (indexOfSpikes[i] < 0 || indexOfSpikes[i] >= nSamples) {
				indexOfSpikes[i] = -1;
			}

			// do the same for the second spike
			if(indexOfSpikes[i+1] < 0 || indexOfSpikes[i+1] >= nSamples)
				indexOfSpikes[i+1] = -1;

			// and the same for the third spike
			if(indexOfSpikes[i+2] < 0 || indexOfSpikes[i+2] >= nSamples)
				indexOfSpikes[i+2] = -1;
		}
	}


	//----------------------------------------------
	// SPIKE TYPES
	//----------------------------------------------

	// creates regular peaks with equal amplitudes
	public static void constantAmplitude() {

		// add the spikes to the window
		for(int i = 0; i < indexOfSpikes.length; ++i) {
			if (indexOfSpikes[i] != -1)
				signal[indexOfSpikes[i]] = amplitude;
		}

	}


	// creates regular peaks with unequal amplitudes varying from (0.5 -- 1.5) * the amplitude
	public static void irregularAmplitude() {

		// add the spikes to the window
		for(int i = 0; i < indexOfSpikes.length; ++i) {
			if (indexOfSpikes[i] != -1)
				signal[indexOfSpikes[i]] = amplitude * (0.5 + Math.random());
		}

	}


	// creates very unequal amplitudes varying from (0 -- 1) * the amplitude
	public static void veryIrregularAmplitude() {

		// add the spikes to the window
		for(int i = 0; i < indexOfSpikes.length; ++i) {
			if (indexOfSpikes[i] != -1)
				signal[indexOfSpikes[i]] = amplitude * Math.random();
		}

	}


	//----------------------------------------------
	// NOISE LEVELS
	//----------------------------------------------

	// around 5 random small peaks per cycle, 1/10 the amplitude
	public static void noiseLevel1() {

		for (int i = 0; i < nSamples; ++i) {
			if (Math.random() * signal.length / (1 * cyclesInWindow) < 1) {
				signal[i] = amplitude / 10;
			}
		}

	}

	// around 5 random mid peaks per cycle, 1/5 the amplitude
	public static void noiseLevel2() {

		for (int i = 0; i < nSamples; ++i) {
			// 1/10 chance of a medium peak
			if (Math.random() * signal.length / (5 * cyclesInWindow) < 1) {
				signal[i] = amplitude / 8;
			}
		}

	}

	// around 1/4 random full peaks per cycles
	public static void noiseLevel3() {

		for (int i = 0; i < nSamples; ++i) {
			// 1/10 chance of a small peak
			if (Math.random() * signal.length / ((1 / 4.0) * cyclesInWindow) < 1) {
				signal[i] = amplitude;
			}
		}

	}
}