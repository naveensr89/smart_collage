import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.LinkedList;

import java.lang.*;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.features2d.DMatch;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.imgproc.Imgproc;

class Collage extends JScrollPane {
    private static final long   serialVersionUID    = 1L;


	public static BufferedImage img ;
    public static int mouseClicked = -1, mouseClickX, mouseClickY;
    public static int fileCounter=0, collageLevel = 0, currentCategory = -1;
    public static byte BackgroundClr = 127, BackgroundClrSubcollage = 100;;

    public static int Wd = 352;
    public static int Ht = 288;
    public static int CollageSizeX = 3, CollageSizeY = 2;
    public static int CollageBarSizeWd = 40, CollageBarSizeHt = 40;
    public static int CollageWd = Wd * CollageSizeX + (CollageSizeX-1 + 2) * CollageBarSizeWd;
    public static int CollageHt = Ht * CollageSizeY + (CollageSizeY-1 + 2) * CollageBarSizeHt;
    public static int MaxFiles = 1000;
    public static int NumCategories = 15;
    // Allocate memory for image buffer
    public static byte[] bytes = new byte[(int) 3*Wd*Ht];

    public static String[] InputFileNames = new String[MaxFiles];
    public static int[] isVideo = new int[MaxFiles];
    public static int[][] CategoryFileIdx = new int[MaxFiles][MaxFiles];
    public static int[] CategoryFileCounter = new int[MaxFiles];
    public static int pageLevel0 = 0, pageLevel1 = 0;
    public static int maxPageLevel0 = 0xFFFFFFFF;
    public static int[] maxPageLevel1= new int[MaxFiles];

    
    public static JFrame frame;
    public static int ClickedCollagePosition = -1;
    public static int offset=0, frameCnt=0, playVideo=1;

