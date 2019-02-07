import java.awt.Color;
import java.awt.Font;
import java.awt.Polygon;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import javax.swing.JOptionPane;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.Line;
import ij.gui.Plot;
import ij.gui.PlotWindow;
import ij.gui.ProfilePlot;
import ij.gui.Roi;
import ij.gui.WaitForUserDialog;
import ij.measure.ResultsTable;
import ij.plugin.*;
import ij.plugin.filter.Analyzer;
import ij.plugin.frame.RoiManager;
import ij.process.FloatPolygon;
import ij.process.ImageProcessor;

public class Dots_Lines_ implements PlugIn{
	ImagePlus Greeny;
	ImagePlus Reddy;
	int GreenyID;
	int ReddyID;
	double [] xval = new double[300];
	double [] yval = new double[300];
	double[] thegreenarea = new double[10];
	double[] thegreenmean = new double[10];
	double[] theredarea = new double[10];
	double[] theredmean = new double[10];
	double[] redmax = new double[2];
	double redmin;
	double redmean;
	double[] greenmax = new double[2];
	double greenmin;
	double greenmean;
	double redfirstmax = 0;
	double Greenfirstmax = 0;
	double Greensecondmax = 0;
	int redfirstpos =0;
	int greenfirstpos =0;
	double redsecondmax = 0;
	int redsecondpos = 0;
	int greensecondpos = 0;
	int counter = 1;
	String filename;
	
	public void run(String arg) {
		
		/*
		 * Ask user to open an image and split the channels, requires
		 * Bio-Formats plugin to be installed. Also sets the measurements
		 * required for outputs
		 */
		new WaitForUserDialog("Open DV Image", "Open DV Images. SPLIT CHANNELS!").show();
		IJ.run("Bio-Formats Importer");
		IJ.run("Set Measurements...", "area mean min centroid redirect=None decimal=2");
		
		/*
		 * Assign the filename to a variable so that the 
		 * results can be labelled in the output text file
		 */
		ImagePlus imp = WindowManager.getCurrentImage();
    	filename = imp.getTitle(); 	//Get file name
		int dotindex = filename.indexOf('.');		
		filename = filename.substring(0, dotindex + 4);
		
		/*
		 * Get the ID list of all the open image windows,
		 * Make sure that only 2 channels were aquired and
		 * they were acquired in the order green then red
		 */
		int[] Idlist = new int[2];
    	Idlist = WindowManager.getIDList();
    	IJ.selectWindow(Idlist[0]);
    	int maxLoop = Idlist.length;
      	
    	for (int x=0; x<maxLoop; x++){
    		int zproject = Idlist[x];
    		IJ.selectWindow(zproject);
    		imp = WindowManager.getCurrentImage();
    		IJ.run(imp, "Z Project...", "projection=[Max Intensity] all");
    		   
    		if(x==0){
    			Greeny = WindowManager.getCurrentImage();
    			GreenyID = Greeny.getID();
    			IJ.run(Greeny, "Set Scale...", "distance=1 known=1 pixel=1 unit=micron");
    		}
    		if(x==1){
    			Reddy = WindowManager.getCurrentImage();
    			ReddyID = Reddy.getID();
    			IJ.run(Reddy, "Set Scale...", "distance=1 known=1 pixel=1 unit=micron");
    		}
    		
    	}
		
    	
    	SelectCells();//Choose cells and analyse them
    	new WaitForUserDialog("Finished", "All Done").show();
	}
	
