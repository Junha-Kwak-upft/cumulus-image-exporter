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
	exclude-result-prefixes="xhtml xsl e">
	<!--
    <xsl:output method="html" 
		encoding="UTF-8" 
		doctype-public="-//W3C//DTD XHTML 1.0 Transitional//EN" 
		doctype-system="http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd" 
		indent="no" omit-xml-declaration="yes"/>
	-->
	<xsl:template match="/">
		<html xmlns="http://www.w3.org/1999/xhtml">		
		<head>
		<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
		
		<title>ETH Zürich - ETH-Bibliothek - E-PICS Index - EPICS.Demo</title>
		<link rel="stylesheet" href="epics-default-index.css" />		
	</head>	
	
	<body>
		<div id="centerPage">	
	
		<div class="navigation">
			<xsl:call-template name="navigation" />	
		</div>
	
		<h1>
        	<div style="float:right;">
            	<a href="http://www.ethz.ch/"><img src="eth-logo-wb.png" alt="ETH Zurich" /></a>&nbsp;
                <a href="http://www.library.ethz.ch/"><img src="ethbib-b-white.png" alt="ETH library" /></a>   
            </div>
            
			E-PICS - <xsl:value-of select="e:list/e:record/@e:catalogName" />                        
		</h1>
		<div class="infobox">
			
          <div style="float:right;margin-left:2em;"><a rel="license" href="http://creativecommons.org/licenses/by-nc-nd/2.5/ch/"><img alt="Creative Commons License" style="border-width:0;" src="cc-by-nc-nd.png"  /> </a></div>
            <p>                        
            The images presented on this page are part of E-Pics, a service provided by the ETH-Bibliothek of 
			ETH Zurich. You find here static medium resolution images published under a creative commons license. 
            </p>
            <p>This means that you are free to copy, distribute, display, and perform the work under the following conditions: You must give credit to the original author, you may not use this work for commercial purposes and you may not alter, transform, or build upon this work.
            </p>
            
			<p>Please review the <a href="http://creativecommons.org/licenses/by-nc-nd/2.5/ch/" target="cclicense">license terms</a> before using any of the images provided on these pages. 
			</p>
            
			<ul>
			  <li>More information about E-Pics is available at <a href="http://www.e-pics.ethz.ch/">http://www.e-pics.ethz.ch</a></li>
				<li>This catalog can also be accessed at <a href="http://ba.e-pics.ethz.ch/">http://ba.e-pics.ethz.ch/</a>
               . There you can order the image in a higher resolution or in another format for commercial purposes (<a href="http://ba.e-pics.ethz.ch/ba/files/General_Terms_and_Conditions_Image_Archive_ETHZ_2009.pdf">terms and conditions</a>). 
               </li>
			</ul>
		</div>
		

		<!-- <div class="clearFix"></div> -->
				
		<xsl:apply-templates select="//e:record" />		
		
		<!-- <xsl:call-template name="technicalMetadata"/>	 -->
			
		<div class="footer">				
				
            <div id="copyright">
                &#169; 2008 ETH-Bibliothek, ETH Zürich. <a href="http://ba.e-pics.ethz.ch/default/impressum.jsp">Imprint &amp; Contact</a>.               
            </div>
		</div>
			
	</div>	
	</body>
</html>
	
</xsl:template>
	
	<!-- simple navigation -->
	<xsl:template name="navigation">
	
		<div style="float:right;">
			<xsl:if test="string-length(/e:record/@e:prevIndex)>0">
					<a><xsl:attribute name="href"><xsl:value-of select="/e:record/@e:prevIndex" disable-output-escaping="yes"/>.html</xsl:attribute>&lt; Previous   </a> 
			</xsl:if>		
			<xsl:if test="string-length(/e:list/@e:nextIndex)>0">
					<a><xsl:attribute name="href"><xsl:value-of select="/e:list/@e:nextIndex" disable-output-escaping="yes"/>.html</xsl:attribute>  Next &gt;</a>
			</xsl:if>
		</div>
			<!--	
		<div>
			<xsl:value-of select="/e:record/@e:recordName" />
		</div>
		-->
	</xsl:template>
	
	
	<!-- Technical metadata (footnote) -->
	<xsl:template name="technicalMetadata">
		<div class="note">
			Created <xsl:apply-templates select="e:record/e:field[@e:fieldName='Record Creation Date']"/> | 
			Last modified <xsl:apply-templates select="e:record/e:field[@e:fieldName='Record Modification Date']"/>
		</div>
	</xsl:template>
	

