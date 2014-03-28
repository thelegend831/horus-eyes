package trilaceration;
import javax.swing.*;

import java.awt.*;
import java.awt.image.*;
import java.util.*;

import location.geoengine.GeoPosition;

public class LocationArea {
	private double       lati,longi;// coordenadas do canto superior esquerdo da area
	private double       latf,longf;// coordenadas do canto inferior direito da area
	private int        width,height;// largura e altura da area em analise
	private double    deltaX,deltaY;// Variacao de pixels no eixo X e Y
	private int              status;// flag para indicar se o elemento a ser localizado ja foi detectado por algum sensor
	private int               xc,yc;// Centro da area em torno da posicao estimada do dispositivo
	private int               ratio;// Raio em torno da possicao estimada
	private BufferedImage       img;// Imagem  da area provavel
	private ArrayList<Point>  shape; 

	public static final int NO_POSITION = 0; 
	public static final int DETECTED    = 1; 
	public static final int SX[]={ 0, 1, 1, 1, 0,-1,-1,-1};
	public static final int SY[]={-1,-1, 0, 1, 1, 1, 0,-1};
	
	/**
	 * 
	 * @param lati
	 * @param longi
	 * @param latf
	 * @param longf
	 * @param width
	 * @param height
	 */
	public LocationArea(double lati,double longi,double latf,double longf,int width,int height) {
		super();
		// Seta parametros
		this.lati   = lati;
		this.longi  = longi;
		this.latf   = latf;
		this.longf  = longf;
		this.width  = width;
		this.height = height;
		img = new BufferedImage(width,height,java.awt.image.BufferedImage.TYPE_INT_RGB);
		Graphics g = img.createGraphics();
		g.setColor( Color.WHITE );
		g.fillRect( 0, 0, width, height );
		//g.setColor( Color.BLACK );
		//g.drawLine( 0, 0, width, height );
		deltaX = (latf - lati)/width;
		deltaY = (longf - longi)/height;
		
		status = NO_POSITION;
	}
	/**
	 * 
	 * @param x
	 * @param y
	 * @param ratio
	 * @param color
	 * @param image
	 */
	private void drawCicle(int x,int y, int ratio,Color color,BufferedImage image){
		Graphics g = image.createGraphics();
		g.setColor( color );
		// desenha o circulo
		g.fillOval(x - ratio, y - ratio,2*ratio, 2*ratio);
	}
	/**
	 * 
	 * @param image
	 */
	private void clearImage(BufferedImage image){
		Graphics g = image.createGraphics();
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, this.width, this.height);
		
	}
	/**
	 * 
	 * @param x
	 * @param y
	 * @param ratio
	 * @param color1
	 * @param color2
	 * @param img1
	 * @param img2
	 * @return
	 */
	private BufferedImage getIntersectionImage(int x,int y, int ratio,Color color1,Color color2,BufferedImage img1,BufferedImage img2){
		int              i,j;
		BufferedImage result;
		int      c1,c2,c3,c4;
		Point          point;
	
		result = new BufferedImage(width,height,java.awt.image.BufferedImage.TYPE_INT_RGB);
		shape  = new ArrayList<Point>();
		
		clearImage(result);
		c3 = color1.getRGB();
		c4 = color2.getRGB();
		// Percorre a area em torno do circulo da imagem 1
		for (i =(x -ratio);i < (x + ratio + 1);i++){
			for( j=(y-ratio); j <(y + ratio +1); j++){
				c1 = img1.getRGB(i, j);
				c2 = img2.getRGB(i, j);
				//System.out.printf("%d %d\n",c1,c2);
				// verifica se o pixel da imagem 1 e na imagem 2 tem o pixel na cor pintada
				if( (c1 == c3) && (c2 == c4) ){
					result.setRGB(i, j, Color.BLUE.getRGB());
					point = new Point( i,j );
					shape.add(point);
				}
			}
		}
		return result;
	}
	/**
	 * 
	 * @param sensor
	 * @param ratio
	 */
	public void addSensorDetection(SensorPosition sensor,double ratio){
		int               xs,ys;
		int                  rs;
		Double               db;
		BufferedImage      img1;
		// TODO Verifica se o sensor estar dentro da area
		
		// Calcula coordenadas dentro da imagem
		db = new Double( (sensor.getLatitude() - lati)/deltaX);
		xs = db.intValue();
		db = new Double( (sensor.getLongitude() - longi)/deltaY);
		ys = db.intValue();
		// TODO O Raio devera ser convertido para metros
		db = new Double( (ratio /(latf - lati))*width);
		rs = db.intValue();
		// TODO verifica se parametros sao validos
		if( status == LocationArea.NO_POSITION){
			clearImage(this.img);
			// Primeira vez que esta sendo detectado a area possivel e  a mesma da estacao
			drawCicle(xs,ys,rs,Color.BLUE,this.img);
			xc    = xs;
			yc    = ys;
			ratio = rs;
			status = LocationArea.DETECTED;
			
		}
		else{
			img1 = new BufferedImage(width,height,java.awt.image.BufferedImage.TYPE_INT_RGB);
			drawCicle(xs,ys,rs,Color.BLUE,img1);
			// Calcula a imagem interseccao entre as duas imagens
			img = getIntersectionImage(xs,ys, rs,Color.BLUE,Color.BLUE,img1,this.img);
		}
		
	}
	
	public void showCentroide(){
		// calcula o centroide da interseccao
		calculateCentroid();
		// calcula o raio que centrado no centroide engloba todos os pontos
		calculateRatioCentroid();
		drawCentroide(Color.RED);
		
	}
	
	public Centroide getCentroide(){
		Centroide  centroide;
		// calcula o centroide da interseccao
		calculateCentroid();
		// calcula o raio que centrado no centroide engloba todos os pontos
		calculateRatioCentroid();
		centroide = new Centroide(ScaleConverter.convertToLatitude(xc),
				                  ScaleConverter.convertToLongitude(yc),
				                  ratio);
		return centroide;
		
	}
	/**
	 * 		
	 * @return
	 */
	public ImageIcon getImagem() {
		
		BufferedImage buffer = img;
		return new ImageIcon( buffer );
    }
	/**
	 * 
	 * @return
	 */
	private Point calculateCentroid(){
		int                    n;
		Iterator<Point> iterator;
		Point              point;

		xc = yc = n = 0;
		iterator = shape.iterator();		
		while(iterator.hasNext()){
			point = iterator.next();
			xc = xc +point.getX();
			yc = yc + point.getY();
			n++;
		}
		xc = xc / n;
		yc = yc / n;
		point = new Point(xc,yc);
		return point;
		
	}
	/**
	 * 
	 * @return
	 */
	private long calculateRatioCentroid(){
		Iterator<Point> iterator;
		Point              point;
		double          distance;

		ratio = 0;
		iterator = shape.iterator();		
		while(iterator.hasNext()){
			point = iterator.next();
			distance = Math.sqrt( Math.pow((point.getX()-xc),2.0) + Math.pow(((point.getY() - yc)),2.0));
			if (distance > ratio){
				ratio = (int)Math.round(distance);
			}
		}
		return ratio;
	}
	public void drawCentroide(Color color){
		Graphics g=img.getGraphics();
		
		g.setColor(color);
		g.drawLine(xc - 2, yc, xc + 2, yc);
		g.drawLine(xc, yc - 2, xc, yc + 2);
		g.drawOval(xc -ratio, yc-ratio, 2*ratio, 2*ratio);
	}
	/**
	 * @param args
	 */
	@SuppressWarnings("deprecation")
	public static void main(String[] args) {
	    BufferedImage   image;

		LocationArea   area   = new LocationArea (0,0,4,4,400,400);
	    SensorPosition sensor1 = new SensorPosition(1,1,0.5);
	    SensorPosition sensor2 = new SensorPosition(1.5,1,0.5);
	    SensorPosition sensor3 = new SensorPosition(1.25,1.5,0.5);
	    area.addSensorDetection(sensor1, 1);
	    area.addSensorDetection(sensor2, 1);
	    area.addSensorDetection(sensor3, 1);
	    area.showCentroide();
	    /**
		image = new BufferedImage(400,400,java.awt.image.BufferedImage.TYPE_INT_RGB);
	    
		ScaleConverter.latIni = 0.0;
	    ScaleConverter.longIni = 0.0;
	    ScaleConverter.latEnd = 4.0;
	    ScaleConverter.longEnd = 4.0;
	    ScaleConverter.height = 400;
	    ScaleConverter.width  = 400;
	    
	    ElectronicFence fence = new ElectronicFence();
	    
	    GeoPosition   p1,p2,p3,p4,p5;
	    
	    p1 = new GeoPosition(new Date(),0.3,0.2);
	    p2 = new GeoPosition(new Date(),0.3,1.3);
	    p3 = new GeoPosition(new Date(),2.3,1.3);
	    p4 = new GeoPosition(new Date(),2.3,0.2);
	   // p5 = new GeoPosition(new Date(),0.8,1.2);	    
	    
	    fence.addPoint(p1);
	    fence.addPoint(p2);
	    fence.addPoint(p3);
	    fence.addPoint(p4);
	    //fence.addPoint(p5);
	    
	    p1 = new GeoPosition(new Date(),0.1,0.1);
	    
	    boolean result = fence.isInsideFence(p1);
	    System.out.print(result);
	    fence.drawFenceArea(image);
		*/
		// TODO Auto-generated method stub
        JFrame frm = new JFrame("Teste Imagem");
        JPanel pan = new JPanel();
        JLabel lbl = new JLabel( area.getImagem() );
        //JLabel lbl = new JLabel( new ImageIcon( image ));
        pan.add( lbl );
        frm.getContentPane().add( pan );
        frm.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
        frm.pack();
        frm.show();		

	}

}