	public void SelectCells(){
		String response; 
		
		IJ.selectWindow(ReddyID);
		IJ.setTool("freehand");
		 
		/*
		 * Main method to identify the dots from within the selected
		 * cell, measure the intensities, area and get the XY coordinates.
		 * Once the measurements are made the method passes the XY coordinates 
		 * on to the Line profile method to draw the profile and find peaks.
		 */
		do{
			IJ.selectWindow(ReddyID);
			new WaitForUserDialog("Pick One", "Select Timepoint and Draw Round a Cell").show();
			int tPoint = Reddy.getCurrentSlice();	
			IJ.run("ROI Manager...", "");	
			IJ.setAutoThreshold(Reddy, "Yen dark");
			RoiManager rm = new RoiManager();
			rm = RoiManager.getInstance();
			IJ.run(Reddy, "Analyze Particles...", "size=1.5-Infinity pixel display clear add slice");	
			int numROI = rm.getCount();
			ResultsTable rt = new ResultsTable();
			rt = Analyzer.getResultsTable();
			int numvals = rt.getCounter();
			double[] linexval = new double[numvals];
			double[] lineyval = new double[numvals];
			for (int a=0; a<numvals; a++){
				//RED VALUES
				theredarea[a] = rt.getValueAsDouble(0, a);
				theredmean[a] = rt.getValueAsDouble(1, a);
				xval[counter] = rt.getValueAsDouble(6, 0);
				yval[counter] = rt.getValueAsDouble(7, 0);
				linexval[a] = rt.getValueAsDouble(6, a);
				lineyval[a] = rt.getValueAsDouble(7, a);
			}
			
			setImageNumbers(); //Call method to add cell number to red image as marker		
			
			IJ.selectWindow(GreenyID);
			Greeny.setZ(tPoint);
			IJ.setAutoThreshold(Greeny, "Default");
			double theMax = Greeny.getDisplayRangeMax();
			
			//Green Values
			for (int c = 0; c<numROI; c++){
					rm.select(c);
					IJ.setThreshold(Greeny, 0, theMax);
					IJ.run(Greeny, "Analyze Particles...", "size=0-Infinity display clear");		
					thegreenarea[c] = rt.getValueAsDouble(0, 0);
					thegreenmean[c] = rt.getValueAsDouble(1, 0);
			}
			LineProfiler(linexval,lineyval,numvals);//Calls the Line Profile method	

			outputinfo(numvals); //Calls method to output intensity measurements etc
			counter++;//Increment counter to keep a check on how many cells have been counted
			response = JOptionPane.showInputDialog("Another y/n");//Check to see if user is finished
		}while(response.equals("y"));
	}
	
	public void setImageNumbers(){
		
		/*
		 * Method to update the red image with
		 * the number of cells counted The nuber 
		 * text is placed next to the cell to which it
		 * relates
		 */
		int numPoints = Reddy.getNChannels();
		IJ.setForegroundColor(255, 255, 255);
		ImageProcessor ip = Reddy.getProcessor();
			Font font = new Font("SansSerif", Font.PLAIN, 18);
			ip.setFont(font);
			ip.setColor(new Color(255, 255, 0));
			String cellnumber = String.valueOf(counter);
			int xpos = (int) xval[counter];
			int ypos = (int) yval[counter];
			
			for (int g=1; g<numPoints+1; g++){
				Reddy.setC(g);
				ip.drawString(cellnumber, xpos, ypos);
				Reddy.updateAndDraw();
			}
	}
	
	public void outputinfo(int numvals){
		
		/*
		 * Method to output the peaks found for each counted cell
		 * in the form of a text file which can be read by excel 
		 * or in R 
		 */
		String CreateName = "C:\\Temp\\Results.txt";
		String FILE_NAME = CreateName;
	
		try{
			FileWriter fileWriter = new FileWriter(FILE_NAME,true);
			BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
			bufferedWriter.newLine();
			bufferedWriter.write(" File= " + filename + " Cell " + counter );
			for (int d = 0; d < numvals; d++){
				bufferedWriter.newLine();
				bufferedWriter.write(" Dot Measurements");
				bufferedWriter.newLine();
				bufferedWriter.write(" Green Dot Area " + thegreenarea[d] + " Red Dot Area " + theredarea[d] + " Green Dot Intensity " + thegreenmean[d] + " Red Dot Intensity " + theredmean[d]  );
			}
			bufferedWriter.newLine();
			bufferedWriter.write(" Line Profile Measurements");
			bufferedWriter.newLine();
			bufferedWriter.write(" Red LineProfile Mean " + redmean + " Green LineProfile Mean " + greenmean + " Red LineProfile Max1 " + redfirstmax +" Red LineProfile Max2 " + redsecondmax + " Green LineProfile Max1 " + Greenfirstmax + " Green LineProfile Max2 " + Greensecondmax);
			bufferedWriter.newLine();
			bufferedWriter.close();

		}
		catch(IOException ex) {
            System.out.println(
                "Error writing to file '"
                + FILE_NAME + "'");
        }
	} 
	
	public void outputlineprofile(double[] reddata, double[] greendata){
		
		/*
		 * Method to output all the values from the plot
		 * profiles from the red and green channels into 
		 * a text file in case further analysis is required
		 */
		String CreateName = "C:\\Temp\\LineResults.txt";
		String FILE_NAME = CreateName;
		try{
			FileWriter fileWriter = new FileWriter(FILE_NAME,true);
			BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
			bufferedWriter.newLine();
			bufferedWriter.newLine();
			bufferedWriter.write(" File= " + filename + " Cell " + counter );
			bufferedWriter.newLine();
			int valnums = greendata.length;
			for(int e = 0; e<valnums;e++){
				bufferedWriter.newLine();
				bufferedWriter.write(" ProfilePostion " + e + " Red Value " + reddata[e] + " Green Value " + greendata[e]);
			}
			bufferedWriter.close();
		}
		catch(IOException ex) {
            System.out.println(
                "Error writing to file '"
                + FILE_NAME + "'");
        }
	}
	
