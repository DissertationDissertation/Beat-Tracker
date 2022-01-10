package graphs;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;
import java.awt.Shape;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartFrame;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import java.util.ListIterator;
import java.awt.geom.Rectangle2D;

public class BeatPlacementPlotter {

	static String filePath = "E:/results/";
	
	static String placedBeatsFileName = "Deorro-FiveHours user beat labelled";
	static String computerBeatsFileName = "Deorro-FiveHours beat predictions";

	// Arrays to hold the final arrays of the beat placements
	static int[] placedBeatsArray;
	static int[] computerBeatsArray;

	static int falsePositive = 0;
	static int falseNegative = 0;
	static int hits = 0;

	static int errorAllowable = 70;


	public static void main(String args[]) throws FileNotFoundException {

		File placedBeatsFile = new File(filePath + placedBeatsFileName + ".txt");
		File computerBeatsFile = new File(filePath + computerBeatsFileName + ".txt");


		// -------------------------------------------
		// Convert placed beat from file into an array
		// -------------------------------------------

		Scanner scanPlacedBeats = new Scanner(placedBeatsFile);
		ArrayList<Integer> placedBeatsData = new ArrayList<Integer>() ;
		while(scanPlacedBeats.hasNextLine()){
			placedBeatsData.add(Integer.parseInt(scanPlacedBeats.nextLine()));
		}
		scanPlacedBeats.close();

		// remove the first beat in user placed beats
		placedBeatsData.remove(0);

		// convert array list into an array of a set size
		placedBeatsArray = new int[placedBeatsData.size()];

		ListIterator<Integer> placedBeatsIterator = placedBeatsData.listIterator(0);
		for (int i = 0; placedBeatsIterator.hasNext(); ++i) {
			placedBeatsArray[i] = placedBeatsIterator.next() - 400;
		}




		// --------------------------------------------
		// Convert computer beat from file into an array
		// --------------------------------------------

		Scanner scanComputerBeats = new Scanner(computerBeatsFile);
		ArrayList<Integer> computerBeatsData = new ArrayList<Integer>() ;
		while(scanComputerBeats.hasNextLine()){
			computerBeatsData.add(Integer.parseInt(scanComputerBeats.nextLine()));
		}
		scanComputerBeats.close();

		// convert array list into an array of a set size
		// use a temporary array
		int[] temporary = new int[computerBeatsData.size()];

		ListIterator<Integer> computerBeatsIterator = computerBeatsData.listIterator(0);
		for (int i = 0; computerBeatsIterator.hasNext(); ++i) {
			temporary[i] = computerBeatsIterator.next();
		}


		// the next two four loops are done to remove excess values after the song has finished playing
		int counter = 0;
		for (int i = 0; i < temporary.length; ++i) {
			if (temporary[i] > placedBeatsArray[placedBeatsArray.length - 1] + errorAllowable) 
				break;
			else
				++counter;
		}

		computerBeatsArray = new int[counter];

		for (int i = 0; i < computerBeatsArray.length; ++i) {
			computerBeatsArray[i] = temporary[i];
		}











		// create the datasets
		XYSeries placedBeatsSeries = new XYSeries("Placed Beats");
		XYSeries computerBeatsSeries = new XYSeries("Computer Beats");

		// load up the data for each series
		for (int i = 0; i < placedBeatsArray.length; ++i) {
			placedBeatsSeries.add(placedBeatsArray[i], 0);
		}
		for (int i = 0; i < computerBeatsArray.length; ++i) {
			computerBeatsSeries.add(computerBeatsArray[i], 0);
		}


		// plot the results
		XYSeriesCollection dataset = new XYSeriesCollection();
		dataset.addSeries(placedBeatsSeries);
		dataset.addSeries(computerBeatsSeries);
		JFreeChart scatterPlot = ChartFactory.createScatterPlot(
				"Beat Placements", // Chart title
				"time (seconds)", // X-Axis Label
				"", // Y-Axis Label
				dataset // Dataset for the Chart
				);
		XYItemRenderer renderer = scatterPlot.getXYPlot().getRenderer();
		renderer.setSeriesShape(0, (Shape)(new Rectangle2D.Float(0, 0, 1, 15)));
		renderer.setSeriesShape(1, (Shape)(new Rectangle2D.Float(0, 0, 1, 15)));
		ChartFrame frame = new ChartFrame("Doesn't Matter", scatterPlot);
		frame.setVisible(true);
		frame.setSize(1500, 500);






		//-------------------------
		//Calculating the F-measure
		//-------------------------

		// create an boolean array to show where partner beats have been found within the first loop
		// this is done to speed up caluclations
		boolean[] partnered = new boolean[computerBeatsArray.length];

		// First iterate through the placementBeats array, trying to find a partner for each beat.
		// If a partner isn't found within the allowed window of 70? milliseconds, it counts as a false
		// negative
		// If a partner is found, it counts as a hit
		boolean partnerFound = false;
		for (int i = 0; i < placedBeatsArray.length; ++i) {

			partnerFound = false;
			
			for (int j = 0; j < computerBeatsArray.length && computerBeatsArray[j] < placedBeatsArray[i] + errorAllowable; ++j) {
				if (Math.abs(computerBeatsArray[j] - placedBeatsArray[i]) < errorAllowable) {
					++hits;
					partnered[j] = true;
					partnerFound = true;
					break;
				}
			}
			if (!partnerFound)
				++falseNegative;

		}


		// now we iterate through the computerBeats array, but only through the values that weren't
		// assigned a partner. We label these as false positives
		for (int i = 0; i < partnered.length; ++i) {
			if(!partnered[i])
				++falsePositive;
		}




		System.out.println(hits);
		System.out.println(falsePositive);
		System.out.println(falseNegative + "\n");
		
		double precision = (double)hits / (hits + falsePositive);
		double recall = (double)hits / (hits + falseNegative);
		double fmeasure = 2 * (precision * recall) / (precision + recall);
		
		System.out.println("precision = " + precision);
		System.out.println("recall = " + recall);
		System.out.println("fmeasure = " + fmeasure);



	}

}
