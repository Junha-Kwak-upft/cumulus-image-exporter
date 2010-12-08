package ch.ethz.epics.export;

import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.CharsetEncoder;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.zip.GZIPOutputStream;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageOutputStream;
import javax.media.jai.InterpolationBicubic;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.RenderedOp;


import org.jdom.Content;
import org.jdom.DocType;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.ProcessingInstruction;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.jdom.transform.XSLTransformer;
import org.jdom.xpath.XPath;
import org.w3c.dom.NodeList;


import ch.ethz.epics.integration.cumulus.CumulusTools;

import com.canto.cumulus.AssetReference;
import com.canto.cumulus.Cumulus;
import com.canto.cumulus.CumulusException;
import com.canto.cumulus.Field;
import com.canto.cumulus.GUID;
import com.canto.cumulus.MultiCatalogCollection;
import com.canto.cumulus.Pixmap;
import com.canto.cumulus.Record;
import com.sun.media.jai.codec.ByteArraySeekableStream;
import com.sun.media.jai.codec.SeekableStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;



public class XPort {
	private static Log log = LogFactory.getLog(XPort.class);
	private final static String NS_EPICS = "http://www.e-pics.ethz.ch/ns/integration";
		
	public final static Namespace nsEpics = Namespace.getNamespace("epics", NS_EPICS);
	private HashMap config;
	
	private CumulusTools cumulusTools = new CumulusTools();
    private final static int CUMULUS_MAX_COLLECTION = 1000;
    private MultiCatalogCollection mc = null;
    private CumulusTools ct = null;
    
    private final static int MODE_CREATE = 1;
    private final static int MODE_UPDATE = 2;
    private final static int MODE_SITEMAP = 4;    
    private int mode = MODE_CREATE;
    
    private final static int MODE_UPDATE_PAGES = 8;
    private final static int MODE_UPDATE_INDEX = 16;
    private int updateMode = MODE_UPDATE_PAGES;
    
    //private final static String DEFAULT_IMAGESIZE_ERROR = "640x480";
    
	private String consolidatePath(String path) {
		if (path!= null && !path.endsWith("/")) {
			return path+"/";
		}
		return path;
	}
	
