<?xml version="1.0" encoding="utf-8"?>
<!DOCTYPE xsl:stylesheet [
<!ENTITY nbsp "&#160;">
<!ENTITY copy "&#169;">
<!ENTITY uuml "ü">
]>
<xsl:stylesheet version="1.0" 
	xmlns:xhtml="http://www.w3.org/1999/xhtml"	
	xmlns:e="http://www.e-pics.ethz.ch/ns/integration" 
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:xslt="http://www.w3.org/1999/XSL/Transform"
	exclude-result-prefixes="xhtml xsl e">
	<!--
	<xsl:output method="html" 
		encoding="UTF-8" 
		doctype-public="-//W3C//DTD XHTML 1.0 Transitional//EN" 
		doctype-system="http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd" 
		indent="no" omit-xml-declaration="yes"/>
	-->
    
	<!-- <xsl:include href="url-encode.xsl" /> -->
	
    
    <!-- main html page output -->
	<xsl:template match="/">    	
		<html xmlns="http://www.w3.org/1999/xhtml">		
		<head>
			<title>ETH Zürich - ETH-Bibliothek - E-PICS - <xsl:value-of select="/e:record/@e:catalogName" /> - <xsl:value-of select="/e:record/@e:recordName" /></title>
			<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
			<link rel="stylesheet" href="epics-default-record.css" />
		</head>	
	
		<body>
			<div id="centered">	
		        
        	<!-- logos and navigation header -->
            <div class="clearfix" id="header">
                <a href="http://www.ethz.ch/"><img src="eth-logo-wb.png" alt="ETH Zurich" /></a>&nbsp;
                <a href="http://www.ethbib.ethz.ch/"><img src="ethbib-b-white.png" alt="ETH library" /></a>                      
    
                <div id="navigation">
                	<xsl:value-of select="/e:record/@e:catalogName" /><br />
                    
                    <xsl:if test="string-length(/e:record/@e:prevLink)>0">
                        <a><xsl:attribute name="href"><xsl:value-of select="/e:record/@e:prevLink" disable-output-escaping="yes"/>.html</xsl:attribute>&lt; Previous</a> | 
                    </xsl:if>		
                    			
                    <xsl:if test="string-length(/e:record/@e:indexLink)>0">
                        <a><xsl:attribute name="href"><xsl:value-of select="/e:record/@e:indexLink" /></xsl:attribute>Index</a>
                    </xsl:if>
                    	
                    <xsl:if test="string-length(/e:record/@e:nextLink)>0">
                        | <a><xsl:attribute name="href"><xsl:value-of select="/e:record/@e:nextLink" disable-output-escaping="yes"/>.html</xsl:attribute>Next &gt;</a>
                    </xsl:if>
                </div>         	                           
            </div>
            
            <!-- card tab -->
			<div class="tinytab"><span><xsl:value-of select="/e:record/@e:recordName" /></span></div>
		
        	<!-- card sheet -->
            <div id="sheet-box">
                <div id="sheet"><div id="inner-sheet">
                
                <!-- image title field -->
                <h1><xsl:apply-templates select="e:record/e:field[@e:fieldName='Title']"/></h1>
                
                
                <div id="image-frame">        
                    <a>
                    	<xsl:attribute name="href">http://ba.e-pics.ethz.ch/link.jsp?id=<xsl:value-of select="/e:record/@e:recordName" /></xsl:attribute>
                        <img>
                        	<!-- caption -->
                            <xsl:attribute name="alt"><xsl:value-of select="/e:record/e:field[@e:id='{af4b2e34-5f6a-11d2-8f20-0000c0e166dc}']"/></xsl:attribute>
                        	<!-- link to cc image -->
                        	<xsl:attribute name="src"><xsl:value-of select="/e:record/e:field[@e:fieldName='Image']"/></xsl:attribute>
                            <!-- title -->
                            <xsl:attribute name="alt"><xsl:apply-templates select="e:record/e:field[@e:fieldName='Title']"/></xsl:attribute>
                        </img>
                    </a>
                </div>
		        		                
                <table class="image-metadata">                    
                    <colgroup>
                        <col width="25%" />
                        <col width="*" />
                    </colgroup>
            
                    <caption>
                    	<p>Metadata for image <xsl:value-of select="/e:record/@e:recordName" /></p>
                    </caption>
                                
                    <xsl:apply-templates select="//e:field[not(@e:fieldName='Title') and not(@e:fieldName='Image') and not(@e:fieldName='xategories') and not(@e:fieldName='Thumbnail Width') and not(@e:fieldName='Thumbnail Height')]" />
                </table>
                                			
		
        		<!-- copyright notice -->
                <div class="infobox">
                    <div style="float:right;margin-top:0.5em;">
                        <a rel="license" href="http://creativecommons.org/licenses/by-nc-nd/2.5/ch/">
                        	<img alt="Creative Commons License" style="border-width:0;" src="cc-by-nc-nd.png" />
                        </a>
                    </div>
                    
                    <p>The image presented on this page is part of E-Pics, a service provided by the ETH-Bibliothek of 
                    ETH Zurich. This medium resolution copy is published under a creative commons license.</p>
                    
                    <p>
                    Please review and accept the
                    <a href="http://creativecommons.org/licenses/by-nc-nd/2.5/ch/" target="cclicense">license terms</a>
                    before using this image.
                    </p>
                    
                    <ul>
                        <li>The original copy of this image is available at 
                            <a>
                                <xsl:attribute name="href">http://ba.e-pics.ethz.ch/link.jsp?id=<xsl:value-of select="/e:record/@e:recordName" /></xsl:attribute>
                                http://ba.e-pics.ethz.ch/link.jsp?id=<xsl:value-of select="/e:record/@e:recordName" />
                            </a>. There you can order the image in a higher resolution or in another format for commercial purposes (<a href="http://www.ethbib.ethz.ch/bildarchiv/geschaeftsbed_e.pdf">terms and conditions</a>).
                        </li>
                        <li>
                            The ETH image archive can be accessed at 
                            <a href="http://ba.e-pics.ethz.ch">http://ba.e-pics.ethz.ch</a>
                        </li>
                        <li>
                            More information about E-Pics is available at
                            <a href="http://www.e-pics.ethz.ch/">http://www.e-pics.ethz.ch</a>
                        </li>                    
                    </ul>                    
				</div>
               

                </div><!-- inner-sheet -->
                </div><!-- sheet -->		
		
            </div><!-- sheet-box -->
            
            <div id="footer">
            	&copy; 2008 
                <a href="http://www.ethbib.ethz.ch">ETH-Bibliothek</a>, <a href="http://www.ethz.ch">ETH Z&uuml;rich</a>. 
                <a href="http://ba.e-pics.ethz.ch/default/impressum.jsp">Imprint &amp; Contact</a>.
            </div>
        </div><!-- centered -->
				
	</body>
	</html>	
	</xsl:template>
	
    
    
	<xsl:template name="getLink">http://www.e-pics.ethz.ch/link?catalog=<xsl:value-of select="/e:record/@e:catalogNameURL"/>&amp;id=<xsl:value-of select="/e:record/@e:recordNameURL"/></xsl:template>
	
	<!-- simple navigation -->
	<xsl:template name="navigation">
	
		<div style="float:right;">
			<xsl:if test="string-length(/e:record/@e:prevLink)>0">
					<a><xsl:attribute name="href"><xsl:value-of select="/e:record/@e:prevLink" disable-output-escaping="yes"/>.html</xsl:attribute>&lt; Previous   </a> 
			</xsl:if>		
			<xsl:if test="string-length(/e:record/@e:indexLink)>0">
					<a><xsl:attribute name="href"><xsl:value-of select="/e:record/@e:indexLink" /></xsl:attribute>Index</a> 
			</xsl:if>				
			<xsl:if test="string-length(/e:record/@e:nextLink)>0">
					<a><xsl:attribute name="href"><xsl:value-of select="/e:record/@e:nextLink" disable-output-escaping="yes"/>.html</xsl:attribute>  Next &gt;</a>
			</xsl:if>
		</div>			
		
	</xsl:template>
	
	
	<!-- technical metadata (footnote) -->
	<xsl:template name="technicalMetadata">
		<div class="note">
			Created <xsl:apply-templates select="e:record/e:field[@e:fieldName='Record Creation Date']"/>
			| Last modified <xsl:apply-templates select="e:record/e:field[@e:fieldName='Record Modification Date']"/>
		</div>
	</xsl:template>
	
	
	<!-- generic field content (text) -->
    <xsl:template match="e:content">
        <xsl:value-of select="." />
    </xsl:template>


	<!-- rendering for a single field (table row) -->
    <xsl:template match="//e:field">
        <xsl:if test="string-length(e:content) > 0">
            <tr>
                <th scope="row">
                    <xsl:apply-templates select="@e:fieldName"/>	
                </th>
                <td>
                    <xsl:if test="count(e:content/e:category)>0">
                        <ul>
                        <xsl:for-each select="e:content/e:category">
                            <li><xsl:value-of select="." /></li>
                        </xsl:for-each>
                        </ul>
                    </xsl:if>
                    
                    <xsl:if test="count(e:content/e:category)=0">
                        <xsl:apply-templates select="e:content" />
                    </xsl:if>
                </td>
            </tr>
        </xsl:if>
    </xsl:template>


	<!-- record title -->
    <xsl:template match="e:field[@e:fieldName='Title']">	
            <xsl:if test="string-length(e:content/text())=0">
                <span style="opacity:0.1;">(UNTITLED)</span>
            </xsl:if>
            
            <xsl:value-of select="e:content/child::text()"/>		
    </xsl:template>


</xsl:stylesheet>