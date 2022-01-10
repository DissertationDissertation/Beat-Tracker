package rollingmemory;

/**
 * This rolling double works in much the same way as the rolling array memory
 * The rolling memory will start to loop and overwrite data as it is filled up
 * The main difference is that arrays of double (smaller than the memory) can
 * be placed into the memory and taken out of the memory, unlike in the rolling
 * array memory, where 2D arrays cannot be placed or taken out of the memory
 * 
 * Lets see how a rolling double memory works:
 * ' ^ ' is the "ghost index" or max accessible index
 * ' v ' is the actual index being pointed to or last added index
 * size of memory = 6
 * 	0	0	0	0	0	0
 * maxIndexAccessible = -1
 * minIndexAccessible = -6
 * revolutions = 0
 * 
 * An outside thread adds in an double:
 * 	v
 * 	3	0	0	0	0	0
 * 	^
 * maxIndexAccessible = 0
 * minIndexAccessible = -5
 * revolutions = 0
 * 
 * 	An outside thread adds in another double:
 * 		v
 * 	3	8	0	0	0	0
 * 		^
 * maxIndexAccessible = 1
 * minIndexAccessible = -4
 * revolutions = 0
 * 
 * An outside thread adds in an array of 4 doubles:
 * 						v
 * 	3	8	7	9	9	7
 * 						^
 * maxIndexAccessible = 5
 * minIndexAccessible = 0
 * revolutions = 0
 * 
 * An outside thread adds in one more double:
 * 	v
 * 	7	8	7	9	9	7
 * 							^
 * maxIndexAccessible = 6
 * minIndexAccessible = 1
 * revolutions = 1
 * 
 * 	An outside thread adds an array of 3 doubles:
 * 				v
 * 	7	1	1	1	9	7
 * 										^
 * maxIndexAccessible = 9
 * minIndexAccessible = 4
 * revolutions = 1
 * 
 * An outside thread adds an array of 5 doubles:
 * 			v
 * 	8	8	8	1	8	8
 * 															^
 * maxIndexAccessible = 14
 * minIndexAccessible = 9
 * revolutions = 1
 * 
 */

public class RollingDoubleMemory {


	private int sizeOfMemory;

	// read the above to understand what these are
	// briefly, they give the span of accessible indexes available for reading
	private int maxIndexAccessible;
	private int minIndexAccessible;

	// the actual index tells us which index to put a new double into
	// it resets back to 0 once it has reaches the size of the memory
	private int actualIndex = 0;

	// the retrieve index tells us which index to retrieve from
	// it is calculated every time a new request for an array is made
	private int retrieveIndex;

	private int revolutions = 0;

	private double[] rollingDouble;

	/**
	 * Creates a rolling memory for doubles of the requested size
	 * @param sizeOfMemory the maximum number of doubles to be temporarily stored at any one time
	 */
	public RollingDoubleMemory(int sizeOfMemory){

		this.sizeOfMemory = sizeOfMemory;

		// create a rolling memory with the size specified
		rollingDouble = new double[sizeOfMemory];

		// maxIndexAccessible is set to -1 to ensure no data is retried when the rolling memory is empty
		maxIndexAccessible = -1;
		minIndexAccessible = -sizeOfMemory;
		// because minIndexAccessible only becomes relevant after one revolution or wrap of the rolling
		// memory, we set it to become relevant (when it's > 0) once a revolution has been completed by
		// negating the size of one revolution, which is the number of doubles stored in the
		// rolling memory

	}

	/**
	 * Method for adding a single new double to the rolling memory. 
	 * Synchronised with other methods in this object to make the memory thread safe
	 * @param saveDouble the double to be saved to this temporary memory
	 * @see addDoubleArray
	 */
	synchronized public void addDouble(Double saveDouble) {

		// copy the value of the double into the array at the correct index
		rollingDouble[actualIndex] = saveDouble;

		// increase the maxIndexAccessible and the minIndexAccessible, as the span has
		// shifted over by one
		++maxIndexAccessible;
		++minIndexAccessible;

		// reset the actual index back to 0 once it reaches the end of the array,
		// ready for the next time a row is inserted
		// also increase the revolutions count as a wrap has happened on the array
		++actualIndex;
		if (actualIndex == sizeOfMemory) {

			actualIndex = 0;

			++revolutions;
		}


	}


	/**
	 * Method for overwriting a double in the rolling memory. 
	 * Synchronised with other methods in this object to make the memory thread safe
	 * @param overwriteIndex the index to be overwritten in the memory
	 * @param newDouble the new value the index in the memory should take
	 * @throws IllegalArgumentException if the index to be overwritten is not accessible any more
	 * or has not previously been written to
	 */
	synchronized public void overwriteDouble(int overwriteIndex, Double newDouble) {

		// make sure the index to overwritten exists
		checkVaildIndex(overwriteIndex);

		retrieveIndex = overwriteIndex - revolutions * sizeOfMemory;
		if(retrieveIndex < 0) {
			retrieveIndex += sizeOfMemory;
		}
		
		// simply overwrite the index in the array
		rollingDouble[retrieveIndex] = newDouble;

	}