	public void LineProfiler(double [] linexval, double [] lineyval, int numvals){
		int[] Yvalue;
		int[] Xvalue;
		
		/*
		 * In case there are more than 2 dots, find 
		 * the 2 biggest area values which will be the 
		 * dots of interest
		 */
		double max = 0;
		int pos1 = 0;
		double nextmax = 0;
		int pos2 = 1;
		
		if (numvals>2){							
			//Find max area value
			for (int a=0;a<numvals;a++){
				if (theredarea[a] > max){
					max = theredarea[a];
					pos1 = a;
				}
			}
			//Find second largest area value
			for (int a=0;a<numvals;a++){
				if (theredarea[a] >= nextmax && theredarea[a] <= max){
					nextmax = theredarea[a];
					pos2 = a;
				}
			}
		 }	 
		
		//********************************************
		//*Produce line plot to cover the 2 red dots *
		//********************************************
		IJ.selectWindow(ReddyID);
		
		//**********************************************
		//Calculate extension to plot to cover whole dot
		//**********************************************
		
		//Gradient of line
		double Xgrad = linexval[pos1]-linexval[pos2];
		double Ygrad = lineyval[pos1] - lineyval[pos2];
		//	double theGradient = Xgrad/Ygrad;
		double theslope = Ygrad/Xgrad;

		//Find Line Length
		double xlength = Math.abs(linexval[pos1]-linexval[pos2]);
		double ylength = Math.abs(lineyval[pos1]-lineyval[pos2]);
		double len = Math.sqrt((xlength*xlength)+(ylength*ylength));

		//solve equation y=mx+c for c (y intercept)
		double intercept = lineyval[pos1]-(theslope*linexval[pos1]);

		double newx1 = linexval[pos1] + (linexval[pos1]-linexval[pos2]) / (len * 0.1);
		double newy1 = lineyval[pos1] + (lineyval[pos1]-lineyval[pos2]) / (len * 0.1);

		double newx2 = linexval[pos2] + (linexval[pos2]-linexval[pos1]) / (len * 0.1);
		double newy2 = lineyval[pos2] + (lineyval[pos2]-lineyval[pos1]) / (len * 0.1);

		linexval[pos1] = newx1;
		lineyval[pos1] = newy1;
		linexval[pos2] = newx2;
		lineyval[pos2] = newy2;
				
		/*
		 * Set the coordinates of the line based on the
		 * calculations above
		 */
		Reddy.setRoi(new Line(linexval[pos1], lineyval[pos1], linexval[pos2], lineyval[pos2]));
		/*
		 * Apply the line profile and get 
		 * the line values
		 */
		Roi RoiPos = Reddy.getRoi();
		Polygon thevals = RoiPos.getPolygon();
		FloatPolygon p = RoiPos.getInterpolatedPolygon();
		int numpoints = p.npoints;
		float[] XpointVal = p.xpoints;
		float[] YpointVal = p.ypoints;
		
		boolean averageHorizontally = false; // true
		ProfilePlot redprofile = new ProfilePlot(Reddy, averageHorizontally );
		double[] reddata = redprofile.getProfile();
		
		Plot redplot = redprofile.getPlot();
		PlotWindow redpw = redplot.show();
		
		//**************************************************************
		//*Use red line plot to cover the same area on the green image *
		//**************************************************************
		IJ.selectWindow(GreenyID);
		Greeny.setRoi(new Line(linexval[pos1], lineyval[pos1], linexval[pos2], lineyval[pos2]));
		ProfilePlot greenprofile = new ProfilePlot(Greeny, averageHorizontally );
		double[] greendata = greenprofile.getProfile();
		Plot greenplot = greenprofile.getPlot();
		PlotWindow greenpw = greenplot.show();
		RedPeaks(reddata);		
		GreenPeaks(greendata);	
		outputlineprofile(reddata,greendata);
		
	}
	