	/**
	 * Reads the export configuration file and overwrites default values.
	 * 
	 * @param filename
	 * @return
	 * @throws JDOMException
	 * @throws IOException
	 * @throws CumulusException
	 */
	public synchronized HashMap readConfiguration(String filename) throws JDOMException, IOException, CumulusException {
        log.info("use export configuration from file ["+filename+"]");
        
        File configFile = new File(filename);
       
        SAXBuilder builder = new SAXBuilder();
        Document config = builder.build(configFile);
       
        XPath xpath = XPath.newInstance("/epics:export-config/epics:cumulus");  
        xpath.addNamespace(nsEpics);
        
        log.debug("reading cumulus configuration (namespace="+nsEpics);
        Element n = (Element)xpath.selectSingleNode(config);
            
        HashMap params = new HashMap();
        String server = n.getChildText("server", nsEpics);
        String user = n.getChildText("user", nsEpics);
        String password = n.getChildText("password", nsEpics);
        List catalogs = n.getChild("catalogs", nsEpics).getChildren();
        Iterator it = catalogs.iterator();
        Vector vc = new Vector();
        while (it.hasNext()) {
        	Element c = (Element)it.next();
        	String catalogName = c.getTextNormalize();
        	vc.add(catalogName);
        	log.debug("- catalog ["+catalogName+"]");        	
        }
        params.put("cumulus.catalogs", vc);
        params.put("cumulus.server", server);
        params.put("cumulus.user", user);
        params.put("cumulus.password", password);
        
        log.debug("finished reading cumulus configuration");
        
        xpath = XPath.newInstance("/epics:export-config/epics:output/epics:index");  
        xpath.addNamespace(nsEpics);
        n = (Element)xpath.selectSingleNode(config);
        
        log.debug("reading output settings");
        log.debug("- node index");
        String idxStyle = n.getChildText("stylesheet", nsEpics);
        String idxSeg = n.getChildText("segmentationCount", nsEpics);
        String idxDir = n.getChildText("targetDirectory", nsEpics);
        String idxSize = n.getChildText("imageSize", nsEpics);
        String idxView = n.getChildText("viewSet", nsEpics);
        String idxImgDir = n.getChildText("imageDirectory", nsEpics);
        String idxXML = n.getChildText("writeXML", nsEpics);
        String idxSitemap = n.getChildText("writeSitemap", nsEpics);
        String idxSitemapFile = n.getChildText("sitemapFile", nsEpics);
        String idxCompressSitemap = null;
        String idxSitemapLang = "en";
        if (idxSitemapFile != null && idxSitemapFile.length() > 0) {
        	idxCompressSitemap = n.getChild("sitemapFile", nsEpics).getAttributeValue("gzip");
        	idxSitemapLang = n.getChild("sitemapFile", nsEpics).getAttributeValue("lang");
        	if (!idxSitemapLang.equals("de") || !idxSitemapLang.equals("en") ||
        		!idxSitemapLang.equals("fr") || !idxSitemapLang.equals("it")) {
        		idxSitemapLang = "en";
        	}
        }
        
        String idxBaseUrl = n.getChildText("baseUrl", nsEpics);
        if (idxBaseUrl == null) {
        	idxBaseUrl = "/";
        } else if (!idxBaseUrl.endsWith("/")) {
        	idxBaseUrl += "/";
        }
        
        if (idxSitemap != null) {
        	idxSitemap = idxSitemap.toLowerCase();
        }
        
        xpath = XPath.newInstance("/epics:export-config/epics:system");  
        xpath.addNamespace(nsEpics);
        n = (Element)xpath.selectSingleNode(config);
        Element cn = n.getChild("exiftool", nsEpics);
        String sysExifTool = cn.getAttributeValue("path");
        params.put("system.exiftool.path", sysExifTool);
        
        params.put("index.stylesheet", idxStyle);
        params.put("index.segmentation", idxSeg);
        params.put("index.imagesize", idxSize);
        params.put("index.directory", consolidatePath(idxDir));
        params.put("index.imagedir", consolidatePath(idxImgDir));
        params.put("index.view", idxView);        
        params.put("index.writexml", idxXML);
        params.put("index.writesitemap", idxSitemap);
        params.put("index.baseUrl", idxBaseUrl);
        params.put("index.sitemapFile", idxSitemapFile);
        params.put("index.gzipsitemap", idxCompressSitemap);
        params.put("index.sitemapLanguage", idxSitemapLang);
        
        
        log.debug("- index stylesheet: "+idxStyle);
        
        log.debug("parsing metadata node");
        xpath = XPath.newInstance("/epics:export-config/epics:metadata/epics:query");  
        xpath.addNamespace(nsEpics);
        n = (Element)xpath.selectSingleNode(config);
        String query = n.getText();
        if (query != null && query.length() > 0) 
        	params.put("cumulus.query", query);
        log.debug("restricting to query "+query);
        log.debug("finished parsing metadata node");
        
        xpath = XPath.newInstance("/epics:export-config/epics:output/epics:record");  
        xpath.addNamespace(nsEpics);
        n = (Element)xpath.selectSingleNode(config);
        
        log.debug("- node record");
        String recStyle = n.getChildText("stylesheet", nsEpics);
        String recView = n.getChildText("viewSet", nsEpics);
        String recImage = n.getChildText("imageSize", nsEpics);
        String recWatermark = n.getChildText("watermark", nsEpics);
        String recWatermarkPos = n.getChildText("watermarkPos", nsEpics);
        String recImgDir = n.getChildText("imageDirectory", nsEpics);
        String recXML = n.getChildText("writeXML", nsEpics);
        
        if (recWatermarkPos != null) {
        	recWatermarkPos = recWatermarkPos.toLowerCase();        	        	      		        		
        }
        
        params.put("record.writexml", recXML);
        params.put("record.stylesheet", recStyle);
        params.put("record.imagesize", recImage);
        params.put("record.view", recView);
        
        PlanarImage overlay = null;
        if (recWatermark != null) {
        	overlay = JAI.create("fileload", recWatermark); 
        }
        params.put("record.watermark", overlay);
        params.put("record.watermarkPos", recWatermarkPos);
        
        params.put("record.imagedir", consolidatePath(recImgDir));
        
        log.debug("- record stylesheet: "+recStyle);
        
        log.debug("finished reading output settings");
        
        return params;
    }
	

    /**
     * Stops and unloads Cumulus native classes and processes.
     */
    public void finalize() {     	
        try {
            cumulusTools.stopCumulus();
            
        } catch (Exception e) {
            log.debug(e);
        }
    }
    
