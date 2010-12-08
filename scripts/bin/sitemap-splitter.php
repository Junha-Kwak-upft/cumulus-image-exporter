<?php
/**
 * E-PICS Google Exporter
 *
 * Splits up google image sitemap and generate new sitemap index from all target
 * sitemaps.
 *
 * Kai Jauslin / 2008-11-10
 *
 * Original code adapted from:
 * http://www.dyasonhat.com/wordpress-plugins/split-large-xml-sitemaps-php-script/
 *
 */

// where sitemap parts get written
define("SITEMAP_TARGET_DIR", "T:/SampleXPort/");

// sitemap index file (will be overwritten!)
define("SITEMAP_INDEX_FILE", "sitemap.xml");
define("SITEMAP_INDEX_DIR", "T:/SampleXPort/");

// source sitemap file (gzipped)
define("SITEMAP_SRC_FILE", "T:/SampleXPort/sitemap-ethbib-bildarchiv.xml.gz");

// root directory for google sitemaps
define("WEB_ROOT", "http://www.e-pics.ethz.ch/index/");

$sitemap_prefix = "sitemap-ethbib-bildarchiv-";

$sitemap_idx = SITEMAP_INDEX_FILE;
$sitemap_idx_dir = SITEMAP_INDEX_DIR;
$sitemap_target = SITEMAP_TARGET_DIR;
$sitemap_src = SITEMAP_SRC_FILE;
$zd = gzopen($sitemap_src, "r");

// read sitemap up to 250MB
$contents = gzread($zd, 100000000);

$file_count = 1;
$url_count = 0;
$url_max_count = 4000;

$last_file_idex = 0;
$index = 0;
$root_path = "http://www.e-pics.ethz.ch/index/";

$url_start = "<url>";
$url_end = "</url>";
$urlset_end = "\n</urlset>";

$sitemap_index_header = '<?xml version="1.0" encoding="UTF-8"?><sitemapindex xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">';
$sitemap_index_end = "\n</sitemapindex>";

$index_file = fopen($sitemap_idx_dir.$sitemap_idx , "w");
fwrite($index_file , $sitemap_index_header);

$xml_header = substr($contents , 0, strpos($contents , $url_start,0));
$index = strpos($contents , $url_start,0);

$sitemap_xml  = $xml_header;
$file = fopen($sitemap_target.$sitemap_prefix.$file_count.".xml" , "wb");
fwrite($file , $sitemap_xml);

while($index < strlen($contents)){
	$pos = strpos($contents , $url_end,$index+1);

	if($pos != false){
		$url_count++ ;
		fwrite($file , substr($contents , $index, $pos - $index + strlen($url_end) ));

	}else{
		break;
	}

	$index = $pos+ strlen($url_end);

	if($url_count >= $url_max_count){

		fwrite($file ,$urlset_end);
		fclose($file);

		$file = fopen($sitemap_target.$sitemap_prefix.$file_count.".xml" , "rb");
		$zp = gzopen($sitemap_idx_dir.$sitemap_prefix.$file_count .".xml.gz", "w9");

		while(!feof($file)){
			gzwrite($zp, fgets($file));
		}

		gzclose($zp);
		fclose($file);
		unlink($sitemap_target.$sitemap_prefix.$file_count.".xml");

		// add sitemap to sitemap index
		$index_str = "<sitemap> \n<loc>".$root_path.$sitemap_prefix.$file_count.".xml.gz</loc>\n";
		$index_str .= "<lastmod>". date(DATE_ATOM) . "</lastmod>\n</sitemap>\n";
		fwrite($index_file , $index_str);

		$file_count++;
		$url_count = 0;

		$file = fopen($sitemap_target.$sitemap_prefix.$file_count.".xml" , "wb");
		fwrite($file , $xml_header);

	}

}

fwrite($file ,$urlset_end);
fclose($file);

$file = fopen($sitemap_target.$sitemap_prefix.$file_count.".xml" , "rb");
$zp = gzopen($sitemap_idx_dir.$sitemap_prefix.$file_count.".xml.gz", "w9");

while(!feof($file)){
	gzwrite($zp, fgets($file));
}

gzclose($zp);
fclose($file);
unlink($sitemap_target.$sitemap_prefix.$file_count.".xml");

// add sitemap to sitemap_index.xml
$index_str = "<sitemap> \n<loc>". $root_path . $sitemap_prefix.$file_count.".xml.gz</loc>\n";
$index_str .= "<lastmod>". date(DATE_ATOM) . "</lastmod>\n</sitemap>";

fwrite($index_file , $index_str);
fwrite($index_file , $sitemap_index_end);
fclose($index_file);

?>