	/**
	 * Method for adding an array of doubles to the rolling memory. 
	 * The array is duplicated in the rolling memory
	 * Synchronised with other methods in this object to make the memory thread safe
	 * @param saveDouble an double array to be saved into memory
	 * @throws IllegalArgumentException if the inputed array is larger than the memory, which would lead 
	 * to the array overwriting itself as it's added in
	 * @see addInt
	 */
	synchronized public void addDoubleArray(double[] saveDouble) {

		// check if the array is smaller than the rolling double memory size
		// if it isn't throw an exception
		if (saveDouble.length > sizeOfMemory) {
			throw new IllegalArgumentException("Array of size " + saveDouble.length + " is too large for memory of length " + sizeOfMemory);
		}

		// if all is good, start adding the arrays, using the method addDoubleArray
		for (int i = 0; i < saveDouble.length; ++i) {
			addDouble(saveDouble[i]);
		}

	}

	/**
	 * Method for retrieving a in double, at the index requested, from the rolling memory
	 * Synchronised with other methods in this object to make the memory thread safe
	 * @param requestedIndex the index of the double wanted from this temporary memory
	 * @return the double in memory at the index requested
	 * @throws IllegalArgumentException if the requested index has been overwritten or not yet written
	 * @see getDoubleArray
	 */
	synchronized public double getDouble(int requestedIndex) {

		// make sure the index is available
		checkVaildIndex(requestedIndex);

		retrieveIndex = requestedIndex - revolutions * sizeOfMemory;
		if(retrieveIndex < 0) {
			retrieveIndex += sizeOfMemory;
		}

		return rollingDouble[retrieveIndex];

	}


	/**
	 * Method for retrieving an array of doubles from the memory. It returns a copy of the array
	 * in the rolling memory. 
	 * Synchronised with other methods in this object to make the memory thread safe
	 * @param initialIndex the initial (first) index of the array wanted from this rolling memory
	 * @param arraySize the array size wanted out from the memory
	 * @return an array of doubles at the requested size starting at the initial index requested
	 * @throws IllegalArgumentException if any of the indexes in the wanted array have been overwritten 
	 * or have not yet been written. It also throws this if the array requested is larger than the array
	 * memory
	 * @see getDouble
	 * @see getDoublePrivate
	 */
	synchronized public double[] getDoubleArray(int initialIndex, int arraySize) {

		// make sure the initial index is available
		checkVaildIndex(initialIndex);

		// then check to make sure the end index of the array wanted exists
		checkVaildIndex(initialIndex + arraySize - 1);

		// if both indexes are valid, create a new double array an then return that array

		double[] returnArray = new double[arraySize];

		// set the retrieve index to the correct start point in the actual array for the
		// first value in the array to be returned
		retrieveIndex = initialIndex - revolutions * sizeOfMemory;
		if(retrieveIndex < 0) {
			retrieveIndex += sizeOfMemory;
		}

		for (int i = 0; i < arraySize; ++i) {
			// uses the private version of getDouble where the indexes are not checked
			// since we already know they are valid for both boundaries
			returnArray[i] = rollingDouble[retrieveIndex];

			// add on 1 to the read actual index to read the next double in the memory
			++retrieveIndex;

			// if the readActualIndex goes out of bounds of the memory array, set it back to
			// zero
			if (retrieveIndex == sizeOfMemory) {
				retrieveIndex = 0;
			}
		}

		return returnArray;

	}


	/**
	 * This method is only accessed by other methods in this object. 
	 * It will throw an error based on if the requested index has been overwritten or not
	 * 
	 * @param requestedIndex the index of the double wanted from this temporary memory
	 * @throws IllegalArgumentException if the requested index has been overwritten or not yet written
	 * @see getDoubleArray
	 * @see getDouble
	 */
	synchronized private void checkVaildIndex(int requestedIndex) {

		// check the index requested has not been overwritten in the rolling array
		// throw an error if it has been already overwritten
		if (requestedIndex < minIndexAccessible) {
			throw new IllegalArgumentException("Requested index " + requestedIndex + " has been overwritten (not in the range of " + minIndexAccessible + " - " + maxIndexAccessible + ")");
		}

		// check if the index requested has actually been added to the rolling array
		// if it's out of bounds by being over, throw an error message
		if (requestedIndex > maxIndexAccessible) {
			throw new IllegalArgumentException("Requested index " + requestedIndex + " is not yet written (not in the range of " + minIndexAccessible + " - " + maxIndexAccessible + ")");
		}

		// if all is good the code that called this method will continue without throwing an error

	}


	/**
	 * Returns the index of the last double inputed into the rolling memory
	 * Synchronised with other methods in this object to make the memory thread safe
	 * @return the index of the last double added to this rolling array
	 */
	synchronized public int getLastAddedIndex() {
		return maxIndexAccessible;
	}


	/**
	 * Returns the minimum index available in the rolling memory. 
	 * Any array indexes lower than the value returned here have been overwritten
	 * Synchronised with other methods in this object to make the memory thread safe
	 * @return the index of the last double added to this rolling array
	 */
	synchronized public int getMinIndexAvailable() {
		return minIndexAccessible;
	}


	/**
	 * Returns the max amount of doubles the rolling memory can store before it overwrites. 
	 * Synchronised with other methods in this object to make the memory thread safe
	 * @return the maximum number of doubles that can be stored before overwriting occurs
	 */
	synchronized public int getMemorySize() {
		return sizeOfMemory;
	}


}