    /**
     * Initializes the export class. Reads configuration, sets defaults and
     * initializes Cumulus native classes.
     * 
     * @param configPath path including filename+extension of XML configuration file
     * @param mode values: "update", "create" (default), "sitemap"
     * @throws JDOMException
     * @throws IOException
     * @throws CumulusException
     */
	public XPort(String configPath, String mode, String updateMode) throws JDOMException, IOException, CumulusException {
		config = readConfiguration(configPath);
		if (mode != null && mode.equals("update")) {
			this.mode = MODE_UPDATE;
			
			if (this.mode == MODE_UPDATE && updateMode != null && updateMode.equals("pages")) {
				// updates all HTML/XML pages (but without rendering images)
				this.updateMode = MODE_UPDATE_PAGES;
			} else {
				// default: only update index pages
				this.updateMode = MODE_UPDATE_INDEX;
			}
				
		} else 
		if (mode != null && mode.equals("sitemap")) {
			this.mode = MODE_SITEMAP;
		} else
		{
			this.mode = MODE_CREATE;
		}
		
		cumulusTools.initCumulus();
	}
	
	public Object getParam(String name) {
		if (config.get(name) != null)
			return config.get(name);
		
		if (name.equals("index.view")) {
			return "E-PICS Standard";
		}
		
		return null;
	}
	
	private void outputIndexDebug(Document doc) throws IOException {
        // XML intermediate output
        Format xmlFormatDebug = Format.getPrettyFormat().setOmitDeclaration(false);
        xmlFormatDebug.setEncoding("UTF-8");
        XMLOutputter outputDebug = new XMLOutputter(xmlFormatDebug);
        outputDebug.output(doc, System.out);
	}
	
	private Document transformIndex(Document doc) throws JDOMException, IOException {
				
	    //outputIndexDebug(doc);             
        XSLTransformer transformer = new XSLTransformer((String)getParam("index.stylesheet"));  
        Document target = transformer.transform(doc);
        //outputIndexDebug(target);
        
        return target;
		/*
		XPath xpath = XPath.newInstance("//epics:record");  
        xpath.addNamespace(nsEpics);
        
        List recordNodes = xpath.selectNodes(result);
        log.debug(recordNodes.size());
        Iterator it = recordNodes.iterator();
        while (it.hasNext()) {
        	Element n = (Element)it.next();
        	log.debug("processing "+n.getName());
        }
        */
	}

	private String formatNumber(int n) {
		NumberFormat nf = NumberFormat.getInstance();
		nf.setMinimumIntegerDigits(6);
		nf.setGroupingUsed(false);
		nf.setParseIntegerOnly(true);
		
		return nf.format(n);
	}
	
	private String getFilename(String baseName, int number) {
		return baseName + "-" + formatNumber(number);
	}
	
	private void setLinks(List recordNodes, Element n, int index, String baseName, int startCount) throws UnsupportedEncodingException {
    	// previous
    	String prevName = "";
    	String prevLink = "";
    	String prevId = "";
    	String prevCatalog = "";
    	if (index > 0) {
    		
    		Element prev = (Element)recordNodes.get(index-1);
    		prevName = URLEncoder.encode(prev.getAttributeValue("recordName", nsEpics), "utf-8");
    		prevId = prev.getAttributeValue("internalId", nsEpics);
    		prevCatalog = URLEncoder.encode(prev.getAttributeValue("catalogName", nsEpics), "utf-8");
    		//prevLink = getFilename(baseName, startCount+index - 1);
    		prevLink = getGlobalId(prevCatalog, prevName, prevId);
    	}
    	n.setAttribute("prevLink", prevLink, nsEpics);
    	n.setAttribute("prevName", prevName, nsEpics);
    	
    	// next
    	String nextName = "";
    	String nextLink = ""; 
    	String nextId = "";
    	String nextCatalog = "";
    	if (index < recordNodes.size()-1) {
    		//nextLink = getFilename(baseName, startCount+index + 1);
    		log.debug(nextLink);
    		Element next = (Element)recordNodes.get(index+1);
    		log.debug(next);
    		nextName = URLEncoder.encode(next.getAttributeValue("recordName", nsEpics), "utf-8");
    		nextId = next.getAttributeValue("internalId", nsEpics);
    		nextCatalog = URLEncoder.encode(next.getAttributeValue("catalogName", nsEpics), "utf-8");
    		
    		nextLink = getGlobalId(nextCatalog, nextName, nextId);
    	}
    	n.setAttribute("nextLink", nextLink, nsEpics);
    	n.setAttribute("nextName", nextName, nsEpics);
    	
    	n.setAttribute("catalogNameURL", URLEncoder.encode(n.getAttributeValue("catalogName",nsEpics), "utf-8"), nsEpics);    	
    	n.setAttribute("recordNameURL", URLEncoder.encode(n.getAttributeValue("recordName",nsEpics), "utf-8"), nsEpics);
	}
	
