package fourier;

/**
 * The FFT compute method returns an array showing the frequencies
 * present in the window it was given. These frequencies
 * ARE RELATIVE TO THE WINDOW IT WAS GIVEN.
 * For example, if an FFT is done on a window of frames, the signal is
 * broken down into simple sine waves, and an array of values is returned.
 * Let's say the maximum value in the array it returns is at index 7.
 * This means a sine wave that repeats 7 times within the window has the
 * largest amplitude (dominant frequency). It does not mean that the sine
 * wave with the largest amplitude repeats every 7 frames (period of 7 frames).
 * most prominent signal. 
 * @author Clement
 *
 */
class FFTObjectTrial {

	// store the window size of the FFT
	// this cannot be changed once the object has been constructed
	private final int WINDOW_SIZE;

	// create an complex input vector for the FFT computation (hence the 2D array)
	private double[][] _complexVector;

	// create a real output array that the compute method can return
	private double[] _output;

	// create an imaginary output array that the getOffsets method can return
	private double[] _phaseOutput;

	// create a twiddleFactor matrix that will be filled when the object is created
	// and can only be used for the inputed window size
	// it could technically be used for a smaller inputed window size than the size it was initialised for
	// but it cannot handle a window size any larger (at all)
	// if the window size is created for 1024 points, the twiddleFactor will have a length of 1024 / 2
	// because the twiddleFactor matrix doesn't need to change it's values once they have been computed,
	// it is initialised as 'final'
	private final double[][] _twiddleFactor;

	public FFTObjectTrial(int windowSize) {

		// check to make sure that the inputed window size for construction has a size of 2^n
		int windowSizeHalf = windowSize;
		while((windowSizeHalf % 2) == 0 && windowSizeHalf != 1) {
			windowSizeHalf = windowSizeHalf / 2;
		}
		if (windowSizeHalf != 1) {
			// if the inputed window size for construction does not have a size of 2^n, return an error
			throw new IllegalArgumentException("The window size for initialisation should be a number of 2^n");
		}

		// set the property windowSize
		this.WINDOW_SIZE = windowSize;

		// set the size of the complex vector which will store the complex input vector
		// and then will have calculations done and re-stored into the same array
		_complexVector = new double[windowSize][2];

		// set the size of the vector the computeFFT method will return
		_output = new double[windowSize];

		// set the size of the vector the getPhases method will return
		_phaseOutput = new double[windowSize];

		// set the size of the twiddleFactor matrix
		_twiddleFactor = new double[windowSize / 2][2];  // twiddle matrix where [][0] is real and [][1] is imaginary


		// fill in the twiddleFactor matrix
		// this does not have to be recalculated every time the FFT compute method is called
		// the twiddleFactor matrix exactly the same for a window size (regardless of the values in the window)
		// r here stands for row
		for (int r = 0; r < windowSize / 2; ++r) {

			_twiddleFactor[r][0] = Math.cos((2 * Math.PI * r) / windowSize);
			_twiddleFactor[r][1] = -Math.sin((2 * Math.PI * r) / windowSize);

		}

	}

	protected double[] compute(double window[]) {

		// transfer the real input to a complex vector and do bit reversal to shuffle around the input
		// into the correct order for FFT to calculate correctly
		int nr; // nr stands for new row index
		// r stands for row
		for (int r = 0; r < WINDOW_SIZE; ++r) {
			nr = Integer.reverse(r) >>> (32 - (int) (Math.log(window.length) / Math.log(2)) );
		// do a bit reversal of an index to quickly find it's new index 
		_complexVector[nr][0] = window[r];
		_complexVector[nr][1] = window[0];	// the input is real (for now)
		}



		int step = 1;

		int loop = WINDOW_SIZE / step;

		int log2ofN = (int) (Math.log(WINDOW_SIZE) / Math.log(2));
		// this variable tells how many 'stages' of butterfly calculation there are

		int a; // this holds the index of the first reference to the complex input vector
		int b; // this holds the index of the second reference to the complex input vector
		int t; // this hold the index to the wanted twiddle factor in the complex twiddleFactor vector

		// this will hold an intermediary complex value for use in calculations
		double[] complexResult = new double[2];


		for (int stage = 0; stage < log2ofN; ++stage) {

			for (int offset = 0; offset < step; offset++) {

				for (int stepIteration = 0; stepIteration < loop / 2; ++stepIteration) {
					a = offset + stepIteration * step * 2;
					b = offset + step + stepIteration * step * 2;
					t = offset * (loop / 2);
					// find what wn (twiddle factor from the complex twiddleFactor vector)
					// multiplied by the second variable in the step is
					complexResult[0] = _complexVector[b][0] * _twiddleFactor[t][0]
							- _complexVector[b][1] * _twiddleFactor[t][1];
					complexResult[1] = _complexVector[b][1] * _twiddleFactor[t][0]
							+ _complexVector[b][0] * _twiddleFactor[t][1];

					_complexVector[b][0] = _complexVector[a][0] - complexResult[0];
					_complexVector[b][1] = _complexVector[a][1] - complexResult[1];

					_complexVector[a][0] = _complexVector[a][0] + complexResult[0];
					_complexVector[a][1] = _complexVector[a][1] + complexResult[1];
				}
			}
			step = step * 2;
			loop = WINDOW_SIZE / step;
		}

		// find the magnitude of each complex number in the complex vector
		for (int r = 0; r < WINDOW_SIZE; ++r) {
			_output[r] = Math.pow(_complexVector[r][0], 2) + Math.pow(_complexVector[r][1], 2);
		}		

		return _output;

	}

}
