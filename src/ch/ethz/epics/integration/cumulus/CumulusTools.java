package ch.ethz.epics.integration.cumulus;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.color.ICC_ColorSpace;
import java.awt.color.ICC_Profile;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.renderable.RenderContext;
import java.awt.image.renderable.RenderableImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.stream.ImageOutputStream;
import javax.media.jai.ImageLayout;
import javax.media.jai.InterpolationBicubic;
import javax.media.jai.InterpolationBicubic2;
import javax.media.jai.JAI;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.RenderableOp;
import javax.media.jai.RenderedImageAdapter;
import javax.media.jai.RenderedOp;
import javax.media.jai.operator.CompositeDescriptor;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.Text;
import org.jdom.output.XMLOutputter;

import ch.ethz.epics.export.XPort;


import com.canto.cumulus.AssetReference;
import com.canto.cumulus.CatalogCollection;
import com.canto.cumulus.Categories;
import com.canto.cumulus.Category;
import com.canto.cumulus.Cumulus;
import com.canto.cumulus.CumulusException;
import com.canto.cumulus.CumulusSession;
import com.canto.cumulus.Field;
import com.canto.cumulus.FieldDefinition;
import com.canto.cumulus.FieldDefinitions;
import com.canto.cumulus.FieldTypes;
import com.canto.cumulus.GUID;
import com.canto.cumulus.MultiCatalogCollection;
import com.canto.cumulus.Pixmap;
import com.canto.cumulus.Record;
import com.canto.cumulus.Server;
import com.canto.cumulus.ServerCatalogs;
import com.canto.cumulus.StringEnum;
import com.canto.cumulus.StringEnumList;
import com.canto.cumulus.prefs.RecordField;
import com.canto.cumulus.prefs.RecordFieldList;
import com.canto.cumulus.prefs.RecordView;
import com.canto.cumulus.prefs.RecordViewSet;
import com.canto.cumulus.prefs.RecordViewSetList;
import com.canto.cumulus.prefs.ServerRoot;
import com.canto.cumulus.prefs.User;
import com.canto.cumulus.utils.DateOnly;
import com.sun.media.jai.codec.ByteArraySeekableStream;
import com.sun.media.jai.codec.SeekableStream;
import com.sun.media.jai.codecimpl.util.RasterFactory;

public class CumulusTools {
	private static Log log = LogFactory.getLog(CumulusTools.class);
	private final static String NS_EPICS = "http://www.e-pics.ethz.ch/ns/integration";
	private static Namespace nsEpics = Namespace.getNamespace("epics", NS_EPICS);
	private static final boolean  connectionReadWrite = false;
	
    private final static GUID GUID_REC_MODIFICATION_DATE = new GUID("{AF4B2E02-5F6A-11D2-8F20-0000C0E166DC}");
    private final static GUID GUID_REC_TITLE = new GUID("{AF4B2E3D-5F6A-11D2-8F20-0000C0E166DC}");
    private final static GUID GUID_REC_AUTHOR = new GUID("{AF4B2E39-5F6A-11D2-8F20-0000C0E166DC}");
    private final static GUID GUID_REC_DESCRIPTION = new GUID("{AF4B2E34-5F6A-11D2-8F20-0000C0E166DC}");
    private final static GUID GUID_EXIF_DATE = new GUID("{E174B821-623C-476B-A2BC-173725585E0A}");
    
    public synchronized void initCumulus() {       
        log.debug("Cumulus.CumulusStart()");
        
        Cumulus.CumulusStart();
        //Cumulus.setMaxTools(20);
     }

    public void closeCumulus(MultiCatalogCollection mc) {
    	if (mc != null) 
    		mc.close();
    		mc = null;
    }
    
     public void stopCumulus() {    	 
         log.debug("Cumulus.CumulusStop()");
         System.gc();
         Cumulus.CumulusStop();
         log.debug("Cumulus.CumulusStop() finished");
         
         // garbage collect possible native references
         System.gc();
         log.debug("System.gc() [1] finished");
         // garbage collect native DLL
         System.gc();
         log.debug("System.gc() [2] finished");
     }   

     /**
      * Connects to the Cumulus Server and opens the list of catalogs in 
      * a MultiCatalogCollection which is returned.
      * 
      * @param server
      * @param user
      * @param password
      * @param catalogs
      * @return
      * @throws CumulusException
      */
     public synchronized MultiCatalogCollection connect(String server, String user, String password, Vector catalogs) throws CumulusException {
    	    
         log.info("Connecting to Cumulus Server "+server+" as user "+user);
         Server cumulusServer = Server.connectToServer( connectionReadWrite, 
        		 server, 
        		 user,
                 password
             );
         
         log.debug("Retrieving list of server catalogs");        
         ServerCatalogs cumulusCatalogs = cumulusServer.getServerCatalogs();
         
         log.debug("Creating and adding new empty MultiCatalogCollection");         
         MultiCatalogCollection catCollection = cumulusServer.newMultiCatalogCollection();         
         
         Iterator it = catalogs.iterator();
         while (it.hasNext()) {
        	 String catName = (String)it.next();
        	 log.info("Opening catalog ["+catName+"]");                
        	 CatalogCollection c = cumulusCatalogs.getServerCatalog(catName).open();
        	 catCollection.addCatalogCollection(c);
         }

         return catCollection;
     }

