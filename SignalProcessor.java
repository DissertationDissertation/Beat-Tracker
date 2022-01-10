package fourier;

/**
 * This class presents a range of processing methods that can be applied to a signal window
 * in order to alter it's characteristics before beat detection is carried out. It is an extension of the
 * FFT object and inherits all its methods.
 * MANY OF THE METHODS IN THIS CLASS WILL OVERWRITE THE ARRAY GIVEN TO THEM WITH NEW DATA!!!
 * @author Clement Evans
 *
 */
public class SignalProcessor extends FFTObject{

	// This array is used in various methods to return a new array other than the one given to it
	// this variable is overwritten pretty much every time a method in this object is used
	double[] doubleArray;

	double[] intToDoubleArray;

	private int frames;

	private int inFrames;

	private int startIndexOfData;


	/**
	 * This object is used for different types of processing of the signal. It will perform
	 * FFT calculations with different window types and can process the power spectrum
	 * in various ways, all accessible though this objects methods. The number of frames
	 * given to this object must always be equal to the number given here in this constructor, else
	 * the processing will return wrong results. The number of frames must always be of size 2^n,
	 * as these is the only window sizes the FFT object can use. A check on the number of frames given
	 * will be performed here in this constructor, as well as in the FFT object. Although this is
	 * a redundancy, it only needs to be done once, and ensures that the FFT object can be used on
	 * its own, should the need arise.
	 * @param nFrames
	 * @throws a error if the number of frames is not of 2^n
	 */
	public SignalProcessor(int nFrames){

		super(nFrames);

		// check to make sure that the inputed window size for construction has a size of 2^n
		int windowSizeHalf = nFrames;
		while((windowSizeHalf % 2) == 0 && windowSizeHalf != 1) {
			windowSizeHalf /= 2;
		}

		int size = 1;
		while (size < nFrames)
			size *= 2;

		this.startIndexOfData = (frames - inFrames) / 2;

		this.inFrames = nFrames;
		this.frames = size;
		this.doubleArray = new double[size];
		this.intToDoubleArray = new double[nFrames];



	}


	/**
	 * This method does an FFT to the signal given to it but applies a hamming window to the window
	 * and then readjusts the power spectrum after computing the FFT to give true power values
	 * to each frequency
	 * @param framesD the signal that we want to apply a hamming window to and then FFT process, as a double array
	 * @return the computed FFT of the signal with a hamming window placed on it. The array returned is not
	 * a new array and will be overwritten if another FFT (of any type) is performed
	 * @throws the input array length is not the same size as the one this object was constructed
	 * for
	 */
	public double[] hammingFFT(double[] framesD) {
		// make sure the array is of the correct size
		checkArraySize(framesD.length);

		// pad the signal with zeros
		for (int i = 0; i < startIndexOfData; ++i) 
			doubleArray[i] = 0;
		for (int i = startIndexOfData + inFrames; i < frames; ++i)
			doubleArray[i] = 0;

		// add a hamming window to the signal data, which tapers off the ends of the signal
		// start at the initial padding location
		for (int i = startIndexOfData; i < inFrames; ++i) {
			doubleArray[i] = framesD[i] * (0.54 - 0.46 * Math.cos((2 * Math.PI * i) / frames));
		}

		// compute FFT of the signal with compute() method in the FFTObject that this class extends from
		doubleArray = compute(doubleArray);

		// return the double array. If another method is used in this object that
		// utilises the double array, it will be overwritten 
		return doubleArray;
	}


	/**
	 * The integer argument equivalent of hammingFFT(double[])
	 * @param framesI the signal that we want to apply a hamming window to as an integer
	 * @return the computed FFT of the signal with a hamming window placed on it. The array returned is not
	 * a new array and will be overwritten if another FFT (of any type) is performed
	 * @throws the input array length is not the same size as the one this object was constructed
	 * for
	 * @see hammingFFT(double[])
	 */
	public double[] hammingFFT(int[] framesI) {
		// make sure the array is of the correct size
		checkArraySize(framesI.length);

		// converts the input into a double and then uses the hammingFFT(double[]) method
		return hammingFFT(dbleToIntArray(framesI));
	}