	private void outputResult(File f, Document doc) throws IOException {
		if (!f.exists()) f.createNewFile();
		
        // XML output
        Format xmlFormatDebug = Format.getPrettyFormat().setOmitDeclaration(true);
        //Format xmlFormatDebug = Format.getRawFormat().setOmitDeclaration(true);
        xmlFormatDebug.setEncoding("UTF-8");
        XMLOutputter xmlOutput = new XMLOutputter(xmlFormatDebug);
        
        // JDOM XHTML hack
        doc.setDocType(new DocType("html", "-//W3C//DTD XHTML 1.0 Transitional//EN", "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd" ));
        
        FileOutputStream fos = new FileOutputStream(f);
        OutputStreamWriter outsw = new OutputStreamWriter(fos, "UTF-8");        
        xmlOutput.output(doc, outsw);        
        outsw.close();
        fos.close();
	}
	
		
	private void outputXML(String dir, String baseName, Document doc, String stylesheet) throws Exception {
		log.debug("writing XML for record");
		File fStyle = new File(stylesheet);				
		if (stylesheet != null && stylesheet.length() > 0) {
			doc.addContent(0, new ProcessingInstruction("xml-stylesheet", "type='text/xsl' href='"+fStyle.getName()+"'"));
		}
		
		File f = new File(dir + baseName + ".xml");
		f.createNewFile();
		OutputStream os = new FileOutputStream(f);
		
		// XML output
        Format xmlFormatDebug = Format.getPrettyFormat().setOmitDeclaration(false);
        xmlFormatDebug.setEncoding("UTF-8");
        XMLOutputter xmlOutput = new XMLOutputter(xmlFormatDebug);
        
        OutputStreamWriter outsw = new OutputStreamWriter(new FileOutputStream(f), "UTF-8");        
        xmlOutput.output(doc, outsw);
        
        outsw.close();	
        os.close();
	}
	
	private void writeThumbnail(String dir, String baseName, String id) throws CumulusException, IOException {
		log.debug("get thumbnail for record "+id);
		String size = (String)getParam("index.imagesize");
		if (size == null) size = "100x100";
		log.debug("create file "+dir+baseName+".jpg");
		File f = new File(dir + baseName + ".jpg");
		try {
			f.createNewFile();
		} catch (IOException ie) {
			log.error("Failed to create image thumbnail for image '"+dir+baseName+".jpg"+"'", ie);
			return;
		}
		
		OutputStream os = null;
		try {
			os = new FileOutputStream(f);
			ct.outputThumbnail(mc, id, size, os);
		} catch (OutOfMemoryError e) {
			log.error("Out of memory in XPort.writeFullsize()", e);
		} catch (CumulusException e) {
			log.error(e);		
		} finally {
			if (os != null)
				os.close();
		}
	}

	private boolean hasChanged(java.util.Date lastRecordChange, java.util.Date lastFileChange) {
		if (this.mode == MODE_UPDATE) {
			// compares record and file modification date			
			if (lastRecordChange.compareTo(lastFileChange) > 0) {
				log.debug("record is newer than version on disk");
				return true;
			}
		}
		return false;
	}
	private String writeFullsize(String dir, String baseName, String id) throws CumulusException, IOException {
		log.debug("get full image for record "+id);
		String size = (String)getParam("record.imagesize");
		if (size == null) size = "400x400";
		String imgSize = "unknown";
		
		File f = new File(dir + baseName + ".jpg");
		if (!f.exists() || this.mode == MODE_CREATE) f.createNewFile();				
		
		ImageOutputStream ios = null;
		try {
			ios = ImageIO.createImageOutputStream(f);
			PlanarImage overlay = (PlanarImage)getParam("record.watermark");
			String overlayPos = (String)getParam("record.watermarkPos");			
			imgSize = ct.outputImage(mc, id, size, overlay, ios, overlayPos);
			
			// attach XMP metadata to image file (using exiftool!)
			String xmp = "";	
			String exiftoolPath = (String)getParam("system.exiftool.path");
			
			CumulusTools.attachXMP(log, exiftoolPath, f.getPath(), xmp);
			
		} catch (OutOfMemoryError e) {
			log.error("Out of memory in XPort.writeFullsize()", e);			
		} catch (CumulusException e) {
			log.error(e);
		}
		finally {
			if (ios != null)
				ios.close();
		}
	
		return imgSize;
	}
	
