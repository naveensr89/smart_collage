

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
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.LinkedList;

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

import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfFloat4;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.highgui.Highgui;
import org.opencv.features2d.DMatch;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.Features2d;
import org.opencv.features2d.KeyPoint;

public class ImageClassify {
    public static void quicksort(int[] main, int[] index, int length) {
        quicksort(main, index, 0, length - 1);
    }

    // quicksort a[left] to a[right]
    public static void quicksort(int[] a, int[] index, int left, int right) {
        if (right <= left) return;
        int i = partition(a, index, left, right);
        quicksort(a, index, left, i-1);
        quicksort(a, index, i+1, right);
    }

    // partition a[left] to a[right], assumes left < right
    private static int partition(int[] a, int[] index, 
    int left, int right) {
        int i = left - 1;
        int j = right;
        while (true) {
            while (less(a[++i], a[right]))      // find item on left to swap
                ;                               // a[right] acts as sentinel
            while (less(a[right], a[--j]))      // find item on right to swap
                if (j == left) break;           // don't go out-of-bounds
            if (i >= j) break;                  // check if pointers cross
            exch(a, index, i, j);               // swap two elements into place
        }
        exch(a, index, i, right);               // swap with partition element
        return i;
    }

    // is x < y ?
    private static boolean less(int x, int y) {
        return (x > y);
    }

    // exchange a[i] and a[j]
    private static void exch(int[] a, int[] index, int i, int j) {
        int swap = a[i];
        a[i] = a[j];
        a[j] = swap;
        int b = index[i];
        index[i] = index[j];
        index[j] = b;
    }
    
    
    public static int isFace(Mat img, int pic)
    {
        
        MatOfRect faceDetections = new MatOfRect();
        //CascadeClassifier faceDetector = new CascadeClassifier(testopen.class.getResource("haarcascade_frontalface_alt.xml").getPath());
        CascadeClassifier faceDetector = new CascadeClassifier("haarcascade_frontalface_alt.xml");
        
        faceDetector.detectMultiScale(img, faceDetections);
 
        System.out.println(String.format("Detected %s faces", faceDetections.toArray().length));
    
        return faceDetections.toArray().length;
    }//end of isFace
    
    public static int isSunrise(Mat img, int pic)
    {
        
        int wd = 352, ht = 288;
        byte[] bytes_hsv = new byte[(int) 3 * wd * ht];
        
        Mat hsv = new Mat(ht, wd, CvType.CV_8UC3);
        Imgproc.cvtColor(img, hsv, Imgproc.COLOR_BGR2HLS); //Imgproc.COLOR_BGR2HSV);
        //Imgproc.COLOR_BGR2HLS
        hsv.get(0, 0, bytes_hsv);
        
        long res_h =0, res_s =0, res_l =0;
        long normalisedSum = 0;
        for (int row = 0; row < ht/3; row++) //top 1/3rd Image
        {
            for (int col = 0; col < wd; col++) 
            {
                int ind = col + row * wd;
                int off = wd * ht;
                res_h = (int)(bytes_hsv[ind * 3] & 0xFF);
                res_s = (int)(bytes_hsv[ind * 3 + 2] & 0xFF);
                res_l = (int)(bytes_hsv[ind * 3 + 1] & 0xFF);
                //System.out.println(" res_b="+res_b+" res_r="+res_r+" res_g="+res_g);
                
                if(res_h>=0 && res_h<=30 && res_l>32 && res_l<223 &&  res_s>32 )
                    //  http://www.erinsowards.com/articles/2011/01/colors.php
                {
                    normalisedSum++;
                    //System.out.println(String.format("normalisedSum= %s", normalisedSum));
                }
            }
        }
        //System.out.println(String.format("sunrise= %s", normalisedSum));
        if(normalisedSum>20000)
        {
            System.out.println(String.format("sunrise= %s", normalisedSum));
            return 1;
        }
        else
            return 0;
    }//end of isSunrise
    
    
    public static int isSky(Mat img, int pic)
    {
        
        int wd = 352, ht = 288;
        byte[] bytes_hsv = new byte[(int) 3 * wd * ht];
        
        Mat hsv = new Mat(ht, wd, CvType.CV_8UC3);
        Imgproc.cvtColor(img, hsv, Imgproc.COLOR_BGR2HLS); //Imgproc.COLOR_BGR2HSV);
        //Imgproc.COLOR_BGR2HLS
        hsv.get(0, 0, bytes_hsv);
        
        long res_h =0, res_s =0, res_l =0;
        long normalisedSum = 0;
        for (int row = 0; row < ht/3; row++) //top 1/3rd Image
        {
            for (int col = 0; col < wd; col++) 
            {
                int ind = col + row * wd;
                int off = wd * ht;
                res_h = (int)(bytes_hsv[ind * 3] & 0xFF);
                res_s = (int)(bytes_hsv[ind * 3 + 2] & 0xFF);
                res_l = (int)(bytes_hsv[ind * 3 + 1] & 0xFF);
                //System.out.println(" res_b="+res_b+" res_r="+res_r+" res_g="+res_g);
                
                //if(res_h>86 && res_h<135 && res_v>51/2) //171 degree to 270 degree
                //if(res_h>86 && res_h<135 && res_v>32)//=32) //(res_v*res_s)>1024) // && (res_v+res_s)<=255
                //if(res_h>86 && res_h<135 && (res_l+res_s)>255 && res_l>32 && res_l<223)//working
                if(res_h>86 && res_h<135 && res_l>32 && res_l<223 &&  res_s>32 )
                    //  http://dba.med.sc.edu/price/irf/Adobe_tg/models/hsb.html
                {
                    normalisedSum++;
                    //System.out.println(String.format("normalisedSum= %s", normalisedSum));
                }
            }
        }
        if(normalisedSum>20000)
        {
            System.out.println(String.format("sunrise= %s", normalisedSum));
            return 1;
        }
        else
            return 0;
    }//end of isSky
    
