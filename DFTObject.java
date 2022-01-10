package fourier;

public class DFTObject {

	double constant;

	int windowLength;

	double twoPiOverOne = 1 / (Math.PI * 2);
	
	double[] fourierOutput;

		/**
	 * This method computes the DFT for one single specified frequency bin
	 * in the data given to it. It returns the power of the complex result and
	 * also the phase of the frequency bin as an array of size 2
	 * @return a double array, with double[0] being the power of the frequency
	 * bin and double[1] being the phase of the bin
	 */
	public double[] compute(double[] window, double freqBin){

		double[] fourierOutput = new double[2]; 
		
		// first, we want to work out the constant so the for loop doesn't have to do
		// so many calculations
		constant = freqBin * 2 * Math.PI / window.length;

		// we also get the window length to possibly make this a little bit faster
		windowLength = window.length;

		// we then do the DFT
		// double[0] is real, double[1] is imaginary
		for (int i = 0; i < windowLength; ++i) {
			fourierOutput[0] += window[i] * Math.cos(constant * i);
			fourierOutput[1] += window[i] * Math.sin(constant * i);
		}

		// create a new array to store the result of the DFT
		// double[0] is the power, double[1] is the phase
		double[] dftOutput = new double[2];

		// we then find the power of the frequency in the window
		dftOutput[0] = Math.pow(fourierOutput[0], 2) + Math.pow(fourierOutput[1], 2);

		// we also find the phase of the frequency in the window
		dftOutput[1] = Math.atan2(fourierOutput[1], fourierOutput[0]) * twoPiOverOne;


		return dftOutput;

	}

	
	public double[] blackmanHarrisWinodow(double[] window) {

		windowLength = window.length;
		
		double[] newWindow = new double[windowLength];
		
		for (int i = 0; i < windowLength ; ++i) {
			newWindow[i] = window[i] * (0.355768 - 0.487396 * Math.cos(2 * Math.PI * i / windowLength)
					+ 0.144232 * Math.cos(4 * Math.PI * i / windowLength)
					- 0.012604 * Math.cos(6 * Math.PI * i / windowLength));

		}
		
		return newWindow;
	}

}