	private String getRelative(String dir) {
		//log.debug(dir);
		int i = dir.lastIndexOf("/");
		if (!dir.endsWith("/")) {
			return dir.substring(i+1)+"/";
		}
		
		dir = dir.substring(0, i);
		//log.debug(dir);
		i = dir.lastIndexOf("/");
		if (i >= 0) {
			//log.debug(dir);
			return dir.substring(i+1)+"/";
		} else {
			return dir+"/";
		}
	}
	
	private String getImageDimensions(String iDir, String globalId) {
		String dim = "unknown";
		try {
			BufferedImage img = ImageIO.read(new File(iDir + globalId + ".jpg"));
			if (img != null) {
				dim = img.getWidth() + "x" + img.getHeight();
				log.debug("got cached image dimensions="+dim);
			} else {
				log.warn("got a problem getting image dimensions: img=null for "+iDir+globalId+".jpg");
			}
		} catch (IOException e) {			
			log.error("Can't get image dimensions of cached image: ", e);
		}
		
		return dim;
	}
	
	
	private int transformRecords(int startCount, String indexLink, String dir, String tDir, String iDir, String baseName, Document doc, OutputStreamWriter imageSitemapOut) throws Exception {
		XPath xpath = XPath.newInstance("//epics:record");  
        xpath.addNamespace(nsEpics);
                  
        log.debug("- stylesheet used for record transformation: "+(String)getParam("record.stylesheet"));
        
        XSLTransformer transformer = new XSLTransformer((String)getParam("record.stylesheet"));        
        //Document target = transformer.transform(doc);
        
        List recordNodes = xpath.selectNodes(doc);
        log.debug("iterating over "+recordNodes.size()+" node(s)");        
        
        int index = startCount;
        
        // internal count in record list
        int count = 0;
        while (count < recordNodes.size()) {
        	// one record view
        	Element n = (Element)recordNodes.get(count);
        	
        	// public identifier (Bildcode)
        	String recordName = n.getAttributeValue("recordName", nsEpics);
        	
        	// public catalog name (internal catalog identifier is not stable!) 
        	String catalogName = n.getAttributeValue("catalogName", nsEpics);
        	
        	// internal record id (catalog wide)
        	String internalId = n.getAttributeValue("internalId", nsEpics);        	
        	
        	// image global identifier
        	String globalId = getGlobalId(catalogName, recordName, internalId);
        	
        	log.info("processing record "+globalId);
        	
        	n.detach();
        	Document src = new Document();
        	src.setRootElement(n);
        	        	
        	/*
        	Element elNext = (Element)recordNodes.get(index+1);
        	elNext.detach();
        	Document srcNext = new Document();
        	srcNext.setRootElement(elNext);
        	outputIndexDebug(srcNext);
        	*/
        	
        	// check for update mode
        	boolean doUpdate = true;
        	boolean doPageUpdate = false;
        	
        	//old naming: File f = new File(dir + getFilename("record",index)+ ".html");
        	File f = new File(dir + globalId + ".html");
        	if (f.exists() && (mode == MODE_UPDATE)) {        		
        		
	        	Date lastModified = new Date(f.lastModified());
	        	// find record modification date
	        	XPath rpath = XPath.newInstance("/epics:record/epics:field[@epics:id='{af4b2e02-5f6a-11d2-8f20-0000c0e166dc}']/epics:content");
	        	Element lastRecordModification = (Element)rpath.selectSingleNode(src);
	        	String recModDate = lastRecordModification.getTextNormalize();
	        	SimpleDateFormat df = ct.getDefaultDateFormat();
	        	java.util.Date dRecMod = df.parse(recModDate);
	        	if (dRecMod.compareTo(lastModified) <= 0) {
	        		// record and/or image has NOT changed since last update
	        		log.debug("skipping record "+globalId+" (update mode) record changed:"+dRecMod+" file:"+lastModified);
	        		doUpdate = false;	        		
	        	} 
	        	
	        	if (updateMode == MODE_UPDATE_PAGES) {
        			log.debug("will update record "+globalId+" (because of page update mode)");
        			doPageUpdate = true;
        		}
        	}
        	
    	
        	setLinks(recordNodes, n, count, baseName, startCount);
        	// thumbnail gets already written in index        	
        	//writeFullsize(iDir, getFilename("image", index), n.getAttributeValue("internalId", nsEpics));
        	String finalSize = null;
        	if (doUpdate) {
        		finalSize = writeFullsize(iDir, globalId, n.getAttributeValue("internalId", nsEpics));
        	} else { 	        		        		
        		finalSize = getImageDimensions(iDir, globalId);	        		
        	}
        	
        	// replace link for thumbnail         	
        	List nl = n.getChildren();
        	for (int i=0; i<nl.size(); i++) {
        		Element indexNode = (Element) nl.get(i);
        		if (indexNode.getAttributeValue("fieldName", nsEpics) != null &&
        				indexNode.getAttributeValue("fieldName", nsEpics).equals("Thumbnail")	) {
        			log.debug("set relative path for thumbnail: "+getRelative(tDir)+"thumb_"+globalId+".jpg");
        			indexNode.setText(getRelative(tDir)+"thumb_"+globalId+".jpg");
        		}
        	}
        	
        	// add link for full size image
        	Element fl = new Element("field", nsEpics);
        	fl.setAttribute("fieldName", "Image", nsEpics);
        	fl.setAttribute("type", "6", nsEpics);
        	fl.setAttribute("size", finalSize, nsEpics);
        	//fl.setText(getRelative(iDir)+getFilename("image",index)+".jpg");
        	fl.setText(getRelative(iDir)+globalId+".jpg");
        	nl.add(fl);
        	
        	n.setAttribute("indexLink", indexLink, nsEpics);
	        	
	        //outputIndexDebug(src);
	        	        	
	        //outputXML(dir, getFilename("record", index), src, (String)getParam("record.stylesheet"));
        	
        	if (doUpdate || doPageUpdate) {
	        	outputXML(dir, globalId, src, (String)getParam("record.stylesheet"));
        		
	        	Document target = transformer.transform(src);
	        	
	        	if (!target.hasRootElement()) {
	        		log.error("Stylesheet error -- transformed record has no valid root element!");
	        	}        	
	        	log.debug("writing result ("+target+") to file "+f);
	        	outputResult(f, target);	        		        	
        	} // do update	
        	        		        		        	
        	// create google image sitemap entry	        	
        	if (imageSitemapOut != null) {
	        		String sitemapEntry = GoogleImageSitemap.getImageSitemapEntry(this, ct, log, globalId, src, (String)getParam("index.sitemapLanguage"));
	        		log.debug(sitemapEntry);
	        		if (!finalSize.equals("unknown")) {
	        			imageSitemapOut.write(sitemapEntry);
	        		} else {
	        			log.warn("Cannot include image in Google Sitemap because image size is unknown. "+
	        					 "Most common reasons: image is too big to be processed, image has strange colorspace, image has errors, cannot access image. "+
	        					 sitemapEntry );        					 
	        		}
        	}        	
        	
        	index++;
        	count++;
        }

        return count;
	}	
	