    public static int isCartoonHsv(Mat img, int pic)
    {
        
        int wd = 352, ht = 288;
        byte[] bytes_hsv = new byte[(int) 3 * wd * ht];
        
        Mat hsv = new Mat(ht, wd, CvType.CV_8UC3);
        Imgproc.cvtColor(img, hsv, Imgproc.COLOR_BGR2HLS); //Imgproc.COLOR_BGR2HSV);
        //Imgproc.COLOR_BGR2HLS
        hsv.get(0, 0, bytes_hsv);
        
        long res_h =0, res_s =0, res_l =0;
        long normalisedSum = 0;
        for (int row = 0; row < ht; row++)
        {
            for (int col = 0; col < wd; col++) 
            {
                int ind = col + row * wd;
                int off = wd * ht;
                res_h = (int)(bytes_hsv[ind * 3] & 0xFF);
                res_s = (int)(bytes_hsv[ind * 3 + 2] & 0xFF);
                res_l = (int)(bytes_hsv[ind * 3 + 1] & 0xFF);
                //System.out.println(" res_b="+res_b+" res_r="+res_r+" res_g="+res_g);
                
                //if(res_h>86 && res_h<135 && res_v>51/2) //171 degree to 270 degree
                //if(res_h>86 && res_h<135 && res_v>32)//=32) //(res_v*res_s)>1024) // && (res_v+res_s)<=255
                //if(res_h>86 && res_h<135 && (res_l+res_s)>255 && res_l>32 && res_l<223)//working
                //if(res_h>86 && res_h<135 && res_l>32 && res_l<223 &&  res_s>32 )
                    //  http://dba.med.sc.edu/price/irf/Adobe_tg/models/hsb.html
                {
                    normalisedSum += res_s;
                    //System.out.println(String.format("normalisedSum= %s", normalisedSum));
                }
            }
        }
        if(normalisedSum>14000000)
        {
            System.out.println(String.format("isCartoonHsv= %s", normalisedSum));
            return 1;
        }
        else
            return 0;
    }//end of isCartoonHsv
    
    public static int isNature(Mat img, int pic)
    {
        
        int wd = 352, ht = 288;
        byte[] bytes_hsv = new byte[(int) 3 * wd * ht];
        
        Mat hsv = new Mat(ht, wd, CvType.CV_8UC3);
        Imgproc.cvtColor(img, hsv, Imgproc.COLOR_BGR2HLS); //Imgproc.COLOR_BGR2HSV);
        //Imgproc.COLOR_BGR2HLS
        hsv.get(0, 0, bytes_hsv);
        
        long res_h =0, res_s =0, res_l =0;
        long normalisedSum = 0;
        for (int row = 0; row < ht; row++) //full Image
        {
            for (int col = 0; col < wd; col++) 
            {
                int ind = col + row * wd;
                int off = wd * ht;
                res_h = (int)(bytes_hsv[ind * 3] & 0xFF);
                res_s = (int)(bytes_hsv[ind * 3 + 2] & 0xFF);
                res_l = (int)(bytes_hsv[ind * 3 + 1] & 0xFF);
                //System.out.println(" res_b="+res_b+" res_r="+res_r+" res_g="+res_g);
                
                //if(res_h>=32 && res_h<=78 && res_l>=31 && res_l<=226 &&  res_s>=25 )
                if(res_h>=32 && res_h<=78 && res_l>32 && res_l<223 &&  res_s>32 )
                    //  http://www.erinsowards.com/articles/2011/01/colors.php
                {
                    normalisedSum++;
                    //System.out.println(String.format("normalisedSum= %s", normalisedSum));
                }
            }
        }
        System.out.println(String.format("nature= %s", normalisedSum));
        if(normalisedSum>20000)
        {
            //System.out.println(String.format("sunrise= %s", normalisedSum));
            return 1;
        }
        else
            return 0;
    }//end of isNature
    