	public void RedPeaks(double[] reddata) {
		double redtotal = 0;
		int numvals = reddata.length;
		
		//Calculate means from line profiles
		for (int a=0;a<numvals-1;a++){
			redtotal = redtotal + reddata[a];
		}
		redmean = redtotal/numvals;
		
		//************************
		//Peak Detect in Red line*
		//************************
		double current =0;
		int count = 0;
		int [] redpeak = new int[150];
		double [] redintensityvalue = new double [150];
		for (int b =4; b<numvals-4;b++){
			current = reddata[b];
			double precede = reddata[b-1]+reddata[b-2]+reddata[b-3]+reddata[b-4];
			double after = reddata[b+1]+reddata[b+2]+reddata[b+3]+reddata[b+4];
			//Find delta
			double deltaprevious = current - (precede/4);
			double deltaafter = current - (after/4);
			double early = reddata[b]/precede;
			double late = reddata[b]/after;
			if (deltaprevious > 0 && deltaafter > 0){
				if (early < 0.6 && late < 0.6){
					redpeak[count] = b;
					redintensityvalue[count] = current;
					count++;
				}
			}
			
		}
		//Find the high point of the separate red peaks
		int grouping = 0;
		int countcheck = 0;
		int groupcount = 0;
		grouping = redpeak[countcheck];
		double highval = 0;
		int highpos = 0;
		double [] thegroupsintensity = new double[50];
		double [] thegroupsposition = new double[50];
		for (int c =0; c< count; c++){
			do{		
				double currentval = redintensityvalue[countcheck];
				if (currentval>highval){
					highval = currentval;
					highpos = redpeak[countcheck];
				}
				countcheck++;
			} while (countcheck>0 && redpeak[countcheck]-redpeak[countcheck-1]==1);
			thegroupsintensity[groupcount]= highval;
			thegroupsposition[groupcount]= highpos;
			groupcount++;
			highval = 0;
			c = countcheck;
		}
		//Find 2 biggest red peaks
		redfirstmax = 0;
		double first = 0;
		redfirstpos =0;
		redsecondmax = 0;
		double second = 0;
		redsecondpos =0;
		
		for (int d =0; d<groupcount;d++){
			first = thegroupsintensity[d];
			if(first> redfirstmax){
				redfirstmax = first;
				redfirstpos = (int) thegroupsposition[d];
			}
		}
		for (int d =0; d<groupcount;d++){
			second = thegroupsintensity[d];
			if(second > redsecondmax && second<redfirstmax){
				redsecondmax = second;
				redsecondpos = (int) thegroupsposition[d];
			}
		}
		
	}
	
	public void GreenPeaks(double[] greendata) {
		double greentotal = 0;
		int numvals = greendata.length;
	
		//Calculate means from line profiles
		for (int a=0;a<numvals-1;a++){
			greentotal = greentotal + greendata[a];
		}	
	
		greenmean = greentotal/numvals;
	
		//************************
		//Peak Detect in Green line*
		//************************
		double current =0;
		int count = 0;
		int [] greenpeak = new int[150];
		double [] greenintensityvalue = new double [150];
		for (int b =4; b<numvals-4;b++){
			current = greendata[b];
			double precede = greendata[b-1]+greendata[b-2]+greendata[b-3]+greendata[b-4];
			double after = greendata[b+1]+greendata[b+2]+greendata[b+3]+greendata[b+4];
			//Find delta
			double deltaprevious = current - (precede/4);
			double deltaafter = current - (after/4);
			double early = greendata[b]/precede;
			double late = greendata[b]/after;
			if (deltaprevious > 0 && deltaafter > 0){
				if (early < 0.6 && late < 0.6){
					greenpeak[count] = b;
					greenintensityvalue[count] = current;
					count++;
				}
			}
		
		}
		//Find the high point of the separate green peaks
		int grouping = 0;
		int countcheck = 0;
		int groupcount = 0;
		grouping = greenpeak[countcheck];
		double highval = 0;
		int highpos = 0;
		double [] thegroupsintensity = new double[50];
		double [] thegroupsposition = new double[50];
		for (int c =0; c< count; c++){
			do{		
				double currentval = greenintensityvalue[countcheck];
				if (currentval>highval){
					highval = currentval;
					highpos = greenpeak[countcheck];
				}
			countcheck++;
			} while (countcheck>0 && greenpeak[countcheck]-greenpeak[countcheck-1]==1);
			thegroupsintensity[groupcount]= highval;
			thegroupsposition[groupcount]= highpos;
			groupcount++;
			highval = 0;
			c = countcheck;
		}
		//Find 2 biggest green peaks
		redfirstmax = 0;
		double first = 0;
		greenfirstpos =0;
		Greensecondmax = 0;
		double second = 0;
		greensecondpos =0;
	
		for (int d =0; d<groupcount;d++){
			first = thegroupsintensity[d];
			if(first> redfirstmax){
				Greenfirstmax = first;
				greenfirstpos = (int) thegroupsposition[d];
			}
		}
		for (int d =0; d<groupcount;d++){
			second = thegroupsintensity[d];
			if(second > Greensecondmax && second<Greenfirstmax){
				Greensecondmax = second;
				greensecondpos = (int) thegroupsposition[d];
			}	
		}
	}
}
