# YeastLineProfiler

ImageJ plugin to analyse fluorescent dots in timelapse images from a Deltavision microscope. The plugin asks the user to draw round the cell of interest at the correct position in the timelapse. The 2 brightest fluorescent dots in the red channel are automatically selected and measurments made of Intensity and area within the dots. The regions are then applied to the same position on the green channel to measure intensity in the green channel. A line profile is automatically drawn covering the red channel dots and transferred to the green channel. Maximum peak intensity and the line profile data is then output to a text file for offline analysis.

Installation
1.Ensure that the ImageJ version is at least 1.5 and the installation has Java 1.8.0_60 (64bit) or higher installed. If not download the latest version of ImageJ bundled with Java and install it.
2. The versions can be checked by opening ImageJ and clicking Help then About ImageJ.
3.Download the latest copy of Bio-Formats into the ImageJ plugin directory.
4. Create a directory in the C: drive called Temp (case sensitive)
5. Using notepad save a blank .txt files called Results.txt and LineResults.txt into the Temp directory you previously created (also case sensitive).
Place DV_DotCounter.jar into the plugins directory of your ImageJ installation.