    public static int isDark(Mat img, int pic)
    {
        
        int wd = 352, ht = 288;
        byte[] bytes_hsv = new byte[(int) 3 * wd * ht];
        
        Mat hsv = new Mat(ht, wd, CvType.CV_8UC3);
        Imgproc.cvtColor(img, hsv, Imgproc.COLOR_BGR2HLS); //Imgproc.COLOR_BGR2HSV);
        //Imgproc.COLOR_BGR2HLS
        hsv.get(0, 0, bytes_hsv);
        
        long res_h =0, res_s =0, res_l =0;
        long normalisedSum = 0;
        for (int row = 0; row < ht; row++) //full Image
        {
            for (int col = 0; col < wd; col++) 
            {
                int ind = col + row * wd;
                int off = wd * ht;
                res_h = (int)(bytes_hsv[ind * 3] & 0xFF);
                res_s = (int)(bytes_hsv[ind * 3 + 2] & 0xFF);
                res_l = (int)(bytes_hsv[ind * 3 + 1] & 0xFF);
                //System.out.println(" res_b="+res_b+" res_r="+res_r+" res_g="+res_g);
                
                //if(res_h>=32 && res_h<=78 && res_l>=31 && res_l<=226 &&  res_s>=25 )
                if(res_l<=34)
                    //  http://www.erinsowards.com/articles/2011/01/colors.php
                {
                    normalisedSum++;
                    //System.out.println(String.format("normalisedSum= %s", normalisedSum));
                }
            }
        }
        System.out.println(String.format("isDark= %s", normalisedSum));
         
        if(normalisedSum>20000)
        {
            //System.out.println(String.format("isDark= %s", normalisedSum));
            return 1;
        }
        else
            return 0;
    }//end of isDark
    
	public static int SIFTmatch(Mat[] imageDescriptors,MatOfKeyPoint[] imageKeypoints, byte[] bytes1, byte[] bytes2, int index1, int index2, int Wd, int Ht)
	{
		//-- Step 3: Matching descriptor vectors using FLANN matcher
	    DescriptorMatcher matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE);
        MatOfDMatch matches1 = new MatOfDMatch();
        MatOfDMatch matches2 = new MatOfDMatch();
        LinkedList<DMatch> filteredMatches12 = new LinkedList<DMatch>() ;
        

		  matcher.match( imageDescriptors[index1], imageDescriptors[index2], matches1 );
          matcher.match( imageDescriptors[index2], imageDescriptors[index1], matches2 );

		  double max_dist = 0; double min_dist = 100;

		  //-- Quick calculation of max and min distances between keypoints
          DMatch[] matchesList1 = matches1.toArray();
          DMatch[] matchesList2 = matches2.toArray();
        
          for( int i = 0; i < matchesList1.length; i++ )
          { 
              DMatch forward = matchesList1[i]; 
              DMatch backward = matchesList2[forward.trainIdx]; 
              if( backward.trainIdx == forward.queryIdx ) 
                  filteredMatches12.addLast( forward ); 
          }
          
		  for( int i = 0; i < matchesList1.length; i++ )
		  {
			  double dist = matchesList1[i].distance;
		    if( dist < min_dist ) min_dist = dist;
		    if( dist > max_dist ) max_dist = dist;
		  }

          LinkedList<DMatch> goodmatches1 = new LinkedList<DMatch>() ;
          LinkedList<DMatch> goodmatches2 = new LinkedList<DMatch>() ;
          
          for( int i = 0; i < filteredMatches12.size(); i++ )
           { 
              if( filteredMatches12.get(i).distance <= Math.max(2*min_dist, 0.02) )
             {
                  goodmatches1.addLast( matchesList1[i]);
                  goodmatches2.addLast( matchesList2[i]);
             }
           }
          
