package rollingmemory;

/*this 2D array holds a specified number of 1D arrays for temporary safe keeping
 this rolling array will roll from top to bottom.
 Once the array if full from being written to by an outside thread,
 the row index being written will loop back around and data will start
 to be overwritten, thereby only giving temporary storage of data.
 the span of past data that can be accessed starts to move.
 the span size stays constant (maxGhostIndex - minGhostIndex) (or number of rows).
 this is all in an attempt to save space.
 the following is an example of how this works:
 the '  >  ' shows the index where the last row was added to the array (actualIndex)
 the '  <  ' shows the index where maxIndexAccessible is (this value is the same as the 'ghost index')

 Initialised rolling array with 5 rows and 4 columns:
		0	0	0	0	0
		0	0	0	0	0
		0	0	0	0	0
		0	0	0	0	0
 maxIndexAccessible = -1;	// the max index available is -1 as no rows contain useful data
 minIndexAccessible = -4;	// the min index available is irrelevant since no data has been overwritten
 revolutions = 0;

 Outside thread appends (adds) a new row to the data:
 	>	1	5	1	5	1	<
 		0	0	0	0	0
 		0	0	0	0	0
 		0	0	0	0	0	
 maxIndexAccessible = 0;		// the max index accessible is 0 as only rows 0 contains useful data
 minIndexAccessible = -3;		// the min index accessible index stays irrelevant as no data is overwritten yet
 revolutions = 0;				// the data hasn't wrapped around yet

 Outside thread appends (adds) a new row to the data:
		1	5	1	5	1
	>	7	7	7	7	3	<
		0	0	0	0	0
		0	0	0	0	0
 maxIndexAccessible = 1;		// the max index accessible is 1 as rows 0 and 1 contain useful data
 minIndexAccessible = -2;		// the min index accessible index stays irrelevant as no data is overwritten yet
 revolutions = 0;				// the data hasn't wrapped around yet

 Outside thread sequentially appends two new rows:
		1	5	1	5	1
		7	7	7	7	3
		1	2	1	2	3	
	>	4	4	4	5	5	<
 maxIndexAccessible = 3;		// the max index accessible is 3 as rows 0 through 4 contain useful data
 minIndexAccessible = 0;		// the min index accessible index stays irrelevant as no data is overwritten yet
 revolutions = 0;				// the data hasn't wrapped around yet

Outside thread adds one more row:
	>	9	8	9	6	9
		7	7	7	7	3
		1	2	1	2	3	
		4	4	4	5	5	
							<ghost index
 maxIndexAccessible = 4;		// the max index accessible increases by 1
 minIndexAccessible = 1;		// the min index accessible starts to increase as data has been overwritten
 revolutions = 1;				// the data has now wrapped around the array once

 Outside thread adds one more row:
		9	8	9	6	9			order of the rows by how old they are (newest = 1):		2
	>	2	3	4	1	6																	1
		1	2	1	2	3																	4	
		4	4	4	5	5																	3

							<ghost index
 maxIndexAccessible = 5;		// the max index accessible increases by 1
 minIndexAccessible = 2;		// the min index accessible increases by 1
 revolutions = 1;				// the data has still only been wrapped around the array once

 After 1 more revolution (wrap) from sequentially adding rows, the array
 has been fully overwritten from it's state above and it looks like this:
		1	7	7	9	1
	>	1	7	6	9	4
		3	9	9	1	0
		6	5	7	8	1





							<ghost index
 maxIndexAccessible = 11
 minIndexAccessible = 7
 revolutions = 2

 Lets's see what the data would look like if we were keeping it all and storing the added rows
 inside a normal array:
		1	5	1	5	1
		7	7	7	7	3
		1	2	1	2	3	
		4	4	4	5	5	
		9	8	9	6	9
		2	3	4	1	6
		3	9	9	1	0
		6	5	7	8	1
		1	7	7	9	1
	>	1	7	6	9	4
 our rolling array only stores these values, the rest have been overwritten.
 the index of these values corresponds to the min and max indexes accessible.

 for a thread to access an index of the rolling array, it requests the index it wants.
 the index must be in the span of data available:
 (minIndexAccessible<= requestedIndex <= maxIndexAccessible)
 if the index is not in that span, the data has either not yet been inputed into the
 rolling array, or the data has been overwritten and the thread is too late to retrieve it.
 to convert between the requested index vs the actual index of the rolling array
 we do the following:

 actualIndex = requestedIndex - revolutions * numberOfRows
 if(actualIndex < 0) {
	actualIndex += numberOfColumns
 }

 this is actually a little hard to explain,but if you sit down with a piece of paper
 and try it out for yourself, you'll understand this works

 it's important to note that a thread can only add a new row adjacent in index to the
 previous row inputed and once the row is inputed, it cannot be altered,
 it can only be read, or overwritten after more row are added
 */public class RollingArrayMemory {

	 final private int numberOfRows;
	 final public int numberOfColumns;
	 // number of rows equates to how many arrays are going to be stored at one time
	 // number of columns equates to the size of the arrays that are going ot be stored

	 // read the above to understand what these are
	 // briefly, they give the span of accessible indexes available for reading
	 private int maxIndexAccessible;
	 private int minIndexAccessible;

	 // the actual index tells us which index to put a new row into
	 // it resets back to 0 once it has reaches the number of rows in the rollingArray
	 private int actualIndex = 0;
	 
	 // the retrieve index tells us which index to retrieve from
	 // it is calculated every time a new request for an array is made
	 private int retrieveIndex;
	 
	 private int revolutions = 0;

	 // we do not want threads to access the array directly
	 // we want them to use the synchronised methods to read and write
	 // to the array, hence why this is chosen to be private
	 private double[][] rollingArray;


	 /**
	  * Creates a rolling array memory of the requested sizes
	  * @param arraySize the size of the arrays that are going to be temporarily stored
	  * @param numberOfArraysToStore  the amount of arrays to be stored at any one time
	  */
	 public RollingArrayMemory (int arraySize, int numberOfArraysToStore) {
		 // set the number of rows and columns wanted.
		 // these dimensions are final and cannot be changed
		 numberOfRows = numberOfArraysToStore;
		 numberOfColumns = arraySize;

		 // create a rolling array with the wanted dimensions
		 rollingArray = new double[numberOfRows][numberOfColumns];

		 // maxIndexAccessible is set to -1 to ensure no data is retried when the rolling array is empty
		 maxIndexAccessible = -1;
		 minIndexAccessible = - numberOfRows;
		 // because minIndexAccessible only becomes relevant after one revolution or wrap of the rolling array,
		 // we set it to become relevant (when it's > 0) once a revolution has been completed by negating
		 // the size of one revolution, which is the number of rows in the rolling array
	 }


	 /**
	  * Method for adding a new array (row) to the rolling array. The method copies each
	  * value in the array given to it into the memory array. The added array must be of
	  * the same size this rolling memory was created for, else an error is thrown
	  * Synchronised with other methods in this object to make the memory thread safe
	  * @param numbers the array to be saved to this temporary memory
	  * @throws error if array to be added is not the same size as the size of array this
	  * memory holds
	  */
	 synchronized public void addArray(double[] numbers) {

		 // ensure that the array being attempted to add to the memory is of the correct
		 // size this rolling array memory was instantiated for
		 if (numbers.length != numberOfColumns) {
			 throw new IllegalArgumentException("Array to be added is of size " + numbers.length + ". Memory created for array of size " + numberOfColumns + ".");
		 }
		 
		 // copy each value from the input array into the rolling array
		 for (int i = 0; i < numberOfColumns; ++i) {

			 rollingArray[actualIndex][i] = numbers[i] ;

		 }

		 // increase the maxIndexAccessible and the minIndexAccessible, as the span has
		 // shifted over by one
		 ++maxIndexAccessible;
		 ++minIndexAccessible;

		 // reset the actual index back to 0 once it reaches the end of the rolling array,
		 // ready for the next time a row is inserted
		 // also increase the revolutions count as a wrap has happened on the rolling array
		 ++actualIndex;
		 if (actualIndex == numberOfRows) {

			 actualIndex = 0;

			 ++revolutions;
		 }

	 }


	 /**
	  * Method for retrieving a copy of an array (row) from the rolling array
	  * Synchronised with other methods in this object to make the memory thread safe
	  * @param requestedIndex the index of the array wanted from this temporary memory
	  * @return a copy of the array in memory at the index requested
	  * @throws IllegalArgumentException if the requested array index has been overwritten or not yet written
	  * @see getArrayPointer
	  */
	 synchronized public double[] getArrayCopy(int requestedIndex) {

		 this.checkVaildIndex(requestedIndex);
		 // if the index requested is invalid, this method throws an error which halts the code

		 // create a new array to return
		 double[] readRow = new double[numberOfColumns];

		 // find the actual column index in the rolling array to read from
		 retrieveIndex = requestedIndex - revolutions * numberOfRows;
		 if(retrieveIndex < 0) {
			 retrieveIndex += numberOfRows;
		 }

		 // copy the data from the array into the new array
		 for (int i = 0; i < numberOfColumns; ++i) {
			 readRow[i] = rollingArray[retrieveIndex][i];
		 }

		 return readRow;

	 }


	 /**
	  * Method for quickly retrieving a pointer to an array (row) in the rolling array. 
	  * This method is only recommended for use when the array retrieved will receive immediate processing. 
	  * If the rolling memory rolls back around before the retrieved array is utilised, the data in that array
	  * will be overwritten and lost, leading to unexpected errors which may be hard to diagnose. In this way,
	  * this method is not inherently thread safe. 
	  * For more permanent data retrieval, use getArrayCopy, which returns a copy of the data stored 
	  * and is thread safe.
	  * @param requestedIndex the index of the array wanted from this temporary memory
	  * @return a copy of the array in memory at the index requested
	  * @throws IllegalArgumentException if the requested array index has been overwritten or not yet written
	  * @see getArrayCopy
	  */
	 synchronized public double[] getArrayPointer(int requestedIndex) {

		 this.checkVaildIndex(requestedIndex);
		 // if the index requested is invalid, this method throws an error which halts the code

		 // find the actual column index in the rolling array to read from
		 retrieveIndex = requestedIndex - revolutions * numberOfRows;
		 if(retrieveIndex < 0) {
			 retrieveIndex += numberOfRows;
		 }

		 return rollingArray[retrieveIndex];

	 }


	 /**
	  * This method is only accessed by other methods in this object. 
	  * It will throw an error based on if the requested index has been overwritten or not
	  * 
	  * @param requestedIndex the index of the array wanted from this temporary memory copy of the array in memory at the index requested
	  * @throws IllegalArgumentException if the requested array index has been overwritten or not yet written
	  * @see getArrayCopy
	  * @see getArrayPointer
	  */
	 synchronized private void checkVaildIndex(int requestedIndex) {

		 // check the index requested has not been overwritten in the rolling array
		 // throw an error if it has been already overwritten
		 if (requestedIndex < minIndexAccessible) {
			 throw new IllegalArgumentException("Requested array at index " + requestedIndex + " has been overwritten (not in the range of " + minIndexAccessible + " - " + maxIndexAccessible + ")");
		 }

		 // check if the index requested has actually been added to the rolling array
		 // if it's out of bounds by being over, throw an error message
		 if (requestedIndex > maxIndexAccessible) {
			 throw new IllegalArgumentException("Requested array at index " + requestedIndex + " is not yet written (not in the range of " + minIndexAccessible + " - " + maxIndexAccessible + ")");
		 }

		 // if all is good the code that called this method will continue without throwing an error

	 }

	 /**
	  * Returns the index of the last array inputed into the rolling array memory
	  * Synchronised with other methods in this object to make the memory thread safe
	  * @return the index of the last array added to this rolling array
	  */
	 synchronized public int getLastAddedIndex() {
		 return maxIndexAccessible;
	 }


	 /**
	  * Returns the minimum index available in the rolling array. 
	  * Any array indexes lower than the value returned here have been overwritten
	  * Synchronised with other methods in this object to make the memory thread safe
	  * @return the index of the last array added to this rolling array
	  */
	 synchronized public int getMinIndexAvailable() {
		 return minIndexAccessible;
	 }


	 /**
	  * Returns the max amount of arrays the rolling array memory can stores before it overwrites. 
	  * Synchronised with other methods in this object to make the memory thread safe
	  * @return the maximum number of arrays that can be stored before overwriting occurs
	  */
	 synchronized public int getMemorySize() {
		 return numberOfRows;
	 }


	 /**
	  * Returns the array size the rolling array memory stores. 
	  * Synchronised with other methods in this object to make the memory thread safe
	  * @return the expected array size when writing to this rolling array memory
	  */
	 synchronized public int getArraySize() {
		 return numberOfColumns;
	 }


 }