	/**
	 * This method does an FFT to the signal given to it. It returns the array passed to it with the new
	 * data
	 * @param framesD the signal that we want to apply a hamming window to, as a double array
	 * @return the computed FFT of the signal. The array returned is not
	 * a new array and will be overwritten if another FFT (of any type) is performed
	 * @throws the input array length is not the same size as the one this object was constructed
	 * for
	 */
	public double[] FFT(double[] framesD) {
		// make sure the array is of the correct size
		checkArraySize(framesD.length);
		
		// pad the signal with zeros
		for (int i = 0; i < startIndexOfData; ++i) 
			doubleArray[i] = 0;
		for (int i = startIndexOfData + inFrames; i < frames; ++i)
			doubleArray[i] = 0;
		
		for (int i = startIndexOfData; i < inFrames; ++i) {
			doubleArray[i] = framesD[i];
		}

		// compute FFT of the signal with compute() method in the FFTObject that this class extends from
		return compute(doubleArray);
	}


	/**
	 * The integer argument equivalent of FFT(double[])
	 * @param framesI the signal that we want to apply a hamming window to as an integer
	 * @return the computed FFT of the signal, as a double array.
	 * The double array returned is a pointer to an array that exists within this object. Therefore
	 * if a method called subsequently from this object uses this array, the values will be
	 * overwritten
	 * @throws the input array length is not the same size as the one this object was constructed
	 * for
	 * @see FFT(double[])
	 */
	public double[] FFT(int[] framesI) {
		// make sure the array is of the correct size
		checkArraySize(framesI.length);

		// converts the input into a double and then uses the FFT(double[]) method
		return FFT(dbleToIntArray(framesI));
	}


	/**
	 * This method does an FFT to the signal given to it but applies a hanning window to the window
	 * and then readjusts the power spectrum after computing the FFT to give true power values
	 * to each frequency
	 * @param framesD the signal that we want to apply a hanning window to and then FFT process, as a double array
	 * @return the computed FFT of the signal with a hanning window placed on it. The array returned is not
	 * a new array and will be overwritten if another FFT (of any type) is performed
	 * @throws the input array length is not the same size as the one this object was constructed
	 * for
	 */
	public double[] hanningFFT(double[] framesD) {


		// make sure the array is of the correct size
		checkArraySize(framesD.length);

		// pad the signal with zeros
		for (int i = 0; i < startIndexOfData; ++i) 
			doubleArray[i] = 0;
		for (int i = startIndexOfData + inFrames; i < frames; ++i)
			doubleArray[i] = 0;
		
		// add a hanning window to the signal data
		for (int i = startIndexOfData; i < inFrames ; ++i) {
			doubleArray[i] = framesD[i] * (0.5 - 0.4 * Math.cos((2 * Math.PI * i) / frames));
		}

		// compute FFT of the signal with compute() method in the FFTObject that this class extends from
		doubleArray = compute(doubleArray);

		// return the double array. If another method is used in this object that
		// utilises the double array, it will be overwritten 
		return doubleArray;
	}


	/**
	 * The integer argument equivalent of hanningFFT(double[])
	 * @param framesI the signal that we want to apply a hanning window to as an integer
	 * @return the computed FFT of the signal with a hanning window placed on it. The array returned is not
	 * a new array and will be overwritten if another FFT (of any type) is performed
	 * @throws the input array length is not the same size as the one this object was constructed
	 * for
	 * @see hanningFFT(double[])
	 */
	public double[] hanningFFT(int[] framesI) {
		// make sure the array is of the correct size
		checkArraySize(framesI.length);

		// converts the input into a double and then uses the hanningFFT(double[]) method
		return hanningFFT(dbleToIntArray(framesI));
	}