	private void checkDirectory(String dir) throws IOException {
			
		File dirFile = new File(dir);
		if (!dirFile.exists()) {
			log.debug("created directory "+dirFile);
			dirFile.mkdir();
		}		
	}

	/**
	 * Returns a globally unique name for a given record, suitable for
	 * file naming.
	 * 
	 * @param catalogName
	 * @param recordName
	 * @param internalId
	 */
	protected String getGlobalId(String catalogName, String recordName, String internalId) {
		// internal record id (catalog wide)    	
    	String ids[] = internalId.split(",");
    	String recordId = ids[1];
    	
    	String globalId = catalogName + "_" + recordName + "_" + recordId;  
    	globalId = globalId.replaceAll("\\s+", "_");
    	//globalId.replaceAll("\\.", "_");
    	globalId = globalId.replaceAll("\\\\+", "_");
    	globalId = globalId.replaceAll("/+", "_");
    	//log.debug("-------------> "+globalId);
    	return globalId;
	}
	
	
	/**
	 * Adjust paths to thumbnails in XML.
	 * 
	 * @param doc
	 * @param tDir
	 * @param indexStart
	 * @throws JDOMException
	 * @throws IOException 
	 * @throws CumulusException 
	 */
	private void adjustIndex(Document doc, String tDir, int indexStart, String prevIndex, String nextIndex) throws JDOMException, CumulusException, IOException {
		// replace link for thumbnail in every record        
		XPath xpath = XPath.newInstance("/epics:list");  
        xpath.addNamespace(nsEpics);
        		
		Element list = (Element)xpath.selectSingleNode(doc);
		log.debug("setting index prev/next="+prevIndex+"/"+nextIndex+" on "+list);		
		list.setAttribute("prevIndex", prevIndex, nsEpics);
		list.setAttribute("nextIndex", nextIndex, nsEpics);
		
		
		// replace link for thumbnail in every record        
		xpath = XPath.newInstance("//epics:field[@epics:fieldName='Thumbnail']");  
        xpath.addNamespace(nsEpics);
        
        List fieldList = xpath.selectNodes(doc);        
        int count = 0;
    	for (int i=0; i<fieldList.size(); i++) {
    		Element fieldNode = (Element) fieldList.get(i);

    		// internal record id (catalog wide)
        	String internalId = fieldNode.getParentElement().getAttributeValue("internalId", nsEpics);        	
        	String catalogName = fieldNode.getParentElement().getAttributeValue("catalogName", nsEpics);
        	String recordName = fieldNode.getParentElement().getAttributeValue("recordName", nsEpics);
        	String globalId = getGlobalId(catalogName, recordName, internalId);
        	
    		String value = getRelative(tDir)+"thumb"+"_"+globalId+".jpg";
    		
    		log.debug("writing index thumbnail for cumulus internal id "+fieldNode.getParentElement().getAttributeValue("internalId", nsEpics));
    		writeThumbnail(tDir, "thumb"+"_"+globalId, fieldNode.getParentElement().getAttributeValue("internalId", nsEpics));
    		
    		fieldNode.setText(value);
    		
    		log.debug("set relative path for index thumbnail (indexStart="+indexStart+", count="+count+": "+value);
    		
    		count++;
    	}	
    	
    	// adjust record numbering (for linking to detail record)
    	xpath = XPath.newInstance("//epics:record");  
        xpath.addNamespace(nsEpics);
        List recordList = xpath.selectNodes(doc);
        count = 0;
    	for (int i=0; i<recordList.size(); i++) {
    		Element recordNode = (Element) recordList.get(i);
    		
    		String internalId = recordNode.getAttributeValue("internalId", nsEpics);        	
        	String catalogName = recordNode.getAttributeValue("catalogName", nsEpics);
        	String recordName = recordNode.getAttributeValue("recordName", nsEpics);
        	String globalId = getGlobalId(catalogName, recordName, internalId);
        	
    		String value = globalId+".html";
    		recordNode.setAttribute("localLink", value, nsEpics);
    		
    		log.debug("add local link for record "+globalId+" (indexStart="+indexStart+", count="+count+": "+value);
    		
    		count++;
    	}	
	}
	
