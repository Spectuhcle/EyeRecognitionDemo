package com.main.facialRecognition;

import java.awt.geom.Point2D;

import org.opencv.core.Rect;
//container class for eye location on screen
class EyeRect {
	private Point2D.Double topLeft;
	private Point2D.Double topRight;
	private Point2D.Double botLeft;
	private Point2D.Double botRight;
	private int width;
	private int height;
	
	public EyeRect(Rect eye) {
		topLeft = new Point2D.Double(eye.x, eye.y);
		topRight = new Point2D.Double((eye.x + eye.width), eye.y);
		botLeft = new Point2D.Double(eye.x, (eye.y - eye.height));
		botRight = new Point2D.Double((eye.x + eye.width), (eye.y - eye.height));
		this.width = eye.width;
		this.height = eye.height;
	}
	
	public void printPoints() {
		System.out.println("topLeft: " + topLeft.toString());
		System.out.println("topRight: " + topRight.toString());
		System.out.println("botLeft: " + botLeft.toString());
		System.out.println("botRight: " + botRight.toString());
	}
	
	public Point2D getTopLeft() {
		return topLeft;
	}
	
	public Point2D getTopRight() {
		return topRight;
	}
	
	public Point2D getBotLeft() {
		return botLeft;
	}
	
	public Point2D getBotRight() {
		return botRight;
	}
	
	public int getWidth() {
		return width;
	}
	
	public int getHeight() {
		return height;
	}
}