	/**
	 * This method does an FFT to the signal given to it but applies a Blackman-Harris window to the window
	 * and then readjusts the power spectrum after computing the FFT to give true power values
	 * to each frequency
	 * @param framesD the signal that we want to apply a Blackman-Harris window to and then FFT process, as a double array
	 * @return the computed FFT of the signal with a Blackman-Harris window placed on it. The array returned is not
	 * a new array and will be overwritten if another FFT (of any type) is performed
	 * @throws the input array length is not the same size as the one this object was constructed
	 * for
	 */
	public double[] blackmanHarrisFFT(double[] framesD) {
		// make sure the array is of the correct size
		checkArraySize(framesD.length);
		
		// pad the signal with zeros
		for (int i = 0; i < startIndexOfData; ++i) 
			doubleArray[i] = 0;
		for (int i = startIndexOfData + inFrames; i < frames; ++i)
			doubleArray[i] = 0;

		// add a hamming window to the signal data, which tapers off the ends of the signal
		for (int i = startIndexOfData; i < inFrames ; ++i) {
			doubleArray[i] = framesD[i] * (0.355768 - 0.487396 * Math.cos(2 * Math.PI * i / frames)
					+ 0.144232 * Math.cos(4 * Math.PI * i / frames)
					- 0.012604 * Math.cos(6 * Math.PI * i / frames));
		}

		// compute FFT of the signal with compute() method in the FFTObject that this class extends from
		doubleArray = compute(doubleArray);

		// return the double array. If another method is used in this object that
		// utilises the double array, it will be overwritten 
		return doubleArray;
	}


	/**
	 * The integer argument equivalent of blackmanHarrisFFT(double[])
	 * @param framesI the signal that we want to apply a Blackman-Harris window to as an integer
	 * @return the computed FFT of the signal with a Blackman-Harris window placed on it. The array returned is not
	 * a new array and will be overwritten if another FFT (of any type) is performed
	 * @throws the input array length is not the same size as the one this object was constructed
	 * for
	 * @see blackmanHarrisFFT(double[])
	 */
	public double[] blackmanHarrisFFT(int[] framesI) {
		// make sure the array is of the correct size
		checkArraySize(framesI.length);

		// converts the input into a double and then uses the hammingFFT(double[]) method
		return blackmanHarrisFFT(dbleToIntArray(framesI));
	}


	/**
	 * This method simply returns the square root of each value in the array given to it
	 * The method does not return a new array but changes the values in the array given to it
	 * @param input the power spectrum window to be processed by the sqrt
	 * @return the array it was given but with all the values square rooted
	 */
	public double[] sqrt(double[] input) {
		for(int i = 0; i < input.length; ++i) {
			input[i] = Math.sqrt(input[i]);
		}
		return input;
	}


	public double[] log(double[] input) {
		for (int i = 0; i < frames; ++i) {
			input[i] = Math.log10(1 + input[i]);
		}
		return input;
	}


	/**
	 * A private method used by all the methods in this object to first ensure the
	 * array about to be processed is of the correct size for the object instantiated
	 */
	private void checkArraySize(int arrayLength) {
		if (arrayLength != inFrames) {
			throw new IllegalArgumentException("\nInput array length should be " + inFrames
					+ "\nInputed array length was " + arrayLength);
		}
	}


	/**
	 * This is a private method only used in this object. It is made so that methods can
	 * accept both integer values as well as double values as arguments. It also ensures
	 * that a new double array is not carved out of the system memory every time an array
	 * is converted, as it converts the values into an array that already exists in
	 * this object
	 * @param intArray
	 * @return
	 */
	private double[] dbleToIntArray(int[] intArray) {
		for (int i = 0; i < inFrames; ++i) {
			intToDoubleArray[i] = (double) intArray[i];
		}
		return intToDoubleArray;
	}




}
