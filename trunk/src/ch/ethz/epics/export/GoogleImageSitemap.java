package ch.ethz.epics.export;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.jdom.xpath.XPath;

import ch.ethz.epics.integration.cumulus.CumulusTools;

public class GoogleImageSitemap {
	
	// Google image sitemap namespaces
	private final static String NS_SITEMAP = "http://www.google.com/schemas/sitemap/0.9";
	private final static String NS_IMAGESITEMAP = "http://www.google.com/schemas/sitemap-images/1.0";

	public final static Namespace nsSitemap = Namespace.getNamespace("", NS_SITEMAP);
	public final static Namespace nsImageSitemap = Namespace.getNamespace("image", NS_SITEMAP);
	
	protected static Element getSingleNode(Document doc, String strXPath) throws JDOMException {
		XPath xpath = XPath.newInstance(strXPath); 
		xpath.addNamespace(XPort.nsEpics);
		
		List elementList = xpath.selectNodes(doc);
		
		if (elementList.size() > 0) {
			return (Element)(elementList.iterator().next());
		} else {
			return null;
		}
	}
	
	public static String getImageSitemapEntry(XPort xport, CumulusTools ct, Log log, String globalId, Document doc, String lang) throws JDOMException, IOException {
		
        
        Element recordNode = getSingleNode(doc, "//epics:record");                                       
        String recName = recordNode.getAttributeValue("recordName", XPort.nsEpics);
        	
        log.debug("- image sitemap building for internal id='"+recName+"'");                
        Document sitemap = new Document();
        
        // urlset sitemap root
        Element urlset = new Element("urlset", nsSitemap);
        urlset.addNamespaceDeclaration(nsImageSitemap);
        sitemap.setRootElement(urlset);
        
        Element url = new Element("url", nsSitemap);
        urlset.addContent(url);
        
        // <loc> tag specifies the URL for an original image on your site
        Element c = new Element("loc", nsSitemap);
        String baseUrl = (String)xport.getParam("index.baseUrl");
        c.setText(baseUrl + "images/" + globalId + ".jpg");        
        url.addContent(c);
        
        /** 
         * The <priority> value specifies the importance of a particular image relative to 
         * other images on the same site
         */
        c = new Element("priority", nsSitemap);
        c.setText("0.5");        
        url.addContent(c);
        
        /**
         * The <lastmod> value identifies the time that the content at the URL was last modified. We recommend 
         * you set this value to the most recent time that the image last changed. This information enables 
         * crawlers to avoid recrawling content that has not changed.
         */
        c = new Element("lastmod", nsSitemap);
        // uses e-pics record modification date
        Element recMod = getSingleNode(doc, "//epics:field[@epics:id='{af4b2e02-5f6a-11d2-8f20-0000c0e166dc}']");        
        String strDate = recMod.getChildTextNormalize("content", XPort.nsEpics);
        
        SimpleDateFormat df = ct.getDefaultDateFormat();
        java.util.Date dtRecMod;
		try {
			dtRecMod = df.parse(strDate);
			SimpleDateFormat w3c = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
	        strDate = w3c.format(dtRecMod);
	        c.setText(strDate);
	        url.addContent(c);
		} catch (ParseException e1) {
			// cannot parse date: log warning, leave away lastmod
			log.warn("can't parse record modification date for "+globalId);
		}
        
        /** 
         * The <changefreq> value indicates how frequently the content at a particular URL is likely to 
         * change. We recommend you set this value to indicate how frequently the image changes. 
         */
        c = new Element("changefreq", nsSitemap);
        c.setText("monthly");        
        url.addContent(c);       
       
        /** 
         * The <expires> tag identifies the time that the content expires. The value of the <expires> tag 
         * should be a timestamp in W3C DATETIME format.
         */
        // e-pics: add 2 years to now
        c = new Element("expires", nsSitemap);
        SimpleDateFormat w3c = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");        
        Calendar cNow = Calendar.getInstance();
        cNow.add(Calendar.YEAR, 2);
        Date dt = cNow.getTime();
        strDate = w3c.format(dt);
        c.setText(strDate);           
        url.addContent(c); 
        
        // image:image
        Element imageTag = new Element("image", nsImageSitemap);
        url.addContent(imageTag);
        
        // image:landing_page
        Element cLandingPage = new Element("landing_page", nsImageSitemap);
        imageTag.addContent(cLandingPage);
        
        /**
         * The <image:loc> tag identifies the URL where the user will be directed when clicking on the image
         * from Google search results. Please note that this value must be properly encoded.
         */
        c = new Element("loc", nsImageSitemap);
        baseUrl = (String)xport.getParam("index.baseUrl");
        c.setText(baseUrl + globalId + ".html");                
        cLandingPage.addContent(c);    
        
        /**
         * The <image:language> tag identifies the language for each landing page defined by <image:landing_page>
         */
        // e-pics: primarily german content
        c = new Element("language", nsImageSitemap);
        c.setText(lang);                
        cLandingPage.addContent(c);   
        
        /**
         * The <image:title> tag specifies the title of the image. There should be a maximum of one title 
         * per landing page.
         */
        Element e = getSingleNode(doc, "//epics:field[@epics:id='{af4b2e3d-5f6a-11d2-8f20-0000c0e166dc}']");
        String title = e.getChildText("content", XPort.nsEpics);
        if (title != null && title.length() > 0) {
	        c = new Element("title", nsImageSitemap);
	        c.setText(title);                
	        cLandingPage.addContent(c);   
        }
        
        /**
         * The <image:caption> tag's value provides context for an image. Please note that this value must be XMLencoded.
		 * There should be a maximum of one caption per landing page.
         */
        e = getSingleNode(doc, "//epics:field[@epics:id='{af4b2e34-5f6a-11d2-8f20-0000c0e166dc}']");
        String caption = e.getChildText("content", XPort.nsEpics);
        if (caption != null && caption.length() > 0) {
	        c = new Element("caption", nsImageSitemap);
	        c.setText(caption);                
	        cLandingPage.addContent(c);   
        }
        
        /**
         * The <image:category> tag's value specifies one or more groups, subjects or categories that 
         * describe the image. Multiple categories should be included in separate category tags. 
         * Comma-separated values within a single category tag will be considered one single category.
         */      
        //ArrayList<String> keywords = new ArrayList<String>();
        HashSet<String> keywords = new HashSet<String>();
        XPath xpath = XPath.newInstance("//epics:category");
        xpath.addNamespace(XPort.nsEpics);
        List catNodes = xpath.selectNodes(doc);
        Iterator it = catNodes.iterator();
        String locationName = "";
        while (it.hasNext()) {
        	e = (Element)it.next();
        	String catName = e.getTextNormalize();        	
        	
        	log.debug("catName: "+catName);
        	
        	
        	// KJ/20081203: use all keywords
        	/* last keyword:
        	String kw = catName.replaceAll(".* >? (.*)", "$1");
        	log.debug("got keyword: "+kw);
        	if (kw != null && kw.length() > 0) {
    			keywords.addAll(Arrays.asList(kw.split(", ")));
    		}
        	*/
        	/*
        	if (catName != null && catName.length() > 0) {
        		keywords.addAll(Arrays.asList(catName.split("> ")));
        		log.debug("got keywords: "+keywords);
        	}
        	*/
        	
        	// add keywords with duplicates removed
        	if (catName != null && catName.length() > 0) {
        		
        		List<String> kw = Arrays.asList(catName.split(">"));        		
        		        		
        		for (String k: kw) {
        			
        			if (!keywords.contains(k) && k.trim().length() > 1) {
        				log.debug("got keyword: "+k);
        				keywords.add(k.trim());
        			}
        		}
        		        		
        	}
        	
        	String nCatName = catName.replaceAll("\\s*>", ", ");        	
        	if (nCatName != null && nCatName.length() > 0) {
        		// KJ/20081203: don't add categories - use keywords instead
        		
	        	//c = new Element("category", nsImageSitemap);
	            //c.setText(nCatName);                
	            //cLandingPage.addContent(c);   
	            	            
	            // E-Pics ETHBIB.Bildarchiv *spezial*
	            if (catName.contains("Politische Geographie")) {
	            	log.debug("found location = "+catName);
	            	
	            	catName = catName.replaceAll(", Kanton > ", ", ");
	            	catName = catName.replaceAll(", Stadt > ", ", ");
	            		            	
	            	locationName = catName.replaceAll(".*?Politische Geographie\\s*>*(.*)", "$1");
	            	
	            	log.debug("reduced location to = "+locationName);
	            	locationName = locationName.replaceAll("\\s*>", ",");   
	            	log.debug("reduced location to = "+locationName);
	            }	            		            
        	}
        	
        	/*
        	if (nCatName.contains("Sachkatalog")) {
        		String kwlist = catName.replaceAll(".*?Sachkatalog\\s*>*(.*)", "$1");
        		if (kwlist != null && kwlist.length() > 0) {
        			keywords.addAll(Arrays.asList(kwlist.split(", ")));
        		}
        	}
        	*/
        	        	
        }
        
        /**
         * The <image:keyword> tag contains a single keyword that describes an image. By properly tagging 
         * images, you will help us to rank them in the Google Image Index. Please provide keywords that 
         * are as specific and descriptive as possible. Broad keywords may or may not be used in indexing. 
         * Keywords should be included in separate keyword tags, and comma-separated values within a single 
         * keyword tag will be considered one single keyword.
         */
        it = keywords.iterator();
        while (it.hasNext()) {
        	String kw = (String)it.next();        	
        	
        	c = new Element("keyword", nsImageSitemap);
            c.setText(kw);                
            cLandingPage.addContent(c); 
        }
        
        
        
        /**
         * The <image:family_friendly> tag's value indicates whether the image only contains content that 
         * is suitable for children. Acceptable values for this tag are yes and no. Please use reasonable 
         * judgment when determining values for this tag. One way to define family-friendly is whether 
         * the image could appear in a G-rated movie.
         */        
        c = new Element("family_friendly", nsImageSitemap);
        //TODO
        //e = (Element)xpath.selectSingleNode("//epics:field[@epics:id='{af4b2e34-5f6a-11d2-8f20-0000c0e166dc}']");
        //String familyFriendly = e.getChildText("epics:content");
        c.setText("yes");                
        imageTag.addContent(c);   
        
        /**
         * The <image:geo_location> tag is used to specify a geographical location. This can be a string 
         * the form of an address, city name, or latitude and longitude pair. Please note that this value 
         * must be XML-encoded.
         */
        c = new Element("geo_location", nsImageSitemap);
        if (locationName != null) {        
        	c.setText(locationName);                
        	imageTag.addContent(c);        
        }
        
        /**
         * The <image:geo_location> tag is used to specify a geographical location. This can be a string 
         * the form of an address, city name, or latitude and longitude pair. Please note that this value 
         * must be XML-encoded.
         */
        
        // E-Pics: Creative Commons Namensnennung, Non-commercial, no derivatives
        c = new Element("license", nsImageSitemap);
        c.setText("http://creativecommons.org/licenses/by-nc-nd/2.5/ch/");                
        imageTag.addContent(c);            
 
        /**
         * The <image:quality> tag's value specifies the quality of the image relative to other images. 
         * This information may be used to rank images from the same site relative to one another on 
         * search result pages. Unlike <priority>, it is not used to prioritize images indexed by Google.
         */        
        // E-Pics: not used
        //c = new Element("quality", nsImageSitemap);
        //c.setText("1.0");                
        //imageTag.addContent(c);            
       
        
        /**
         * The <image:publication_date> tag identifies the original publication date of the image in YYYY-MM-DD format.
		 * The value of the <image:publication_date> tag should be a timestamp in W3C DATETIME format.
         */        
        // E-Pics: EXIF Date created -or- TODO: Date field
        c = new Element("publication_date", nsImageSitemap);
        Element pubDate = getSingleNode(doc, "//epics:field[@epics:id='{af4b2e51-5f6a-11d2-8f20-0000c0e166dc}']");
        if (pubDate != null) {
        	strDate = pubDate.getChildTextNormalize("content", XPort.nsEpics);   
        } else {
        	// try to get date field
        	Element dateField = getSingleNode(doc, "//epics:field[@epics:id='{132267c2-4148-4b76-b851-88409d7d2799}']");
        	if (dateField != null) {
        		strDate = dateField.getChildTextNormalize("content", XPort.nsEpics);   
        		if (strDate != null && strDate.length() > 0) {	        		
	        		if (!strDate.contains(".") && !strDate.contains("/")) {
	        			// year only
	        			strDate = "01.01."+strDate; //+" 00:00:00 CET";
	        		} else 
	        		if (strDate.contains("/") && strDate.length() > 0) {
	        			// mm/YYYY
	        			String[] dateDetails = strDate.split("\\/");
	        			if (dateDetails.length == 2) {
		        			strDate = "01." + dateDetails[0] + "." + dateDetails[1];
		        			//strDate += " 00:00:00 CET";
	        			}
	        		} else {
	        			//strDate = strDate+" 00:00:00 CET";
	        		}
	        		log.debug("converted dateonly value = "+strDate);
        		}
        	}
        }
        if (strDate != null && strDate.length() > 0) {
	        df = ct.getDefaultDateFormat();
	        java.util.Date dtPubDate;
			try {
				dtPubDate = df.parse(strDate);
				w3c = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		        strDate = w3c.format(dtPubDate);
		        c.setText(strDate);
		        
		        // KJ: 20090619 - Google does not like publication dates < 1970
		        // therefore they are removed from the sitemap
		        //imageTag.addContent(c);
		        
			} catch (ParseException e1) {
				// cannot parse date: log warning, leave away lastmod
				log.warn("can't parse publication date for "+globalId, e1);
			}      
        }
        
        
        /**
         * The <image:size> tag specifies the size of the image in pixels. The images that you make 
         * available to Google's crawlers should be the same size as the images that you display to 
         * users on your site.
         */        
        // E-Pics: will need size of derivative image
        Element mediumImage = getSingleNode(doc, "//epics:field[@epics:fieldName='Image']"); 
        String finalSize = mediumImage.getAttributeValue("size", XPort.nsEpics);
        if (finalSize != null) {
        	c = new Element("size", nsImageSitemap);
            c.setText(finalSize);                
            imageTag.addContent(c);        	
        }
        
                
        /**
         * The <image:watermarking> tag's value indicates whether watermarking exists on the image. The 
         * only valid values of this tag are yes and no. If the value is yes, then you have the option 
         * of specifying the percentage attribute:
         */          
        // E-Pics: maximum of 5% for creative commons license
        c = new Element("watermarking", nsImageSitemap);
        c.setText("yes");     
        c.setAttribute("percentage", "5");
        imageTag.addContent(c);            
       
        
        
        // write XML fragment into string
        StringWriter sw = new StringWriter();
        Format xmlFormatDebug = Format.getPrettyFormat().setOmitDeclaration(true);
        xmlFormatDebug.setEncoding("UTF-8");
        XMLOutputter xmlOutput = new XMLOutputter(xmlFormatDebug); 
        Element sitemapRoot = sitemap.getRootElement();
        xmlOutput.output((Element)sitemapRoot.getContent(0), sw);
        
        String part = sw.toString();
        part = part.replaceAll(" xmlns=\"http://www.google.com/schemas/sitemap/0.9\"", "");
        part = part.replaceAll(" xmlns\\:image=\"http://www.google.com/schemas/sitemap/0.9\"", "");
        
        return part;
	}
	
	/**
	 * Creates a new google image sitemap. 
	 * @return
	 * @throws Exception 
	 */
	public static void writeHeader(OutputStreamWriter out) throws Exception {
		String header = "<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n";
		header += "<urlset xmlns=\"http://www.google.com/schemas/sitemap/0.9\"\n";
		header += "        xmlns:image=\"http://www.google.com/schemas/sitemap-images/1.0\">\n";
		
		out.write(header);							
	}
	
	/**
	 * Writes footer and closes sitemap file.
	 */
	public static void writeFooter(OutputStreamWriter out) throws Exception {	
		String footer = "\n</urlset>";		
		out.write(footer);
		out.flush();		
	}
	
}