          return goodmatches1.size();

	}
	
	public static void SIFTcompute(byte[] bytes, Mat[] imageDescriptors,MatOfKeyPoint[] imageKeypoints, int index, int Wd, int Ht)
	{
		Mat image01 = new Mat(Ht, Wd, CvType.CV_8UC3);
        
        image01.put(0, 0, bytes);
        
		Mat grayImage01 = new Mat(image01.rows(), image01.cols(), image01.type());
		Imgproc.cvtColor(image01, grayImage01, Imgproc.COLOR_BGRA2GRAY);
		Core.normalize(grayImage01, grayImage01, 0, 255, Core.NORM_MINMAX);

		FeatureDetector siftDetector = FeatureDetector.create(FeatureDetector.STAR);
		DescriptorExtractor siftExtractor = DescriptorExtractor.create(DescriptorExtractor.SIFT);
		
		siftDetector.detect(grayImage01, imageKeypoints[index]);

		siftExtractor.compute(grayImage01, imageKeypoints[index], imageDescriptors[index]);
	}
	
    public static void rgbPlanarToInterleaved(byte[] bytes,int Wd,int Ht)
    {
    	int offsetG = Wd*Ht;
    	int offsetB = 2*Wd*Ht;
    	
    	byte[] temp = Arrays.copyOf(bytes, bytes.length);
    	
    	for(int i=0; i<Ht; i++)
    	{
    		for(int j=0; j<Wd; j++)
    		{
    			int ind = j + i * Wd;
    			byte r = temp[ind];
    			byte g = temp[ind+offsetG];
    			byte b = temp[ind+offsetB];
    			
    			bytes[ind*3] = b;
    			bytes[ind*3+1] = g;
    			bytes[ind*3+2] = r;    			
    		}
    	}
    }

    public static int ReadImage(String[] FileName, int FileIdx, byte[] bytes, int Wd, int Ht, int offset)
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
            
            rgbPlanarToInterleaved(bytes, Wd, Ht);
                        
        } catch (FileNotFoundException e)
        {
            e.printStackTrace();
        } catch (IOException e)
        {
            e.printStackTrace();
        }
        return numRead;
    }

    public static int imageClassify(String[] InputFileNames, int[] isVideo, int fileCounter, int[][] CategoryFileIdx, int[] CategoryFileCounter, int Wd, int Ht, int MaxFiles) 
    {
        int[][] CategoryFileIdxTemp = new int[MaxFiles][MaxFiles];
        int[] CategoryFileCounterTemp = new int[MaxFiles];
        Mat[] imageDescriptors = new Mat[MaxFiles];
        MatOfKeyPoint[] imageKeypoints = new MatOfKeyPoint[MaxFiles];
        byte[] bytes = new byte[Wd*Ht*3];
        byte[] bytes1 = new byte[Wd*Ht*3];
        byte[] bytes2 = new byte[Wd*Ht*3];
        
        // Computer SIFT descriptor for all the images 
        for(int i=0; i<fileCounter; i++)
        {
            imageDescriptors[i] = new Mat(Ht, Wd, CvType.CV_8UC3);
            imageKeypoints[i] = new MatOfKeyPoint();

            ReadImage(InputFileNames, i, bytes, Wd, Ht, 0);
            SIFTcompute(bytes, imageDescriptors, imageKeypoints, i, Wd, Ht);
        }

        //SIFT matching of one image with all the other images
        int category = 0, category_flag=0;
        int[] calssificationMap = new int[MaxFiles];
        
        for(int i=0; i<fileCounter; i++)
        {
            category_flag=0;
            if(calssificationMap[i] != 1)
            {
                int count=0;
                for(int j=i+1; j<fileCounter; j++)
                {
                    if(calssificationMap[j] != 1)
                    {
                        ReadImage(InputFileNames, i, bytes1, Wd, Ht, 0);
                        ReadImage(InputFileNames, j, bytes2, Wd, Ht,0);
                        if(!imageDescriptors[i].empty() && !imageDescriptors[j].empty())
                        {
                            int val = SIFTmatch(imageDescriptors, imageKeypoints, bytes1, bytes2, i, j, Wd, Ht);
                            
                            if(val > 2)
                            {
                                category_flag=1;
                                CategoryFileIdxTemp[category][count++] = j;
                                CategoryFileCounterTemp[category] = count;
                                calssificationMap[j] = 1;
                            }
                        }
                    }
                }
            }
            if(category_flag == 1)
            {
                int count =  CategoryFileCounterTemp[category];

                CategoryFileIdxTemp[category][count++] = i;
                CategoryFileCounterTemp[category] = count;

                calssificationMap[i] = 1;
                category++;
            }
        }
        
        // Add sky, dark, nature detection code below
        int count=0;
        Mat img = new Mat(Ht, Wd, CvType.CV_8UC3);
        for(int i=0; i<fileCounter; i++)
        {
            if(calssificationMap[i] == 0)
            {
                int category_idx;
                ReadImage(InputFileNames, i, bytes, Wd, Ht, 0);
                
                img.put(0, 0, bytes);

                if(isFace(img, i) >= 1)
                {
                    category_idx = 0;
                }
                else if(isCartoonHsv(img, i) == 1)
                {
                    category_idx = 1;
                }
                else if(isSky(img, i) == 1)
                {
                    category_idx = 2;
                }
                else if(isSunrise(img, i) == 1)
                {
                    category_idx = 3;
                }
                else  if(isDark(img, i) == 1)
                {
                    category_idx = 4;
                }
                else
                {
                    category_idx = 5;
                }
                count = CategoryFileCounter[category_idx];
                CategoryFileIdx[category_idx][count++] = i;                    
                CategoryFileCounter[category_idx] = count;
                calssificationMap[i] = 1;
            }
        }

        System.out.printf("\nFace: ");
        printCategoryFileIdx(CategoryFileCounter, CategoryFileIdx, 0);
        System.out.printf("Cartoon: ");
        printCategoryFileIdx(CategoryFileCounter, CategoryFileIdx, 1);
        System.out.printf("Sky: ");
        printCategoryFileIdx(CategoryFileCounter, CategoryFileIdx, 2);
        System.out.printf("Sunrise: ");
        printCategoryFileIdx(CategoryFileCounter, CategoryFileIdx, 3);
        System.out.printf("Dark: ");
        printCategoryFileIdx(CategoryFileCounter, CategoryFileIdx, 4);
        System.out.printf("Uncategorized: ");
        printCategoryFileIdx(CategoryFileCounter, CategoryFileIdx, 5);

        int fixedCategories = 6;
        // Sorting to get category with most number of images as first
        int[] categoryIndex = new int[category];
        for(int i=0; i<category; i++)
        {
            categoryIndex[i] = i;
        }

        quicksort(CategoryFileCounterTemp, categoryIndex, category);
        
        for(int i=0; i<category; i++)
        {
            int oldIdx = categoryIndex[i];
            int newCategory = fixedCategories + i;
            
            for(int j=0; j<CategoryFileCounterTemp[i]; j++)
            {
                CategoryFileIdx[newCategory][j] = CategoryFileIdxTemp[oldIdx][j];
            }
            CategoryFileCounter[newCategory] = CategoryFileCounterTemp[i];
            
            System.out.printf("Category");
            printCategoryFileIdx(CategoryFileCounter, CategoryFileIdx, newCategory);
        }        
        
        return category;
        
    }
    
    public static void printCategoryFileIdx(int[] CategoryFileCounter, int[][] CategoryFileIdxTemp, int category)
    {
        System.out.printf("%d: ",category);
        for(int i=0; i<CategoryFileCounter[category]; i++)
        {
             System.out.printf("%d ",CategoryFileIdxTemp[category][i]);
        }
        System.out.printf("\n");
    }