    public static void main(String[] args) {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        // Get all the filenames in the folder
        
        if(args.length < 1)
        {
            System.out.println("\n Input Folder where RGB 352x288 Images are presetn");
            return;
        }
        final File folder = new File(args[0]);
        
        for (final File fileEntry : folder.listFiles())
        {
            if(fileCounter == MaxFiles)
            {
                System.out.printf("\n Handling only first %d images",MaxFiles);
                break;
            }
            int dot = fileEntry.getAbsolutePath().lastIndexOf('.');
            String ext = fileEntry.getAbsolutePath().substring(dot + 1);

            if (fileEntry.isFile() && (ext.equals("rgb") || ext.equals("raw") ))
            {
                InputFileNames[fileCounter] = args[0] + "\\" + fileEntry.getName();
                isVideo[fileCounter] = (fileEntry.length() > (Wd*Ht*3) ? 1 : 0);
                fileCounter++;
            }
        }
        
        // Analysing and separating into categories code
        UpdateCategory(InputFileNames, CategoryFileIdx, CategoryFileCounter, CollageSizeX, fileCounter);

        setMaxLevels();
        
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {

                createAndShowGUI();
            }
        });
    }

    private static void createAndShowGUI() {
        frame =  new JFrame("Awesome Collage");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(true);
        frame.add(new Collage());
        frame.setSize(CollageWd + 40, CollageHt + 40);

        Graphics2D    graphics = img.createGraphics();
        graphics.setPaint ( new Color ( BackgroundClr, BackgroundClr, BackgroundClr ) );
        graphics.fillRect ( 0, 0, img.getWidth(), img.getHeight() );
        graphics.dispose();

        frame.setVisible(true);
    }
    
    public Collage() {
		img = new BufferedImage(CollageWd, CollageHt,
				BufferedImage.TYPE_INT_RGB);
		
        JLabel label = new JLabel(new ImageIcon(img));
        setViewportView(label); // ********** changed *******

        setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        label.setSize(CollageWd + 100, CollageHt);

		new Timer(33, paintTimer).start();

		label.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				mouseClicked = 1;
				
				int mouseXonCollage = e.getX() - (label.getBounds().width - CollageWd)/2;
				int mouseYonCollage = e.getY() - (label.getBounds().height - CollageHt)/2;

				int[] ClickedX = new int[1], ClickedY = new int[1];

		        ClickedCollagePosition = GetCategory(mouseXonCollage, mouseYonCollage, CollageSizeX, CollageSizeY, Wd, Ht, CollageBarSizeWd, CollageBarSizeHt, ClickedX, ClickedY);

                if(SwingUtilities.isLeftMouseButton(e))
                {
                	// Check if Clicked in Edge
                	if(mouseXonCollage > (CollageWd - CollageBarSizeWd))
                	{
                		if(collageLevel == 0)
                		{
                			if(pageLevel0 < maxPageLevel0)
                				pageLevel0++;
                		}
                		else
                		{
                			if(pageLevel1 < maxPageLevel1[currentCategory])
                				pageLevel1++;
                		}
                		if(collageLevel == 2)
                			collageLevel = 1;
                	}
                	else if(mouseXonCollage < CollageBarSizeWd)
                	{
                		if(collageLevel == 0)
                		{
                			if(pageLevel0 > 0)
                				pageLevel0--;
                		}
                		else
                		{
                			if(pageLevel1 > 0)
                				pageLevel1--;
                		}
                		if(collageLevel == 2)
                			collageLevel = 1;                	
                	}
                	else
                	{	
                		int tempCategory = ClickedCollagePosition + pageLevel0 * CollageSizeX * CollageSizeY ;
						if (collageLevel == 0 && ClickedCollagePosition != -1 && CategoryFileCounter[tempCategory] > 0) 
						{
							collageLevel = 1;
			                currentCategory = ClickedCollagePosition + pageLevel0 * CollageSizeX * CollageSizeY ;
						}
						else if((collageLevel == 1 || collageLevel == 2) && ClickedCollagePosition != -1)
						{
							collageLevel = 2;
							playVideo = 1;
							offset = 0;
							frameCnt = 0;
						}
						else if(collageLevel == 2 && ClickedCollagePosition == -1)
						{
//							collageLevel = 1;
							playVideo = 0;
							offset = 0;
							frameCnt = 0;
						}
					}
                }
                else
                {
                    collageLevel = 0;
                    pageLevel1 = 0;
                }

				mouseClickX = mouseXonCollage;
				mouseClickY = mouseYonCollage;
			}
		});
	}
    
    public static void Resize(byte[] bytes, byte[] resized, int Wd, int Ht, int newWd, int newHt)
    {
        int avgR, avgG, avgB;
        int offsetG, offsetB;
        int offsetGSub, offsetBSub;
        int[] gaussianWdw = {1,2,1,2,4,2,1,2,1};
        int centerOffset = 4;
        
        offsetG = Wd*Ht;
        offsetB = 2*offsetG;
        offsetGSub = newWd*newHt;
        offsetBSub = 2*offsetGSub;

        int n=1;

        for(int newy=0; newy<newHt; newy++)
        {
            for (int newx = 0; newx < newWd; newx++)
            {
                
                int x = (Wd*newx/newWd);
                int y = (Ht*newy/newHt);

                avgR = avgG = avgB = 0;
                // Gaussian smoothing
                for (int i = -n; i <= n; i++)
                {
                    int yy = y + i;

                    if (yy < 0)
                        yy = 0;
                    if (yy > Ht - 1)
                        yy = Ht - 1;

                    for (int j = -n; j <= n; j++)
                    {
                        int xx = x + j;

                        if (xx < 0)
                            xx = 0;
                        if (xx > Wd - 1)
                            xx = Wd - 1;
                        {
                            int ind = xx + yy * Wd;
                            int kernelOffset = centerOffset + j + i * 3;

                            avgR += (bytes[ind] & 0xff)
                                    * gaussianWdw[kernelOffset];
                            avgG += (bytes[ind + offsetG] & 0xff)
                                    * gaussianWdw[kernelOffset];
                            avgB += (bytes[ind + offsetB] & 0xff)
                                    * gaussianWdw[kernelOffset];
                        }
                    }
                }
                avgR = avgR >> 4;
                avgG = avgG >> 4;
                avgB = avgB >> 4;
                int ind = newx + newy * newWd;
                resized[ind] = (byte)(avgR & 0xff);
                resized[ind+offsetGSub] = (byte)(avgG & 0xff);
                resized[ind+offsetBSub] = (byte)(avgB & 0xff);
            }
        }
    }

    public static void CopySubCollage(byte[] bytes, byte[] subcollage, int Wd, int Ht, int SubWd, int SubHt, int offsetX, int offsetY)
    {
        int offsetGSub = SubHt*SubWd;
        int offsetBSub = 2*SubHt*SubWd;
        int offsetG = Ht*Wd;
        int offsetB = 2*Ht*Wd;
        
        for(int i=0; i<SubHt; i++)
        {
            for(int j=0; j<SubWd; j++)
            {
                int indSub = j + i * SubWd;
                int ind = j + offsetX + (i + offsetY) * Wd;
                subcollage[ind] = bytes[indSub];
                subcollage[ind + offsetG] = bytes[indSub + offsetGSub];
                subcollage[ind + offsetB] = bytes[indSub + offsetBSub];
            }
        }
        
    }
    
    
    public static void GenerateSubCollage(String[] InputFileNames, int[][] CategoryFileIdx, int[] CategoryFileCounter, int Wd, int Ht, int CollageSizeX, int FileCounter, int CategoryIdx, byte[] subcollage )
    {
        // Allocate memory for image buffer
        int MaxCollageX=3, MaxCollageY=3;
        
        int SubWd, SubHt;
        
        SubWd = Wd/MaxCollageX; 
        SubHt = Ht/MaxCollageY;
        
        Arrays.fill(subcollage, (byte)BackgroundClrSubcollage);

        byte[] bytes = new byte[3*Wd*Ht];
        
        byte[] resized = new byte[3*SubWd*SubHt];
        
        for(int i=0; i<MaxCollageY; i++)
        {
            for (int j = 0; j < MaxCollageX; j++)
            {
                int CollageIdx = j + i * MaxCollageX;
                
                if(CategoryFileCounter[CategoryIdx] > CollageIdx)
                {
                    int offset = 0;
                	int startX = 0;
                	int startY = 0;

                    int fileIdx = CategoryFileIdx[CategoryIdx][CollageIdx];
                    ReadImage(InputFileNames,
                            fileIdx, bytes, offset);

                    indicateVideoUsingBytes(bytes, fileIdx, startX, startY, 64, 64);
                    
                    Resize(bytes, resized, Wd, Ht, SubWd, SubHt);

                    CopySubCollage(resized, subcollage, Wd, Ht, SubWd, SubHt, j
                            * SubWd, i * SubHt);
                }
            }
        }
    }
    
    public static void UpdateBufferedImage(BufferedImage img, int width, int height,
            byte[] bytes, int CollageXOffset, int CollageYOffset)
    {
        int ind = 0;

        for (int y = 0; y < height; y++)
        {
            for (int x = 0; x < width; x++)
            {
                byte r = bytes[ind];
                byte g = bytes[ind + height * width];
                byte b = bytes[ind + height * width * 2];

                r = (byte) (r);
                g = (byte) (g);
                b = (byte) (b);

                int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8)
                        | (b & 0xff);
                img.setRGB(x + CollageXOffset, y + CollageYOffset, pix);
                ind++;
            }
        }
    }
    
    public static void UpdateCategory(String[] InputFileNames, int[][] CategoryFileIdx, int[] CategoryFileCounter, int CollageSizeX, int FileCounter)
    {

        NumCategories = ImageClassify.imageClassify(InputFileNames, isVideo, fileCounter, CategoryFileIdx, CategoryFileCounter, Wd, Ht, MaxFiles);
        
/*        
        int CollageTotalPic = FileCounter / NumCategories; 
        for(int i=0; i< CollageTotalPic; i++)
        {
            FileCounter = FileCounter - CollageTotalPic;
            if(FileCounter >= 0 )
                CategoryFileCounter[i] = CollageTotalPic;
            else 
            {
                CategoryFileCounter[i] = FileCounter + CollageTotalPic;
                FileCounter = 0;
            }                
            
            for (int j = 0; j < CategoryFileCounter[i]; j++)
            {
                CategoryFileIdx[i][j] = j + i * CollageTotalPic;
            }
        } 
*/               
    }
    
    public static int ReadImage(String[] FileName, int FileIdx, byte[] bytes, int offset)
    {
        int numRead = -1;
        try
        {
            RandomAccessFile raf = new RandomAccessFile(FileName[FileIdx], "r");

            if(offset >= raf.length())
            {
                raf.close();
                return -1;
            }
            
            raf.seek(offset);

            numRead = raf.read(bytes, 0, bytes.length);
            numRead += offset;
            raf.close();
                        
        } catch (FileNotFoundException e)
        {
            e.printStackTrace();
        } catch (IOException e)
        {
            e.printStackTrace();
        }
        return numRead;
    }

    public static int GetCategory(int X, int Y, int CollageSizeX, int CollageSizeY, int Wd, int Ht, int CollageBarSizeWd, int CollageBarSizeHt, int[] ClickedX, int[] ClickedY)
    {
        ClickedX[0]=-1;
        ClickedY[0]=-1;
        
        for(int j=0; j<CollageSizeX; j++)
        {
            int LowX = CollageBarSizeWd*(j+1) + Wd*j;
            int HighX = LowX + Wd;
            if(X > LowX && X < HighX)
            {
                ClickedX[0] = j;
                break;
            }
        }

        for(int i=0; i<CollageSizeY; i++)
        {
            int LowY = CollageBarSizeHt*(i+1) + Ht*i;
            int HighY = LowY + Ht;
            if(Y > LowY && Y < HighY)
            {
                ClickedY[0] = i;
                break;
            }
        }

        if(ClickedX[0] != -1 && ClickedY[0] != -1)
			return (ClickedX[0] + ClickedY[0] * CollageSizeX);
        else
            return -1;
        
    }

    public static void drawPrevBar(boolean is_prev)
    {
    	int[] xPoints = new int[3];
    	int[] yPoints = new int[3];
    	int startX, startY, blkWd, blkHt;
    	
        Graphics2D    graphics = img.createGraphics();
        if(is_prev)
        	graphics.setPaint ( new Color ( 227, 71, 5 ) );
        else
        	graphics.setPaint ( new Color ( BackgroundClr, BackgroundClr, BackgroundClr ) );
        
        startX = 0; startY = 0; 
        blkWd = CollageBarSizeWd; blkHt = CollageBarSizeWd;
        for(int i=0; i< CollageHt / CollageBarSizeWd; i+=2)
        {
            xPoints[0] = startX + CollageBarSizeWd;                  yPoints[0]  =  startY + i*blkHt;
            xPoints[1] = startX;                                   yPoints[1]  =  startY + (int)((i+.5)*blkHt);
            xPoints[2] = startX + CollageBarSizeWd;                  yPoints[2]  =  startY + (i+1)*blkHt;
            
            graphics.fillPolygon(xPoints, yPoints, 3);        	
        }
        graphics.dispose();    	
    }
    public static void drawNextBar(boolean is_next)
    {
    	int[] xPoints = new int[3];
    	int[] yPoints = new int[3];
    	int startX, startY, blkWd, blkHt;

        Graphics2D    graphics = img.createGraphics();
        if(is_next)
        	graphics.setPaint ( new Color ( 227, 71, 5 ) );
        else
        	graphics.setPaint ( new Color ( BackgroundClr, BackgroundClr, BackgroundClr ) );

        startX = CollageWd - CollageBarSizeWd; startY = 0; 
        blkWd = CollageBarSizeWd; blkHt = CollageBarSizeWd;
        for(int i=0; i< CollageHt / CollageBarSizeWd; i+=2)
        {
            xPoints[0] = startX  ;                 yPoints[0]  =  startY + i*blkHt;
            xPoints[1] = startX + CollageBarSizeWd;  yPoints[1]  =  startY + (int)((i+.5)*blkHt);
            xPoints[2] = startX  ;                 yPoints[2]  =  startY + (i+1)*blkHt;
            
            graphics.fillPolygon(xPoints, yPoints, 3);        	
        }

        graphics.dispose();    	    	
    }
    public static void setMaxLevels()
    {
        int i;
    	maxPageLevel0 = (NumCategories) / (CollageSizeX * CollageSizeY );
        if(NumCategories == maxPageLevel0*CollageSizeX * CollageSizeY )
        {
            maxPageLevel0--;    
        }

    	
    	for(i=0; i<NumCategories; i++)
    	{
    		maxPageLevel1[i] = (CategoryFileCounter[i]) / (CollageSizeX * CollageSizeY );
    		// If exact multiple, then reduce maxlevel by 1
    		if(CategoryFileCounter[i] == maxPageLevel1[i]*CollageSizeX * CollageSizeY )
    		{
    		    maxPageLevel1[i]--;    
    		}
    	}
    	// Remainder of images to last category
    	maxPageLevel1[i] =  CategoryFileCounter[i] / (CollageSizeX * CollageSizeY );
        if(CategoryFileCounter[i] == maxPageLevel1[i]*CollageSizeX * CollageSizeY )
        {
            maxPageLevel1[i]--;    
        }
    }
    public static void drawBars()
    {
    	if(collageLevel == 0)
    	{
    		int idx; 
    		idx = (pageLevel0 +1 )* CollageSizeX * CollageSizeY ;
    		if(CategoryFileCounter[idx] > 0)
    			drawNextBar(true);
    		else
    			drawNextBar(false);
    		idx = (pageLevel0 -1 )* CollageSizeX * CollageSizeY ;
    		if(idx >= 0 && CategoryFileCounter[idx] > 0)
    			drawPrevBar(true);
    		else
    			drawPrevBar(false);
    	}
    	else
    	{
    		int idx; 
    		idx = (pageLevel1 +1 )* CollageSizeX * CollageSizeY ;
    		if(CategoryFileCounter[currentCategory] > idx)
    			drawNextBar(true);
    		else
    			drawNextBar(false);
    		idx = (pageLevel1 -1 )* CollageSizeX * CollageSizeY ;
    		if(idx >= 0)
    			drawPrevBar(true);
    		else
    			drawPrevBar(false);
    	}
    }

    public static void indicateVideoUsingGraphics(int fileIdx, int startX, int startY, int blkWd, int blkHt)
    {
        if(isVideo[fileIdx] == 1)
        {
        	int[] xPoints = new int[3];
        	int[] yPoints = new int[3];
	        Graphics2D    graphics = img.createGraphics();
	        graphics.setPaint ( new Color ( 255, 0, 0 ) );
//	        graphics.fillRect ( startX + Wd - blkWd, startY + Ht - blkHt, blkWd, blkHt );
	        xPoints[0] = startX + Wd - blkWd; yPoints[0]  =  startY + Ht - blkHt;
	        xPoints[1] = startX + Wd - blkWd; yPoints[1]  =  startY + Ht;
	        xPoints[2] = startX + Wd;         yPoints[2]  =  startY + Ht - blkHt/2;
	        
	        graphics.fillPolygon(xPoints, yPoints, 3);
	        graphics.dispose();
        }
    }
    public static void indicateVideoUsingBytes(byte[] bytes, int fileIdx, int startX, int startY, int blkWd, int blkHt)
    {
    	int x = startX + Wd - blkWd;
    	int y = startY + Ht - blkHt;
        int offsetG = Ht*Wd;
        int offsetB = 2*Ht*Wd;

        if(isVideo[fileIdx] == 1)
        {
	        for(int i=0; i<blkHt; i++)
	        {
	            for(int j=0; j<blkWd; j++)
	            {
	            	if((j <= 2*i && i <= blkHt/2) || (i > blkHt/2 && (i - blkHt/2 < (-(double)blkHt*j/(2*blkWd) + blkHt/2) )))
	            	{
		                int ind = x + j + (y+i) * Wd;
		                bytes[ind] = (byte)0xff;
		                bytes[ind + offsetG] = 0;
		                bytes[ind + offsetB] = 0;
	            	}
	            }
	        }
        }
    }
    
    public static void updateCollageLevel0(int CollageX, int CollageY)
    {
        int CollageIdx = CollageX+CollageY*CollageSizeX + pageLevel0 * CollageSizeX * CollageSizeY ;

        GenerateSubCollage(InputFileNames, CategoryFileIdx, CategoryFileCounter, Wd, Ht, CollageSizeX, fileCounter, CollageIdx, bytes);
        UpdateBufferedImage(img, Wd, Ht, bytes, CollageX * Wd + (CollageX+1)*CollageBarSizeWd, CollageY * Ht + (CollageY+1)*CollageBarSizeHt);	
    }
    
    public static void updateCollageLevel1(int CollageX, int CollageY)
    {
        int offset = 0;
        int CollageIdx = CollageX+CollageY*CollageSizeX + pageLevel1 * CollageSizeX * CollageSizeY;
        
        int CategoryIdx = currentCategory;
        
        if(CategoryFileCounter[CategoryIdx] > CollageIdx )
        {
        	int fileIdx = CategoryFileIdx[CategoryIdx][CollageIdx];
        	int startX = CollageX * Wd + (CollageX+1)*CollageBarSizeWd;
        	int startY = CollageY * Ht + (CollageY+1)*CollageBarSizeHt;

        	if(collageLevel == 1)
        	{
        		offset = ReadImage(InputFileNames, fileIdx, bytes, offset);
                UpdateBufferedImage(img, Wd, Ht, bytes, startX, startY);
        	}
            indicateVideoUsingGraphics(fileIdx, startX, startY, 64, 64);
        }
        else
        {
            Arrays.fill(bytes,(byte)BackgroundClrSubcollage);
            UpdateBufferedImage(img, Wd, Ht, bytes, CollageX * Wd + (CollageX+1)*CollageBarSizeWd, CollageY * Ht + (CollageY+1)*CollageBarSizeHt);
        }
    }
    
    public static void DisplayCollage(String[] InputFileNames, int[] isVideo, int FileCounter, int[][] CategoryFileIdx, int[] CategoryFileCounter, int Wd, int Ht, int CollageSizeX, int CollageSizeY, int CollageBarSizeWd, int CollageBarSizeHt, BufferedImage img, JFrame frame, int MouseClickX, int MouseClickY)
    {
    	drawBars();
    	
        if(collageLevel == 0 || collageLevel == 1 || (collageLevel == 2 && playVideo==0))
        {
            for(int CollageY=0; CollageY <CollageSizeY; CollageY++)
            {
                for(int CollageX=0; CollageX <CollageSizeX; CollageX++)
                {                    
                    if(collageLevel == 0)  // Display First level of categorized collage
                    {
                    	updateCollageLevel0(CollageX, CollageY);
                    }
                    else // Display Images in collage
                    {
                    	updateCollageLevel1(CollageX, CollageY);
                    }
                }                
            }

        }
        else if(collageLevel == 2 && currentCategory != -1 && playVideo == 1) // Display Video if mouse is clicked on video file
        {
        	int pageFileIdx = ClickedCollagePosition + pageLevel1 * CollageSizeX * CollageSizeY ;
            int FileIdx = CategoryFileIdx[currentCategory][pageFileIdx];
            
            for(int CollageY=0; CollageY <CollageSizeY; CollageY++)
            {
                for(int CollageX=0; CollageX <CollageSizeX; CollageX++)
                {                    
                	updateCollageLevel1(CollageX, CollageY);
                }
            }
            
            if(CategoryFileCounter[currentCategory] >= ClickedCollagePosition && isVideo[FileIdx] == 1)
            {
				offset = ReadImage( InputFileNames, FileIdx, bytes, offset);

				if (offset < 0) 
				{
					offset = 0;
					frameCnt = 0;
					playVideo = 0;
					System.out.printf("Video File idx = %d",FileIdx);
					return;
				}
				
				int clickedY = (ClickedCollagePosition/CollageSizeX);
				int clickedX = (ClickedCollagePosition - clickedY * CollageSizeX );
				UpdateBufferedImage(img, Wd, Ht, bytes, clickedX * Wd
						+ (clickedX + 1) * CollageBarSizeWd, clickedY * Ht
						+ (clickedY + 1) * CollageBarSizeHt);

				System.out.printf("Frame No - %d \n", frameCnt++);
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
		DisplayCollage(InputFileNames, isVideo, fileCounter, CategoryFileIdx, CategoryFileCounter, Wd, Ht, CollageSizeX, CollageSizeY, CollageBarSizeWd, CollageBarSizeHt, img, frame, mouseClickX, mouseClickY);
    }

    Action  paintTimer  = new AbstractAction() {
        private static final long   serialVersionUID    = -2121714427110679013L;

        boolean paintButton;
        @Override
        public void actionPerformed(ActionEvent e) {
            paintButton = false;
            if(mouseClicked ==1 || playVideo == 1)
            {
                repaint();
                mouseClicked = 0;
            }
        }
    };

}