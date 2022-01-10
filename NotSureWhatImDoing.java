package fourier;
import org.jfree.chart.*;
import org.jfree.data.xy.*;

public class NotSureWhatImDoing {

	final static int numberOfPoints = 1000;
	final static double periodLength = 3; // the cycle repeats every 'T' (seconds lets say)
	final static int numberOfCycles = 20; // the number of repeated cycles that happens in the data set

	
	final static int numberSamplesPerRevolution = 400;
	final static int maxNumberOfRevolutions = 100;

	public static void main(String arg[]) {

		double[] YDataArray = new double[numberOfPoints]; // array to hold the Y dataPoints
		double incrementX = periodLength * numberOfCycles / numberOfPoints;
		double currentPosition = 0;
		// create the sin wave data
		XYSeries series = new XYSeries("Average Weight");
		for (int i = 0; i < numberOfPoints; ++i) {
			YDataArray[i] = Math.sin((currentPosition * Math.PI * 2) / periodLength); // build up the array for the Y
																						// coordinates
			series.add(currentPosition, YDataArray[i]); // add the XY coordinate to the series
			currentPosition += incrementX;
		}

		// an array to store the frequency response for X axis
		double[] freqResponseX = new double[numberSamplesPerRevolution * maxNumberOfRevolutions];

		
		XYSeries freqSeriesX = new XYSeries("Frequency Response", false);

		// 'sampleCount' will restart after each revolution
		// after each revolution 'revolutionCount' will increment
		int sampleCount = 0; // keeps track of the number of samples taken 'so far' in each revolution
		int revolutionCount = 0; // keeps track of how many revolutions have been undertaken

		// 'maxAngle' and 'sigmaAngle' are calculated in every iteration of the for
		// loops but are both always increasing
		double maxAngle = 0; // keeps track of the leading data point's angle which can then be used to find
								// the 'sigmaAngle'
		double sigmaAngle = 0; // the change in angle between two adjacent data points

		double avrX = 0; // keeps track of where the 'centre of mass' is for X
		// double avrY = 0; // keeps track of where the 'centre of mass' is for Y
		
		XYSeries circleSeries = new XYSeries("CircleDrawing", false);

		// for loop using the six variables above
		for (int r = 0; r < maxNumberOfRevolutions; ++r) {
			for (int s = 0; s < numberSamplesPerRevolution; ++s) {
				// find the leading datapoints angle (note that this value will go over 2pi if
				// #Revolutions is larger than 1)
				maxAngle = ((2 * Math.PI * sampleCount) / numberSamplesPerRevolution) + (2 * Math.PI * revolutionCount);
				// divide by number of data points to find the change in angle between data
				// points
				sigmaAngle = maxAngle / numberOfPoints;
				for (int p = 0; p < numberOfPoints; ++p) {
					// iteratively accumulate the average position over all the data points
					avrX = avrX + (YDataArray[p] * Math.cos(sigmaAngle * p));
					
					// checking the circle graph at arbritray point
					if (r == 9 && s == 0) {
					circleSeries.add(YDataArray[p] * Math.cos(sigmaAngle * p), YDataArray[p] * Math.sin(sigmaAngle * p));
					System.out.println(YDataArray[p] * Math.sin(sigmaAngle * p));
					}
					
				}
				// save the average into a series array
				freqResponseX[s + numberSamplesPerRevolution * r] = avrX;
				freqSeriesX.add((s + numberSamplesPerRevolution * r) / (double) numberSamplesPerRevolution,
						avrX); // add the X coordinate to the series
				++sampleCount;
				avrX = 0;
			}
			++revolutionCount;
			sampleCount = 0;
		}

		// create the data set
		XYDataset xyDataset = new XYSeriesCollection(series);
		JFreeChart chart = ChartFactory.createXYLineChart("Input Data", "Time", "Amplitude", xyDataset);
		// create and show the chart
		ChartFrame frame1 = new ChartFrame("XYLine Chart", chart);
		frame1.setVisible(true);
		frame1.setSize(1200, 500);

		// create the frequency data set
		XYDataset xyDatasetFreq = new XYSeriesCollection(freqSeriesX);
		JFreeChart chartFreq = ChartFactory.createXYLineChart("Frequency Respose", "Frequency", "Amplitude",
				xyDatasetFreq);
		// create and show the chart
		ChartFrame frame2 = new ChartFrame("XYLine Chart", chartFreq);
		frame2.setVisible(true);
		frame2.setSize(1300, 400);
		
		// create the circle data set
		XYDataset xyDatasetCircle = new XYSeriesCollection(circleSeries);
		JFreeChart chartCircle = ChartFactory.createXYLineChart("Frequency Respose", "Frequency", "Amplitude",
				xyDatasetCircle);
		// create and show the chart
		ChartFrame frame3 = new ChartFrame("XYLine Chart", chartCircle);
		frame3.setVisible(true);
		frame3.setSize(700, 700);
	}
}