     protected RenderingHints getRenderingHints() {
    	 RenderingHints renderingHints = new RenderingHints(
      			RenderingHints.KEY_RENDERING, 
 	     		RenderingHints.VALUE_RENDER_QUALITY);
      	renderingHints.put(RenderingHints.KEY_ANTIALIASING, 
      			RenderingHints.VALUE_ANTIALIAS_ON);
      	renderingHints.put(RenderingHints.KEY_DITHERING,
      			RenderingHints.VALUE_DITHER_DISABLE);
      	renderingHints.put(RenderingHints.KEY_STROKE_CONTROL,
      			RenderingHints.VALUE_STROKE_PURE);
      	renderingHints.put(RenderingHints.KEY_FRACTIONALMETRICS,
      			RenderingHints.VALUE_FRACTIONALMETRICS_ON);
      	
      	renderingHints.put(RenderingHints.KEY_INTERPOLATION,
      			RenderingHints.VALUE_INTERPOLATION_BICUBIC);
      	
      	renderingHints.put(RenderingHints.KEY_ALPHA_INTERPOLATION,
      			RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
      	renderingHints.put(RenderingHints.KEY_COLOR_RENDERING,
      			RenderingHints.VALUE_COLOR_RENDER_QUALITY);
      	renderingHints.put(RenderingHints.KEY_COLOR_RENDERING,
      			RenderingHints.VALUE_COLOR_RENDER_QUALITY);
      	      	
      	
      	return renderingHints;
     }
     
     public RenderedImage scaleDown(RenderableOp img) {
    	 return null;
     }
     
     public RenderedImage getOverlay(RenderedImage img, PlanarImage overlayImg, String overlayPos) {
     	//ListRegistry();
     	if (overlayPos == null) {
     		overlayPos = "full";
     	}
     	
     	// global options
     	RenderingHints renderingHints = getRenderingHints();
     	
     	
     	// code adapted (migrated from Renderable to Rendered) from 
     	// http://swjscmail1.java.sun.com/cgi-bin/wa?A2=ind0110&L=jai-interest&P=R14667
     	
     	// prepare overlay image
     	//
     	ParameterBlockJAI pb = new ParameterBlockJAI("renderable");
     	pb.addSource(overlayImg);
     	pb.setParameter("height", (float) overlayImg.getHeight());     	
     	RenderableOp renOverlay = JAI.createRenderable("renderable", pb, renderingHints);

     	// get RGB bands of overlay image
     	pb = new ParameterBlockJAI("bandSelect", "renderable");
     	pb.addSource(renOverlay);
     	pb.setParameter("bandIndices", new int[] {0, 1, 2});
     	RenderableOp renOverlayImage = JAI.createRenderable("bandSelect", pb, renderingHints);

     	// get alpha band of overlay image
     	pb = new ParameterBlockJAI("bandSelect", "renderable");
     	pb.addSource(renOverlay);
     	pb.setParameter("bandIndices", new int[] {3});
     	RenderableOp renOverlayAlpha = JAI.createRenderable("bandSelect", pb, renderingHints);
     	
     	// prepare main image
     	//
     	 /*
        RenderingHints qualityHints = new RenderingHints(RenderingHints.KEY_RENDERING,
        	    RenderingHints.VALUE_RENDER_QUALITY);
       
        ParameterBlock pab = new ParameterBlock();
        pab.addSource(img);
        pab.add(0.5d);
        pab.add(0.5d);
        pab.add(new InterpolationBicubic2(16));
        RenderableOp renImage = JAI.createRenderable("SubsampleAverage", pab, qualityHints);
     	*/
     	
        
     	pb = new ParameterBlockJAI("renderable");
     	pb.addSource(img);
     	//pb.setParameter("height", (float) img.getHeight()/2);
     	RenderableOp renImage = JAI.createRenderable("renderable", pb, renderingHints);
     	/*
        pb = new ParameterBlockJAI("translate", "renderable");
        pb.addSource(renImage);
        pb.setParameter("xTrans", (float) (0 - img.getMinX()));
        pb.setParameter("yTrans", (float) (0 - img.getMinY()));
        renImage = JAI.createRenderable("translate", pb, null);
        */
     	/// prepare translation
     	///
     	// Move background image to 0,0
     	/*
         pb = new ParameterBlockJAI("translate", "renderable");
         pb.addSource(img);
         pb.setParameter("xTrans", (float) (400 - img.getMinX()));
         pb.setParameter("yTrans", (float) (200 - img.getMinY()));
         RenderableOp renderableImage = JAI.createRenderable("translate", pb, null);
         */
 /*
         // Move overlay to target position:
         pb = new ParameterBlockJAI("translate", "rendered");
         pb.addSource(src1);
         pb.setParameter("xTrans", (float) (400 - overlayImg.getMinX()));
         pb.setParameter("yTrans", (float) (200 - overlayImg.getMinY()));
         overlayImg = JAI.create("translate", pb, null);        
         */
                 
     	// overlay has to be square!
     	int w=0, h=0;
     	/*
     	System.out.println(
     			img.getWidth()+"/"+img.getHeight()+","+overlayImg.getHeight()+"/"+overlayImg.getWidth()
     			+ " :: "+((float)img.getHeight()/overlayImg.getHeight())*overlayImg.getWidth());
     	*/
 		if (img.getWidth() > img.getHeight()) {
 			// image is landscape
 			log.debug("image size w="+img.getWidth()+", h="+img.getHeight());
 	 		
 			w = img.getWidth();
 			h = (int)(((float)w/overlayImg.getWidth())*overlayImg.getHeight());
 			
 		} else {
 			// image is portrait
 			h = img.getHeight();
 			w = (int)(((float)h/overlayImg.getHeight())*overlayImg.getWidth());
 		}
 		log.debug("image output size w="+w+", h="+h);
 		
     	//System.out.println(w);
     	try {     		
     		if (overlayPos.equals("full")) {
		     	// Setup and create the composite operation.        
		     	pb = new ParameterBlockJAI("composite");
		     	pb.addSource( renOverlayImage.createScaledRendering(w,h, renderingHints) );
		     	
		     	//pb.addSource( src2.createScaledRendering(img.getWidth(), img.getHeight(), null));
		     	pb.addSource( renImage.createScaledRendering(img.getWidth(), img.getHeight(), renderingHints) );
		     	pb.setParameter("source1Alpha", renOverlayAlpha.createScaledRendering(w,h, renderingHints) );
		     	pb.setParameter("source2Alpha", null);
		     	pb.setParameter("alphaPremultiplied", false);
		     	pb.setParameter("destAlpha", CompositeDescriptor.NO_DESTINATION_ALPHA);
		     	
		     	// move back image to original position
		     	RenderedOp composite= JAI.create("composite", pb, renderingHints);
		     	BufferedImage bi =  new
		     	RenderedImageAdapter(img).getAsBufferedImage();
		     	bi.getRaster().setRect(composite.getData());
		     	return new RenderedImageAdapter(bi);
		     	
     		} else {
     			
     			// positioned overlay   
     			AffineTransform screenResolution = new AffineTransform();
     			RenderContext renderContext = new RenderContext(screenResolution, renderingHints);
     					     	
		     	RenderedImage ovImg = renOverlayImage.createRendering(renderContext);
		     	RenderedImage ovAlpha = renOverlayAlpha.createRendering(renderContext);		     	

		     	// move overlay
		     	pb = new ParameterBlockJAI("translate");
		        pb.addSource(ovImg);
		        
		        float overlayX = 0.0f;
		        float overlayY = 0.0f;
		        float overlayAlphaX = 0.0f;
		        float overlayAlphaY = 0.0f;
		        if (overlayPos.charAt(1) == 'c') {
		        	overlayX = ((float)img.getWidth() - ovImg.getWidth())/2;
		        	overlayAlphaX = ((float)img.getWidth() - ovAlpha.getWidth())/2;
		        }
		        else if (overlayPos.charAt(1) == 'r') {
		        	overlayX = ((float)img.getWidth() - ovImg.getWidth());
		        	overlayAlphaX = ((float)img.getWidth() - ovAlpha.getWidth());
		        }
		        
		        if (overlayPos.charAt(0) == 'c') {
		        	overlayY = ((float)img.getHeight() - ovImg.getHeight())/2;
		        	overlayAlphaY = ((float)img.getHeight() - ovAlpha.getHeight())/2;
		        }
		        else if (overlayPos.charAt(0) == 'b') {
		        	overlayY = ((float)img.getHeight() - ovImg.getHeight());
		        	overlayAlphaY = ((float)img.getHeight() - ovAlpha.getHeight());
		        }
		        	
		        //overlayY=220.0f;
		        //overlayAlphaY=220.0f;
		        
		        pb.setParameter("xTrans", overlayX);
		        pb.setParameter("yTrans", overlayY);		        
		        
		        ovImg = JAI.create("translate", pb, renderingHints);		  
		        
		        // move alpha channel
		        pb = new ParameterBlockJAI("translate");
		        pb.addSource(ovAlpha);
		        pb.setParameter("xTrans", overlayAlphaX);
		        pb.setParameter("yTrans", overlayAlphaY);
		        log.debug("alpha tx="+overlayAlphaX+" ty="+overlayAlphaY+" / original image = ("+img.getWidth()+","+img.getHeight()+"), overlay image ("+ovImg.getWidth()+","+ovImg.getHeight());
		        ovAlpha = JAI.create("translate", pb, renderingHints);
		     	//

/*	            
	            //pb.add(new InterpolationBicubic2(4));
	            img = JAI.create("scale", pb, null); 
		        pb = new ParameterBlockJAI("scale");
		        pb.addSource(renImage);
		        // scale factors x and y
		        pb.setParameter("xScale", img.getWidth()/renImage.getWidth());
		        pb.setParameter("yScale", img.getHeight()/renImage.getHeight());
		        // translation x and y
		        pb.setParameter("xTrans", 0.0f);
		        pb.setParameter("yTrans", 0.0f);	
		        pb.add(new InterpolationBicubic(4));
		        renImage = JAI.createRenderable("scale", pb, renderingHints);
*/
		        
		        pb = new ParameterBlockJAI("composite");
		     	pb.addSource( ovImg );
		     	pb.addSource( renImage.createDefaultRendering() ); 		     			
		     			
		     	log.debug("---------> "+ovImg.getWidth()+"/"+ovImg.getHeight());
		     	
		     	pb.setParameter("source1Alpha", ovAlpha );
		     	pb.setParameter("source2Alpha", null);
		     	pb.setParameter("alphaPremultiplied", false);
		     	pb.setParameter("destAlpha", CompositeDescriptor.NO_DESTINATION_ALPHA);
		     	
		     	// move back image to original position
		     	RenderedOp composite= JAI.create("composite", pb, renderingHints);
		     			        
		     	BufferedImage bi =  new RenderedImageAdapter(img).getAsBufferedImage();
		     	bi.getRaster().setRect(composite.getData());
		     	return new RenderedImageAdapter(bi);
     		}
     	} catch (Exception e) {
     		log.error(e);
     		
     	}
     	
     	return null;
     }
     
     /**
      * Outputs an image from an image/jpg InputStream to a Java OutputStream. The image is
      * scaled down to a bounding box specified with sw and sh.
      *  
      * @param response
      * @param rec
      * @param sw
      * @param sh
      * @throws IOException
      * @throws CumulusException 
      */
     public String outputImage(MultiCatalogCollection mc, 
                                 String id, String size, PlanarImage overlay, ImageOutputStream out, String overlayPos) 
         throws IOException, CumulusException {
         
    	RenderingHints renderingHints = getRenderingHints();
    	
    	String finalSize = "unknown";
     	String[] ids = id.split(",");
    	int catId = Integer.parseInt(ids[0]);
    	int recId = Integer.parseInt(ids[1]);
   	 
         log.debug("display image "+id+" with size "+size);
         log.debug("search result contains "+mc.countRecords()+" records");         
         
         Integer width=null, height=null;
         
         if (size != null && size.indexOf("x") >= 0) {
             String sw = size.substring(0, size.indexOf('x'));
             log.debug(sw+" / size="+size);
             String sh = size.substring(size.indexOf('x')+1, size.length());
             
             if (sw != null) width = new Integer(sw);
             if (sh != null) height = new Integer(sh);
         }
                 
         
         if (mc.countRecords() > 0) {
             
             Record rec = mc.getRecord(catId, recId);
                
             // supported image formats: TIFF, PNG, GIF, JPEG
             Field f = rec.getFields().getFieldByID(GUID.UID_REC_FORMAT);
             boolean supported = false;
             String fileFormat = null;
             if (f.hasValue()) {
             	fileFormat = (String)f.getValue();
             	if (fileFormat.startsWith("JPEG") ||
             		fileFormat.startsWith("TIFF")||
             		fileFormat.startsWith("PNG") ||
             		fileFormat.startsWith("GIF")	) {
             		
             		supported = true;
             	}
             }
             
             RenderedImage img = null;
 	           
             int w = 0;
             int h = 0;
             
             if (supported) {
 	            // create image
 	            AssetReference ref = rec.getAssetReference();
 	            InputStream is = ref.getAsset().openInputDataStream();
 	            SeekableStream jaiStream = SeekableStream.wrapInputStream(is, true);
 	            

 	            try {
 	                img = JAI.create("stream", jaiStream);
 	                
 	                // does not support CMYK JPEG:
 	                // img = ImageIO.read(is);
 	                
 	                if (fileFormat.startsWith("JPEG") && img.getColorModel().hasAlpha()) {
 	                	// probably CMYK JPEG read as RGBA
 	                	log.debug("original is JPEG/CMYK-RGBA, converting to RGB");
 	                	img = convertJPEGCMYKtoRGB(img, "conf/EuroscaleCoated.icc");
 	                }
 	                
 	                // rescale image
 	                w = img.getWidth();
 	                h = img.getHeight();
 	                log.debug("original image is "+img.getWidth()+"/"+img.getHeight());
 	                
 	            } catch (java.lang.RuntimeException e) {
 	                // JAI can't render image: just output scaled thumbnail      
 	            	log.warn("can't read original image, using thumbnail");
 	                img = renderThumbnail(rec);
 	                
 	            }
             } else {

             	img = renderThumbnail(rec);
             	
             }

             // rescale image
             w = img.getWidth();
             h = img.getHeight();
             
             //float ratio = (float)w/h;
             
             //log.debug("width="+width.intValue()+" height="+height.intValue());
             int isw = w, ish = h;
             if (width != null) isw = width.intValue();
             if (height != null) ish = height.intValue();
             
             // has to fit into bounding box        
             float scaleX = (float)isw/w;
             float scaleY = (float)ish/h;
             
             if (w < h && scaleY < scaleX) {
                 // fit in height for portrait images
                 scaleX = scaleY;
             }
             
             if (w > h && scaleY > scaleX) {
                 // fit in width for landscape images
                 scaleY = scaleX;
             }
             
             log.debug("scales("+scaleX+","+scaleY+")");
             ParameterBlock pb = null;
             if (scaleX > 1.0 || scaleY > 1.0) {             
	             pb = new ParameterBlock();
	             pb.addSource(img);
	             pb.add(scaleX);
	             pb.add(scaleY);
	             pb.add(0.0F);
	             pb.add(0.0F);
	             pb.add(new InterpolationBicubic(4));
	             //pb.add(new InterpolationBicubic2(4));
	             img = JAI.create("scale", pb, renderingHints);   
             } 
             else 
             {             
	             // low pass filter scale down
	             pb = new ParameterBlock();
	             pb.addSource(img);
	             pb.add((double)scaleX);
	             pb.add((double)scaleY);
	             //pb.add(new InterpolationBicubic2(8));             
	             img = JAI.create("SubsampleAverage", pb, renderingHints);             
             }
             
             if (img != null) {
            	 finalSize = img.getWidth() + "x" + img.getHeight();
             }
             
             //System.out.println(img.getColorModel().getColorSpace().getName(0));
             // colorspace conversion (Gray->RGB, CMYK->RGB)
             if (img.getColorModel().getColorSpace().getType() == ColorSpace.TYPE_GRAY) {
            	log.debug("original is grayscale image, converting to RGB");
     	    	double[][] matrix = { {1,0}, {1,0}, {1,0} };
     	    	pb = new ParameterBlock();
     	    	pb.addSource(img);
     	    	pb.add(matrix);
     	    	img = JAI.create("bandcombine", pb, renderingHints);
         	}
             
            // KJ/ETH: 20081027
         	if (img.getColorModel().getColorSpace().getType() == ColorSpace.TYPE_RGB &&
         		img.getColorModel().hasAlpha()	
         	    ) {
         		// remove alpha channel of original image
             	pb = new ParameterBlock();
             	pb.addSource(img);
             	pb.add(new int[] {0, 1, 2});
             	img = JAI.create("bandSelect", pb, renderingHints);
             	
             	log.debug("removed alpha channel from rgb image");
         	}
         		
         	if (img.getColorModel().getColorSpace().getType() == ColorSpace.TYPE_CMYK) {
         		// convert CMYK to RGB 
         		img = convertCMYKtoRGB(img, "conf/EuroscaleCoated.icc");
         		log.info("converted from CMYK to RGB "+img.getColorModel().getColorSpace().getName(0));
         		
         	}
         	
         	// KJ/ETH: 20081027 test if image is 16-bit rgb
         	if (img.getSampleModel().getSampleSize(0) == 16) {
         		log.info("found 16-bit image: converting to 8-bit RGB");
         		/*
         		pb = new ParameterBlock();
         		pb.addSource(img);
         		ColorModel cm = 
         			new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_LINEAR_RGB),
         		                                                      new int[] {8,8,8}, 
         		   						      false, 
         		        					  false, 
         		        					  Transparency.OPAQUE, 
         		        					  DataBuffer.TYPE_BYTE);
         		pb.add(cm);
         		img = JAI.create("ColorConvert", pb);
         		*/
         		log.warn("16-bit to 8-bit conversion of images not implemented - using thumbnail as fallback");
         		img = renderThumbnail(rec);
         	}
         	
            // add overlay (if available)
         	RenderedImage imgOverlayed = null;
             if (overlay != null) {             	
            	log.debug("rendering overlay at "+overlayPos);
             	imgOverlayed = getOverlay(img, overlay, overlayPos);
             }
                          
             //response.addHeader("Content-Type","image/jpeg");
             if (imgOverlayed != null) {
            	 writeJPEGImage(imgOverlayed, out, 0.8f);            	 

             } else {
            	 if (img != null) {
            		 writeJPEGImage(img, out, 0.8f);                 		 
            	 }
             }
                          
         }
         
         return finalSize;
     }
     
     protected void writeJPEGImage(RenderedImage img, ImageOutputStream out, float quality) throws IOException {
    	 //ImageIO.write(img, "jpg", out);
    	 
    	 ImageWriter writer = null;
    	 Iterator iter = ImageIO.getImageWritersByFormatName("JPEG");
    	 if (!iter.hasNext()) {
    		 throw new IOException("ImageIO error - no writers available for JPEG format");
    	 }
    	 writer = (ImageWriter)iter.next();
    	     	 
    	 writer.setOutput(out);
    	 JPEGImageWriteParam iwp = new JPEGImageWriteParam(null);
    	 iwp.setCompressionMode(JPEGImageWriteParam.MODE_EXPLICIT);    	
    	 iwp.setCompressionQuality(quality);
    	 writer.write(null,new IIOImage(img, null, null), iwp);    	 
    	 out.flush();
    	 writer.dispose();    	     	 
     }
     
     
     /**
      * Code copied from: http://archives.java.sun.com/cgi-bin/wa?A2=ind0212&L=jai-interest&P=R2198
      */
     public static RenderedImage convertCMYKtoRGB(RenderedImage op, String profileName) {
         try {
             // -- Convert RGBA to CMYK
             // -- because JAI reads CMYK as RGBA
             ICC_Profile cmyk_profile = ICC_Profile.getInstance(profileName);
             ICC_ColorSpace cmyk_icp = new ICC_ColorSpace(cmyk_profile);
             ColorModel cmyk_cm =
                 RasterFactory.createComponentColorModel(op.getSampleModel().getDataType(),
                                                         cmyk_icp,
                                                         false,
                                                         false,
                                                         Transparency.OPAQUE);
             ImageLayout cmyk_il = new ImageLayout();
             cmyk_il.setColorModel(cmyk_cm);
             RenderingHints cmyk_hints = new RenderingHints(JAI.KEY_IMAGE_LAYOUT, cmyk_il);
             ParameterBlockJAI pb = new ParameterBlockJAI("format");
             pb.addSource(op);
             pb.setParameter("datatype", op.getSampleModel().getDataType());
             op = JAI.create("format", pb, cmyk_hints);

             // -- Convert CMYK to RGB
             ColorSpace rgb_icp = ColorSpace.getInstance(ColorSpace.CS_sRGB);
             ColorModel rgb_cm =
                 RasterFactory.createComponentColorModel(op.getSampleModel().getDataType(),
                                                         rgb_icp,
                                                         false,
                                                         false,
                                                         Transparency.OPAQUE);
             ImageLayout rgb_il = new ImageLayout();
             rgb_il.setSampleModel(rgb_cm.createCompatibleSampleModel(op.getWidth(),
                                                                      op.getHeight()));
             RenderingHints rgb_hints = new RenderingHints(JAI.KEY_IMAGE_LAYOUT, rgb_il);
             pb = new ParameterBlockJAI("colorconvert");
             pb.addSource(op);
             pb.setParameter("colormodel", rgb_cm);
             op = JAI.create("colorconvert", pb, rgb_hints);
         } catch(Exception ex) {
             ex.printStackTrace();
         }
         return op;
     }
          
 	private RenderedOp renderThumbnail(Record rec) throws CumulusException, IOException {
 		//		 JAI can't render image: just output scaled thumbnail                
         Field thumbnail = rec.getFields().getFieldByID(GUID.UID_REC_THUMBNAIL);
                        
         byte[] data = (byte[])thumbnail.getValue();
         Pixmap piximg = new Pixmap(data);
         //byte[] scaledImage = piximg.getAsJPEG(1000,0,8);
         byte[] scaledImage = piximg.getAsJPEG();
         
         return JAI.create("stream", new ByteArraySeekableStream(scaledImage));        
 	}
 	
 	/**
     * Returns the specified Cumulus recordview.
     * @param s Cumulus session
     * @param name set name, e.g. "Bilder (EXIF)"
     * @param viewClass view type, e.g. "com.canto.recordviewsets.views.ThumbnailView"
     * 
     * @return the recordview object
     */
    private RecordView getRecordView(CumulusSession s, String name, String viewClass) {
        ServerRoot serverRoot = (ServerRoot)s.getPreferences().getPreferences("/");
        //User user2 = serverRoot.getUsers().getUser(serverRoot.getCurrentActiveUser());
        User user = serverRoot.getUsers().getUserByUID(serverRoot.getCurrentActiveUserUID());
        
        Map recordViewSetsMap = new TreeMap();  
        RecordViewSetList sharedRecordViewSetList = (RecordViewSetList)serverRoot.getServerSettings().getRecordViewSetList();
        for(Iterator it = sharedRecordViewSetList.getRecordViewSetNames().iterator(); it.hasNext(); ){
            String recordViewSetName = (String)it.next();
            recordViewSetsMap.put(recordViewSetName, sharedRecordViewSetList.getRecordViewSet(recordViewSetName));
            //log.debug("adding[1] recordviewset "+recordViewSetName);
        }
        RecordViewSetList ownRecordViewSetList = user.getRecordViewSetList();
        for(Iterator it = ownRecordViewSetList.getRecordViewSetNames().iterator(); it.hasNext(); ){
            String recordViewSetName = (String)it.next();
            recordViewSetsMap.put(recordViewSetName, ownRecordViewSetList.getRecordViewSet(recordViewSetName));
            //log.debug("adding[2] recordviewset "+recordViewSetName);
        }
        
        /*
        String currentRecordViewSetName = (String)session.getServletContext().getInitParameter("DefaultRecordViewSet.InfoView");
        if (currentRecordViewSetName == null) {
            currentRecordViewSetName = "Bilder (EXIF)";
        }
        */
        RecordViewSet recordViewSet = (RecordViewSet)recordViewSetsMap.get(name);
        //log.debug("loading view "+recordViewSet+" with class "+viewClass);
        com.canto.cumulus.prefs.RecordView recordView = recordViewSet.getRecordViewList().getRecordView(viewClass);
        
        return recordView;        
    }
    
    private GUID[] getFieldList(CumulusSession session, String recordViewSet, String viewType) {
        RecordView recordView = getRecordView(session, recordViewSet, viewType);
        RecordFieldList recFieldList = recordView.getRecordFieldOptions().getRecordFieldList();
        
        int cmsFieldCount = 2;
        GUID[] fieldIDs = new GUID[recFieldList.getRecordFields().size()+cmsFieldCount];
                
        Iterator it = recFieldList.getRecordFields().iterator();
        int fi=0;
        while (it.hasNext()) {
	        RecordField recField = (RecordField)it.next();
	        fieldIDs[fi] = new GUID(recField.getUID());
        
	        fi++;
        }     
       
        // add required CMS fields
        
        // Thumbnail Width, Thumbnail Height
        int count=0;
        fieldIDs[recFieldList.getRecordFields().size()+count++] = new GUID("{a187f29d-d1f4-4581-b05f-c94f97f6d705}");
        fieldIDs[recFieldList.getRecordFields().size()+count++] = new GUID("{0bc13111-5dd0-4f6c-be94-2059fbbe5fa5}");
        
        return fieldIDs;
    }

    public SimpleDateFormat getDefaultDateFormat() {
    	//return new SimpleDateFormat("yyyy/MM/dd HH:mm:ss z");    	
    	//return new SimpleDateFormat("dd.MM.yyyy HH:mm:ss z");
    	return new SimpleDateFormat("dd.MM.yyyy");
    }
    public String dateOnlyConvert(String strDate) {
    	DateOnly dt;
		try {
			dt = DateOnly.parse(strDate);
			return DateOnly.format(dt);  
		} catch (ParseException e) {
			log.warn("can't convert dateonly value ["+strDate+"]", e);
		}
    	return "";
    }
    private String formatField(String catName, String recName, Object obj, FieldDefinition fDef) throws CumulusException, UnsupportedEncodingException {
    	if (obj == null) return "";
    	
        if (fDef.getFieldType() == FieldTypes.FieldTypeString) {        	
            return obj.toString();            
        } 
        else
        if (fDef.getFieldType() == FieldTypes.FieldTypeInteger) {
        	
            if (fDef.getValueInterpretation() == fDef.VALUE_INTERPRETATION_DATE_ONLY) {
                DateOnly dateValue = new DateOnly((Integer)obj);
                String dvStr = Integer.toString(dateValue.getYear());
                
                if (dateValue.getMonth() > 0) {                	
                	if (dateValue.getDay() > 0) {
                    	dvStr = Integer.toString(dateValue.getDay()) + "." + 
                    			Integer.toString(dateValue.getMonth()) + "." + dvStr;                
                    } else {
                    	dvStr = Integer.toString(dateValue.getMonth()) + "/" + dvStr;
                    }
                } 
                
                //log.debug("has dateonly value="+dvStr);                
                return dvStr;
                
                //return dateValue.toString();
                //log.debug("has dateonly value="+DateOnly.format(dateValue, Locale.US)+" "+dateValue.getYear());
                //return DateOnly.format(dateValue, Locale.GERMANY);
            }            
                        
            return obj.toString();
        } 
        else 
        if (fDef.getFieldType() == FieldTypes.FieldTypeDate) {
        	java.util.Date recDate = (java.util.Date)obj;
        	SimpleDateFormat df = getDefaultDateFormat();
        	return df.format(recDate);
        }
        else
        if (fDef.getFieldType() == FieldTypes.FieldTypeEnum) {
        	int[] stringIDs = null;
        	// multi combo
        	if (fDef.getValueInterpretation() == fDef.VALUE_INTERPRETATION_STRING_ENUM_MULTIPLE_VALUES) {
        		stringIDs = (int[]) obj;
        	} else {
        		stringIDs = new int[1];
        		stringIDs[0] = ((Integer)obj).intValue();
        	}
        	        	
        	StringEnumList ls = fDef.getStringEnumList();
        	StringBuffer buf = new StringBuffer();
        	for (int i=0; i<stringIDs.length; i++) {
        		StringEnum se = ls.getStringByID(stringIDs[i], 1);
        		buf.append(se.getString());
        		if (i < stringIDs.length-1) {
        			buf.append(", ");
        		}
        	}
        	return buf.toString();
        	        	
        }
        else
        if (fDef.getFieldType() == FieldTypes.FieldTypeDouble) {
        	return Double.toString((Double)obj);
        }
        if (fDef.getFieldType() == FieldTypes.FieldTypePicture) {     
        	
            /*
            // just return URL to retrieve thumbnail
            HashMap p = new HashMap();
            p.put("id", recName);
            p.put("cmd", "thumbnail");
            p.put("c", request.getParameter("c"));
                   
	       if (request.getParameter("user") != null) {
	       		p.put("user", request.getParameter("user"));
	       		p.put("pass", request.getParameter("pass"));
	       }
       		
        	
            StringBuffer sb = new StringBuffer(createCommandLink(request, p));
            return sb.toString();
            */
        	return "";
        }        
       
        return "";
    }
     
    public String getCategoryPath(Category cat) {
    	if (cat == null) return "";
    	int count=0;
    	StringBuffer buf = new StringBuffer();
    	while (cat != null && cat.getParentCategory() != null && cat.getType() != Category.MasterCategory) {
    		String catName = cat.getName();
    		if (count > 0) {
    			catName += " > ";
    		}
    		buf.insert(0, catName);
    		cat = cat.getParentCategory();
    		count += 1;
    	}
    	
    	return buf.toString();
    }
    private Element printRecord(String catName, FieldDefinitions fDefs, 
    					GUID[] guids, Record rec) throws CumulusException, UnsupportedEncodingException {
       /*
       int recId = rec.getRecordID();
       int catalogId = rec.getCatalogID();
       */
               
       
       Element recRoot = new Element("record", nsEpics);
       //recRoot.setAttribute("recordId", String.valueOf(recId), epicsNs);
       //recRoot.setAttribute("catalogId", String.valueOf(catalogId), epicsNs);
       
       if (rec == null || rec.getFields() == null) 
    	   return recRoot;
       
       String recName = (String)rec.getFields().getFieldByID(GUID.UID_REC_RECORD_NAME).getValue();
       recRoot.setAttribute("recordName", recName, nsEpics);       
       recRoot.setAttribute("catalogName", catName, nsEpics);
       int catId = rec.getCatalogCollection().getCatalog().getID();
       int recId = rec.getID();
       recRoot.setAttribute("internalId", catId + "," + recId, nsEpics);
       
       for (int i=0; i<guids.length; i++) {
       	   Field f = null; 
       	   Object obj = null;
       	   try {
       		   f = rec.getFields().getFieldByID(guids[i]);    
       		   
       		   obj = (f != null && f.hasValue()) ? f.getValue() : null;
       		   
       	   } catch (CumulusException e) {
       		   //log.warn("Field not found: "+guids[i]);
       		   //log.warn(e);
       	   }
           //Object obj = (f != null && f.hasValue()) ? f.getValue() : null;
       	   
       	   // TODO: language switch for name
       	   //log.debug(f.getFieldDefinition().getName(1));
       	
           // only continue if field exists for this record
           if (f != null) {
               
               //log.debug(guids[i]);
               FieldDefinition fDef = fDefs.getFieldDefinitionByID(guids[i]);        
               //log.debug(fDef.getName());
               
               //if (obj != null) {
                   Element n = new Element("field", nsEpics);
                   
                   //GUID guid = fDef.getFieldUID();
                   
                   n.setAttribute("id", guids[i].toString(), nsEpics);
                   n.setAttribute("type", Integer.toString(fDef.getFieldType()), nsEpics);
                   n.setAttribute("fieldName", fDef.getName(1), nsEpics);
                   //log.debug(fDef.getName());
                   
                   recRoot.addContent(n);
    
                   // get field definitions by catalog etc. etc. 
                   Element t = new Element("content", nsEpics);
                   
                   /*
                   StringBuffer sb = new StringBuffer();
                   sb.append(request.getRequestURL());
                   */
                   
                   if (guids[i].equals(GUID.UID_REC_CATEGORIES)) {
                	   Categories cats = rec.getCategories();
                	   
                	   for (int j=0; j<cats.countCategories(); j++) {
                		   Category cat = cats.getCategory(j);
                		   String cName = getCategoryPath(cat);
                		   //log.debug(cName);
                		   Element el = new Element("category", nsEpics);
                		   el.setText(cName);
                		   t.addContent(el);
                	   }
                	   
                   } else {
                	   try {
                		   String content = formatField(catName, recName, obj, fDef);
                		   content = content.replaceAll("\\x0B", " ");
                		   content = content.replaceAll("\\x0C", " ");
                		   
                		   t.setText(content);
                	   } catch (org.jdom.IllegalDataException e) {
                		   log.error(e);
                	   }
                   }
                   
                   n.addContent(t);
                                      
               //}
           }
       }
        
       // add full resolution link
       /*
       Element f = new Element("link");
       f.setAttribute("name", "fullResolution");
       
       HashMap p = new HashMap();
       p.put("id", recName);
       p.put("cmd", "image");
       p.put("c", request.getParameter("c"));
       
       if (request.getParameter("user") != null) {
       		p.put("user", request.getParameter("user"));
       		p.put("pass", request.getParameter("pass"));
       }
       
       f.addContent(new Text(createCommandLink(request, p))); 
                  
       recRoot.addContent(f);
       */
       
       return recRoot;
    }
        
    /**
     * Retrieves record data for a set of cumulus records (TODO:check ranges) and
     * converts each record using the given XSL.
     * 
     * @param mc
     * @param viewSet
     * @param viewType
     * @param from
     * @param to up to and included
     * @return
     * @throws CumulusException
     * @throws UnsupportedEncodingException
     */
    public Element getMetadataSet(MultiCatalogCollection mc,
                    String viewSet, String viewType, int from, int to) throws CumulusException, UnsupportedEncodingException {        
    	    	
        GUID[] fieldIDs = getFieldList(mc.getCumulusSession(), viewSet, viewType);                      
        FieldDefinitions fDefs = getFieldDefinitions(mc);
        
        int index = from;
        int count = mc.countRecords();
        log.debug("index "+index+" of "+count+" records");
        
        Element list = new Element("list", nsEpics);
        while (index < count && index <= to) {
        	Record rec = mc.getRecord(index);
        	
            String catName = mc.getCatalogNames()[0];
            Element e = printRecord(catName, fDefs, fieldIDs, rec);                      
            list.addContent(e);    
            
            index++;
        }
           	
        return list;
    }
    
    /**
     * Gets the field definitions for a given catalog collection (first catalog
     * in a multicatalogcollection).
     * 
     * @param mc
     * @return
     */
    private FieldDefinitions getFieldDefinitions(MultiCatalogCollection mc) {
        int catalogId = mc.getCatalogIDs()[0]; //recData.getCatalogID()
        CatalogCollection catCollection = mc.getCatalogCollectionByCatalogID(catalogId);
        log.debug(catCollection.getCatalog().getName());
        FieldDefinitions fDefs = catCollection.getCatalog().getRecordLayout().getFieldDefinitions();
            
        return fDefs;
    }
    
    
    private void sort(MultiCatalogCollection mc, String key, String dir) throws CumulusException {
        FieldDefinitions fDefs = getFieldDefinitions(mc);
        FieldDefinition fDef = null;
        try {
        	fDef = fDefs.getFieldDefinition(key);
        } catch (CumulusException e) {
        	throw new RuntimeException(e);
        }
        
        GUID fuid = fDef.getFieldUID();
        log.debug("sorting by field guid "+(fuid==null ? "[]" : fuid.toString()));
        
        if (fuid != null) {
            mc.setSorting(fuid);
            if (dir != null && dir.equals("up")) {
                mc.setSortDirection(Cumulus.Ascending);
            } else {
                mc.setSortDirection(Cumulus.Descending);
            }
        }
    }

    
    /**
     * Retrieves record data (executes a Cumulus query).
     * 
     * @param params HTTP parameter HashMap (id, query, sortkey, sortdir)
     * @return the Cumulus MultiCatalogCollection with the query result
     * @throws CumulusException
     */
    public MultiCatalogCollection runQuery(MultiCatalogCollection mc, HashMap params) throws CumulusException {
        
        String id = (String)params.get("id");
        String query = (String)params.get("query");
        String sortKey = (String)params.get("sortkey");
        if (sortKey == null) 
        	sortKey = "Bildcode";
        
        String sortDirection = (String)params.get("sortdir");
        if (sortDirection == null)
        	sortDirection = "down";
        
        if (id != null) {

            // find one record
            mc.find("\"Record Name\" is \""+id+"\"", Cumulus.FindNew);
            
        } else {
                         
            if (query == null) {
                // find all
                log.debug("runQuery:mc.findAll()");
                mc.findAll();
                if (!sortKey.equals("none")) {
                	sort(mc, sortKey, sortDirection);
                }
            } else {
            	 
                log.debug("query string is "+query);
                mc.find(query, Cumulus.FindNew);
                if (!sortKey.equals("none")) {
                	sort(mc, sortKey, sortDirection);
                }
            }
                        
        }
        
        return mc;
    }
    
    public void outputXML(Document doc, OutputStream out) throws IOException {
        // nice printing and output streaming
        XMLOutputter output = new XMLOutputter();
                    
        output.setFormat(org.jdom.output.Format.getPrettyFormat());
        output.output(doc, out);        
    }
    
    /**
     * Cuts out part of a cumulus collection and returns the cutout part.
     * Very useful for cursor-based operations or paging. 
     * 
     * @param mc
     * @param start
     * @param end
     * @return
     */
    /*
    private MultiCatalogCollection cropCollection(MultiCatalogCollection mc, int start, int end) {
        MultiCatalogCollection mcClone = mc.cloneMultiCatalogCollection();
        mcClone.removeAllRecords();
        if (end > mc.countRecords()) {
            end = mc.countRecords();
        }
        
        for (int i=start; i<end; i++) {
            Record rec = mc.getRecord(i);
            mcClone.addRecord(mc.getRecordCatalogID(i), rec.getID());
        }
        
        return mcClone;
    }
    */
    
    
    
   
    
    public static RenderedImage convertJPEGCMYKtoRGB(RenderedImage src, String profileName) {
    	/*
    	    	
    	    	// code from http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4799903
    	    	Raster raster = op.getData();
    	    	ICC_Profile cmykProfile;
    			try {
    				cmykProfile = ICC_Profile.getInstance(profileName);
    			} catch (IOException e) {			
    				e.printStackTrace();
    				return null;
    			}
    	        ICC_ColorSpace cmykCS = new ICC_ColorSpace(cmykProfile);
    	        
    	        int w = raster.getWidth();
    			int h = raster.getHeight();
    			
    			BufferedImage rgbImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
    			WritableRaster rgbRaster = rgbImage.getRaster();
    			ColorSpace rgbCS = rgbImage.getColorModel().getColorSpace();
    			ColorConvertOp cmykToRgb = new ColorConvertOp(cmykCS, rgbCS, null);
    			cmykToRgb.filter(raster, rgbRaster);
    			
    	      ParameterBlock pb = new ParameterBlock();
    	      pb.addSource(rgbImage);
    	      return JAI.create("invert",pb);
    	*/		
    	      double[][] matrix = {
    		                    { -1.0D,  0.0D,  0.0D, 1.0D, 0.0D },
    		                    {  0.0D, -1.0D,  0.0D, 1.0D, 0.0D },
    		                    {  0.0D,  0.0D, -1.0D, 1.0D, 0.0D },
    		            };
    		   
    	        // Step 1: 4-band nach 3-band
    	        ParameterBlock pb = new ParameterBlock();
    	        pb.addSource(src);
    	        pb.add(matrix);
    	        // Perform the band combine operation.
    	        src = JAI.create("bandcombine", pb, null);
    		   
    	        // Step 2: CMY to RGB
    	        ParameterBlockJAI pbjai = new  ParameterBlockJAI("colorconvert");
    	        ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_sRGB);
    	        int[] bits = { 8, 8, 8 };
    	        ColorModel cm = new ComponentColorModel(cs,bits,false,false,Transparency.OPAQUE,DataBuffer.TYPE_BYTE);
    	        pbjai.addSource(src);
    	        pbjai.setParameter("colormodel", cm);
    		   
    	        // ImageLayout for RenderingHints
    	        ImageLayout il = new ImageLayout();
    	        // compatible sample model
    	        il.setSampleModel(cm.createCompatibleSampleModel(src.getWidth(),src.getHeight()));
    	        RenderingHints hints = new RenderingHints(JAI.KEY_IMAGE_LAYOUT, il);
    	        // Perform the color conversion.
    	        RenderedOp dst = JAI.create("colorconvert", pbjai, hints);
    		   
    		    return dst;
    	    }
    