	private void copyRelatedFiles(String dir) throws Exception {
		// TODO: add copy from data directory
		
		// copy stylesheets
		File fStyle = new File((String)getParam("record.stylesheet"));
		File fStyleTarget = new File(dir + fStyle.getName());
		if (fStyle.exists() && !fStyle.equals(fStyleTarget)) {
			log.debug("copy "+fStyle+" to "+fStyleTarget);
			JCopy.copyFile(fStyle, fStyleTarget);
		}
		fStyle = new File((String)getParam("index.stylesheet"));
		fStyleTarget = new File(dir + fStyle.getName());
		if (fStyle.exists() && !fStyle.equals(fStyleTarget)) {
			log.debug("copy "+fStyle+" to "+fStyleTarget);
			JCopy.copyFile(fStyle, fStyleTarget);
		}		
	}
	
	/**
	 * Processes a MultiCatalogCollection of images. Creates directories,
	 * copies related files, writes index pages, processes landing pages.
	 *  
	 * @param ct
	 * @param mc
	 * @throws Exception
	 */
	private void processAll(CumulusTools ct, MultiCatalogCollection mc) throws Exception {
		
		int toFinal = mc.countRecords();
		int from = 0;
		
		String viewSet = (String)getParam("record.view");		
		String viewType = "com.canto.recordviewsets.views.InfoWindow";
		int segmentationCount = Integer.parseInt((String)getParam("index.segmentation")); /*CUMULUS_MAX_COLLECTION;*/
		int indexCount = 0;
		this.mc = mc;
		this.ct = ct;
		
		String dir = (String)getParam("index.directory");
		String indexImages = (String)getParam("index.imagedir");
		String recordImages = (String)getParam("record.imagedir");
		log.debug("writing index files to "+dir);
		log.debug("writing index thumbnails to "+indexImages);
		log.debug("writing record images to "+recordImages);
		
		checkDirectory(dir);
		checkDirectory(indexImages);
		checkDirectory(recordImages);
		
		copyRelatedFiles(dir);
		
		boolean compressSitemap = getParam("index.gzipsitemap") != null && ((String)getParam("index.gzipsitemap")).equals("yes");
		OutputStreamWriter imageSitemapOut = null;
		if (getParam("index.writesitemap") != null && ((String)getParam("index.writesitemap")).equals("yes")) {
			log.debug("writing google image sitemap to "+(String)getParam("index.sitemapFile"));
			
			String sitemapName = (String)getParam("index.sitemapFile");
			if (compressSitemap) {
				sitemapName += ".gz";
			}
			File f = new File(sitemapName);
			if (f.exists()) {
				f.delete();
			}
			if (f.createNewFile()) {
				if (compressSitemap) {					
					GZIPOutputStream gzOut = new GZIPOutputStream(new FileOutputStream(f));
					imageSitemapOut = new OutputStreamWriter(gzOut, "UTF-8");
				} else {
					imageSitemapOut = new OutputStreamWriter(new FileOutputStream(f), "UTF-8");
				}
																
				GoogleImageSitemap.writeHeader(imageSitemapOut);
			} else {
				log.error("Cannot create new sitemap file at "+f.getAbsolutePath());				
				throw new Exception("Cannot create new sitemap file at "+f.getAbsolutePath());
			}
		}
		
		
		// global record count (from 0 up to mc.countRecords()-1)
		int recordCount = 0;
		while (from < mc.countRecords()) {
			
			// this index will contain the images [from,to[
			int to = from + segmentationCount - 1;
			if (to > mc.countRecords()) {
				to = mc.countRecords()-1;
			}
			
			// get metadata from cumulus to XML element, create XML document (resultListDoc)
			log.debug("get metadata for set ["+from+","+to+"] with view "+viewSet);			
			Element resultList = ct.getMetadataSet(mc, viewSet, viewType, from, to);
			Document resultListDoc = new Document();
			resultList.detach();
			resultListDoc.addContent(resultList);
						
			// create, transform and write index document
			log.debug("transform index document");
			
			// set index paging
			String nextIndex = "";
			String prevIndex = "";
			if (indexCount > 0) { 
				prevIndex = getFilename("index", (indexCount - 1));
			}
			if (from + segmentationCount < mc.countRecords()) {
				nextIndex = getFilename("index", (indexCount + 1));
			}
			adjustIndex(resultListDoc, indexImages, from, prevIndex, nextIndex);
			
			// output index as XML
			if (((String)getParam("index.writexml")).equals("yes")) {
				outputXML(dir, getFilename("index", indexCount), resultListDoc, (String)getParam("index.stylesheet"));
			}
			
			// output index as HTML
			String idxFile = getFilename("index", indexCount) + ".html";
			log.debug("writing index file "+indexCount+" to "+idxFile);				
			Document result = transformIndex(resultListDoc);
			outputResult(new File(dir + idxFile), result);
			
			// process all records
			String indexLink = getFilename("index", indexCount) + ".html";
			int numRecords = transformRecords(recordCount, indexLink, dir, indexImages, recordImages, "record", resultListDoc, imageSitemapOut);

			// increase record count
			log.debug("- processed "+numRecords+" records");
			recordCount += numRecords;
									
			// increase starting point
			from += numRecords;
			from = to+1;
			
			// index paging count
			indexCount++;
		}
		
		if (imageSitemapOut != null) {
			GoogleImageSitemap.writeFooter(imageSitemapOut);			
			imageSitemapOut.close();
		}
	}
	
