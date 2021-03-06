package com.main.facialRecognition;

import java.awt.FlowLayout;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.videoio.VideoCapture;

import com.main.eyeRecognition.EyeReader;

public class FacialRecognition implements Runnable
{
	private CascadeClassifier faceDetector;
	private CascadeClassifier eyeDetector;
	private VideoCapture capture;
	private BufferedImage faceFrame;
	private EyeRect eye;
	private File faceVideo;
	
	private int faceCounter;
	private int faceVideoCounter;
	private Mat frame;
	private EyeReader eyeReader;
	private boolean eyesOpen = true;
	private boolean training = false;
	private boolean open = true;
	private boolean isRecording = false;
	private boolean currentStream = false;
	
	private Thread thread;
	private boolean running;
	
	//loads opencv lib, cascadeclassifiers for eyes and face, and opens videostream to webcam
	public FacialRecognition() {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		
		faceCounter = 0;
		faceVideoCounter = 0;
		frame = new Mat();
		faceVideo = new File("res/FaceVideo");
		faceDetector = new CascadeClassifier("lbpcascade_frontalface.xml");
		eyeDetector = new CascadeClassifier("haarcascade_lefteye_2splits.xml");
		faceFrame = new BufferedImage(1, 1, BufferedImage.TYPE_3BYTE_BGR);
		capture = new VideoCapture();
		capture.open(0);
	}
	
	@Override
	public void run() {
		
		eyeReader = new EyeReader();
		JFrame window = new JFrame("Facial Recognition");
		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		window.setSize(500, 500);
		window.getContentPane().setLayout(new FlowLayout());
		JLabel label = new JLabel();
		window.getContentPane().add(label);
		window.setVisible(true);
		long startTime = System.nanoTime();
		double dTime = (double) (startTime)/ 1000000000;
		
		while(running) {
			if(dTime%1000 == 0) {
				runFacialRecognition();
				label.setIcon(new ImageIcon(faceFrame));
				//System.out.println("Displaying face " + faceVideoCounter);	
			}
				
			long currentTime = System.nanoTime();
			dTime = (double)(currentTime - startTime) / 1000000000;
			//System.out.println("Elapsed Time: " + dTime + " seconds");
			startTime = currentTime;
		}
	}
	
	public synchronized void start() {
		if (running) return;
		thread = new Thread(this);
		thread.start();
		running = true;
	}
	
	public void halt() {
		capture.release();
		running = false;
	}
	
	public void runFacialRecognition() {
		if(currentStream) {
			readFromFile();
		}
		else if(!currentStream) {
			readFromCamera();
		}
	}
	
	public void readFromCamera() {
		
		//System.out.println("Reading from Camera");
		if(capture.isOpened()) {
			if(!isRecording) {
				capture.read(frame);
				if(!frame.empty()) {
					matToImage(frame);
					if(detectFaces(frame)) {
						detectEyes(frame);
						if(eye != null) {
							//System.out.println(eye.toString());
						}
					} else {
						eye = null;
					}
				}
			} else {
				capture.read(frame);
				recordVideo(frame);
			}
		} else {
			System.err.println("Error: No video device could be opened");
			System.exit(1);
		}
	}
	
	public void readFromFile() {
		//System.out.println("Reading From File");
		if(faceVideo.listFiles().length > 0 && faceVideoCounter < faceVideo.listFiles().length) {
		
			frame = Imgcodecs.imread(faceVideo.listFiles()[faceVideoCounter%faceVideo.listFiles().length].getPath());
			if(!frame.empty()) {
				matToImage(frame);
				if(detectFaces(frame)) {
					detectEyes(frame);
					if(eye != null) {
						//System.out.println(eye.toString());
					}
				} 
				else {
					eye = null;
				}
			}
			faceVideoCounter++;
			
		}
		else if(faceVideoCounter >= faceVideo.listFiles().length){
			faceVideoCounter = 0;
		}
		else {
			System.out.println("No face video file exists");
		}
	}
	
	public void recordVideo(Mat frame) {
		Imgcodecs.imwrite("res/FaceVideo/face" + faceCounter + ".png", frame);
		faceCounter++;
		System.out.println(faceCounter + " Written to File");
	}
	//converts Mat to array of bytes to a buffered image for easier display
	public void matToImage(Mat original) {
		int width = original.width();
		int height = original.height();
		int channels = original.channels();
		byte[] sourcePixels = new byte[width*height*channels];
		original.get(0, 0, sourcePixels);
		faceFrame = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
		final byte[] targetPixels = ((DataBufferByte) faceFrame.getRaster().getDataBuffer()).getData();
		System.arraycopy(sourcePixels, 0, targetPixels, 0, sourcePixels.length);
	}
	//uses opencv to analyze frame and compare it to the cascade classifier
	public boolean detectFaces(Mat frame) {
		Mat mRBG = new Mat();
		Mat mGrey = new Mat();
		MatOfRect faces = new MatOfRect();
		frame.copyTo(mRBG);
		frame.copyTo(mGrey);
		Imgproc.cvtColor(mRBG, mGrey, Imgproc.COLOR_BGR2GRAY);
		Imgproc.equalizeHist(mGrey, mGrey);
		faceDetector.detectMultiScale(mGrey, faces);
		
		return faces.toArray().length > 0;
	}
	//uses opencv to detect eyes from cascade classifier
	public void detectEyes(Mat frame) {
		Mat mRBG = new Mat();
		Mat mGrey = new Mat();
		MatOfRect eyes = new MatOfRect();
		frame.copyTo(mRBG);
		frame.copyTo(mGrey);
		Imgproc.cvtColor(mRBG, mGrey, Imgproc.COLOR_BGR2GRAY);
		Imgproc.equalizeHist(mGrey, mGrey);
		eyeDetector.detectMultiScale(mGrey, eyes);
		Rect[] eyeArray = eyes.toArray();
		
		if(eyeArray.length > 0) {
			eye = new EyeRect(eyeArray[0]);
			// Export to file system for now
			BufferedImage eyeImg = faceFrame.getSubimage(eye.getX(), eye.getY(), eye.getWidth(), eye.getHeight());
			eyesOpen = eyeReader.isOpen(eyeImg);
			
			//Adding to the training set
			if (training) {
				int i = 0;
				String path = "res/EyeImages/";
				if (open) {
					path += "Open";
				} else {
					path += "Closed";
				}
				File eyeSaveLocation = new File(path + i + ".png");
				while (eyeSaveLocation.exists()) {
					i++;
					eyeSaveLocation = new File(path + i + ".png");
				}
				try {
					ImageIO.write(eyeImg, "png", eyeSaveLocation);
					System.out.println("Wrote eye to file. [" + eyeSaveLocation.getAbsolutePath() + "]");
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
			
		}
	}

	public void toggleRecording() {
		isRecording = !isRecording;
	}
	
	public void toggleStream() {
		currentStream = !currentStream;
	}
	//getter function for the faceFrame
	public BufferedImage getFaceFrame() {
		return faceFrame;
	}
	//getter function for the eyeRect/location
	public EyeRect getEyeRect() {
		return eye;
	}
	
	public boolean getCurrentStream() {
		return currentStream;
	}
	
	public boolean getIsRecording() {
		return isRecording;
	}
	public boolean eyesOpen () {
		return eyesOpen;
	}
	
	public void toggleTraining () {
		training = !training;
	}
	public void toggleOpen () {
		open = !open;
	}
	public boolean getTraining () {
		return training;
	}
	public boolean getOpen () {
		return open;
	}
	public EyeReader getEyeReader () {
		return eyeReader;
	}
}