/*	
	public static void main(String[] args)
	{
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        int Wd=352, Ht=288;
		String Folder = "C:\\Users\\naveensr\\CSCI_Project_data\\CS576_Project_Dataset_all";
//        String Folder = "C:\\Users\\naveensr\\CSCI_Project_data\\temp";
		int fileCounter=0, MaxFiles = 1000;
		String[] InputFileNames = new String[MaxFiles];
		int[] isVideo = new int[MaxFiles];
	    int[][] CategoryFileIdx = new int[MaxFiles][MaxFiles];
	    int[] CategoryFileCounter = new int[MaxFiles];
	    byte[] bytes = new byte[Wd*Ht*3];
	    
		final File folder = new File(Folder);
        
        for (final File fileEntry : folder.listFiles())
        {
            if(fileCounter == MaxFiles)
            {
                System.out.printf("\n Handling only first %d images",MaxFiles);
                break;
            }
            if (fileEntry.isFile())
            {
                InputFileNames[fileCounter] = Folder + "\\" + fileEntry.getName();
                isVideo[fileCounter] = (fileEntry.length() > (Wd*Ht*3) ? 1 : 0);
                
//                ReadImage(InputFileNames, fileCounter, bytes, Wd, Ht, 0);
//        		Mat image01 = new Mat(Ht, Wd, CvType.CV_8UC3);
//                image01.put(0, 0, bytes);                
//                Highgui.imwrite(Folder+"\\"+ fileEntry.getName() + ".png", image01);
                
                fileCounter++;

            }
        }

		imageClassify(InputFileNames, isVideo, fileCounter, CategoryFileIdx, CategoryFileCounter, Wd, Ht, MaxFiles);
	}*/
}