	/**
	 * Connects to cumulus server and exports all images for the
	 * given query.
	 * 
	 * @throws Exception
	 */
	public void start() throws Exception {
        
        // XML result document (for outputting metadata)
        Document result = new Document();     
        
        String defaultView = "E-PICS Standard";
        CumulusTools ct = new CumulusTools();
        
        // connect to cumulus server
        MultiCatalogCollection mc = ct.connect(
        		    (String)getParam("cumulus.server"), 
        		    (String)getParam("cumulus.user"), 
        			(String)getParam("cumulus.password"), 
        			(Vector)getParam("cumulus.catalogs"));
        
        HashMap params = new HashMap();
        params.put("sortkey", "Bildcode"); 
        params.put("sortdir", "up"); //down
        
        if (getParam("cumulus.query") != null)
        	params.put("query", getParam("cumulus.query"));      
        
        mc = ct.runQuery(mc, params);
        log.debug("Search result is "+mc.countRecords()+" records");
        
        processAll(ct, mc);
        
        ct.closeCumulus(mc);
	}
	
	
	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		String configPath = (args != null && args.length > 0) ? args[0] : "D:/Eclipse Workspace/E-PICS Google Export/conf/export-template.xml";
		// mode parameter
		String mode = (args != null && args.length > 1) ? args[1] : "create";
		String updateMode = "index";
		
		if (mode.equalsIgnoreCase("update")) {
			
			if (args.length > 2 && args[2].equals("pages")) {
				updateMode = "pages";
			}
			
		}
		XPort xp = new XPort(configPath, mode, updateMode);
		xp.start();
		xp.finalize();
		
		log.debug("quitting XPort.main()");
		System.exit(0);
	}

}