    public void outputThumbnail(MultiCatalogCollection mc, String id, String size, OutputStream os) 
    			throws CumulusException, IOException {
    	String[] ids = id.split(",");
    	int catId = Integer.parseInt(ids[0]);
    	int recId = Integer.parseInt(ids[1]);

		int thumbSize = 1000;
	    Integer width=null, height=null;
	    
	    if (size != null && size.indexOf("x") >= 0) {
	        String sw = size.substring(0, size.indexOf('x'));
	        //log.debug(sw+" / size="+size);
	        String sh = size.substring(size.indexOf('x')+1, size.length());
	        
	        if (sw != null) width = new Integer(sw);
	        if (sh != null) height = new Integer(sh);
	        
	        if (width != null) thumbSize = width.intValue();
	        if (height != null) {
	        	if (height.intValue() > width.intValue()) {
	        		thumbSize = height.intValue();
	        	}
	        }
	    }
	
	            
	    Record rec = mc.getRecord(catId, recId);
	    Field thumbnail = rec.getFields().getFieldByID(GUID.UID_REC_THUMBNAIL);
	   
	    //byte[] data = (byte[])( (Pixmap)thumbnail.getValue() ).getData();
	    byte[] data = (byte[])thumbnail.getValue();
	    Pixmap img = new Pixmap(data);
	    byte[] scaledImage = img.getAsJPEG(thumbSize,0,8);
	    
	    //BASE64Encoder base64 = new BASE64Encoder();
	    //String jpegString = base64.encode(img.getAsJPEG());
	    
	    //int maxSize = (w > h) ? w : h;
	    //img.getAsJPEG(maxSize, 0, 8);
	    
	    
	    //response.getOutputStream().write(img.getData());
	    os.write(scaledImage);
	}
    

	public static void attachXMP(Log log, String exiftoolPath, String imgPath, String xmp) {
		return;
		/*
		File tempFile = null;
		try {
			tempFile = File.createTempFile(imgPath, "xmp");
		} catch (IOException e) {
			log.error(e);
			return;
		}
		
		String cmd = magickPath + " " + imgPath + 
			" -profile xmp:\"" + tempFile.getPath() + "\" "+imgPath;
		
		//execCmd(log, cmd);
		log.debug(cmd);
		
		return;
		*/
	}
	
	/**
	 * Execute external program.
	 * @param argv
	 */
	protected static int execCmd(Log log, String cmd) {
	    try {
	    	
	      Process p = Runtime.getRuntime().exec(cmd);
	      p.waitFor();
	      
	      return p.exitValue();
	    }
	    catch (Exception err) {
	      err.printStackTrace();
	    }
	    
	    return -1;
	}

}