<xsl:template match="e:record/e:field[@e:fieldName='Title']">
	<div class="recordTitle">
		<a><xsl:attribute name="href"><xsl:value-of select="../@e:localLink" disable-output-escaping="yes"></xsl:value-of></xsl:attribute>
		<xsl:if test="string-length(e:content/text())=0">
		<span style="opacity:0.1;">(UNTITLED)</span>
		</xsl:if>
		
		<xsl:value-of select="e:content/child::text()"/>	
		</a>	
	</div>
</xsl:template>
	
<xsl:template match="e:field[@e:fieldName='Thumbnail']">
	<!-- <epics:field epics:id="{af4b2e0b-5f6a-11d2-8f20-0000c0e166dc}" epics:type="0" epics:fieldName="Notes">
		<epics:content>ETH-Bibliothek; Ausstellung: HÃ¶fflichkeit &amp; Bergkgeschrey, Georgius Agricola 1494-1555. 21.11.2005-15.4.2006. Digitalfoto aus HG Gmehrere Werte... für Usability Walkthrough</epics:content>
		</epics:field> -->
	<div class="thumbnail"><a><xsl:attribute name="href"><xsl:value-of select="../@e:localLink" disable-output-escaping="yes"/></xsl:attribute><img><xsl:attribute name="src"><xsl:value-of select="text()"/></xsl:attribute><xsl:attribute name="alt"><xsl:value-of select="normalize-space(../e:field[@e:fieldName='Title'])" /></xsl:attribute><xsl:attribute name="longdesc"><xsl:value-of select="../e:field[@e:fieldName='Notes']" /></xsl:attribute></img></a></div>
</xsl:template>

	<!--	
<xsl:template match="e:field[@e:fieldName='Record Name']">
	xxx<div class="recordHead"><xsl:value-of select="content" /></div>
</xsl:template>
	
-->
<xsl:template match="e:content">
	<xsl:value-of select="." />
</xsl:template>

<xsl:template match="e:record">
	<div class="record clearfix">
		<xsl:apply-templates select="e:field[@e:fieldName='Thumbnail']" />
				
		<xsl:apply-templates select="e:field[@e:fieldName='Title']" />	
		<xsl:apply-templates select="e:field[@e:fieldName='Record Name']" />	
		
		<xsl:apply-templates select="e:field[@e:fieldName='Caption']" />
		<xsl:apply-templates select="e:field[@e:fieldName='Creator']" />		
		<!-- <xsl:apply-templates select="e:field[@e:fieldName='Credits']" /> -->
		<xsl:apply-templates select="e:field[@e:fieldName='Copyright Notice']" />		
		<!-- <xsl:apply-templates select="e:field[@e:fieldName='Record Modification Date']" />  -->
						
	</div>
	
</xsl:template>

<xsl:template match="e:field">
	<xsl:if test="string-length(e:content) > 0">
		<div class="fieldLine">
			<div class="fieldName">
				<xsl:apply-templates select="@e:fieldName"/>			
			</div>
			<div class="fieldValue">
				<xsl:if test="count(e:content/e:category)>0">
					
					<xsl:for-each select="e:content/e:category">
						<div class="categoryDisplay"><xsl:value-of select="." /></div>
					</xsl:for-each>
				</xsl:if>
				
				<xsl:if test="count(e:content/e:category)=0">
					<xsl:apply-templates select="e:content" />
				</xsl:if>
			</div>
		</div>
		
	</xsl:if>
</xsl:template>


</xsl:stylesheet>