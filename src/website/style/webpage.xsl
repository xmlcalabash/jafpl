<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
		xmlns="http://www.w3.org/1999/xhtml"
		xmlns:h="http://www.w3.org/1999/xhtml"
		xmlns:atom="http://www.w3.org/2005/Atom"
                xmlns:db="http://docbook.org/ns/docbook"
		xmlns:dc="http://purl.org/dc/elements/1.1/"
                xmlns:f="http://docbook.org/xslt/ns/extension"
                xmlns:m="http://docbook.org/xslt/ns/mode"
                xmlns:r="http://nwalsh.com/ns/git-repo-info"
		xmlns:t="http://docbook.org/xslt/ns/template"
                xmlns:xlink="http://www.w3.org/1999/xlink"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                exclude-result-prefixes="atom h db dc f m r t xlink xs"
		version="2.0">

<xsl:import href="../../../build/docbook/xslt/base/html/final-pass.xsl"/>

<xsl:output name="final"
	    method="xhtml"
	    doctype-public="-//W3C//DTD XHTML 1.0 Transitional//EN"
	    doctype-system="http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd"/>

<xsl:param name="autolabel.elements">
  <db:appendix format="A"/>
  <db:chapter/>
</xsl:param>

<xsl:param name="resource.root"
           select="concat('http://cdn.docbook.org/release/',$VERSION,'/resources/')"/>

<xsl:param name="linenumbering" as="element()*">
<ln path="literallayout" everyNth="0"/>
<ln path="programlisting" everyNth="0"/>
<ln path="programlistingco" everyNth="0"/>
<ln path="screen" everyNth="0"/>
<ln path="synopsis" everyNth="0"/>
<ln path="address" everyNth="0"/>
<ln path="epigraph/literallayout" everyNth="0"/>
</xsl:param>

<!-- ============================================================ -->

<xsl:variable name="sitemenu" select="document('../etc/menu.xml')/*"
	      as="element()"/>

<xsl:variable name="gitlog" select="document('../etc/git-log-summary.xml')/*"
	      as="element()"/>

<!-- ============================================================ -->

<xsl:template match="*" mode="m:css">
  <xsl:param name="node" select="."/>

  <link rel="stylesheet" type="text/css" href="/css/docbook.css"/>
  <link rel="stylesheet" type="text/css" href="/css/tabs.css" />
  <link rel="stylesheet" type="text/css" href="/css/website.css" />
  <link href='http://fonts.googleapis.com/css?family=Rambla'
        rel='stylesheet' type='text/css' />
<!--
  <link rel="icon" href="/graphics/icon.ico" type="image/ico"/>
-->
</xsl:template>

<xsl:template match="*" mode="m:javascript-head">
  <xsl:param name="node" select="."/>

  <xsl:if test="/*/@xml:id = 'home'">
    <script src="https://code.jquery.com/jquery-3.2.1.min.js"
            integrity="sha256-hwg4gsxgFZhOsEEamdOYGBf13FyQuiTwlAQgxVSNgt4="
            crossorigin="anonymous"></script>
    <script src="/js/jafpl.js"></script>
  </xsl:if>
</xsl:template>

<!-- ============================================================ -->

<xsl:template match="db:article[@xml:id]">
  <xsl:variable name="header" select="doc('../include/header.html')"/>
  <xsl:apply-templates select="$header" mode="to-xhtml"/>

  <xsl:if test="not($sitemenu//h:li[@id = current()/@xml:id])">
    <xsl:message terminate="yes">
      <xsl:text>Error: page is not in the menu: </xsl:text>
      <xsl:value-of select="@xml:id"/>
    </xsl:message>
  </xsl:if>

  <xsl:if test="not($sitemenu//h:li[@id = current()/@xml:id])">
    <xsl:message terminate="yes">
      <xsl:text>Error: page is not in the menu: </xsl:text>
      <xsl:value-of select="@xml:id"/>
    </xsl:message>
  </xsl:if>

  <xsl:variable name="menu"
                select="concat('../../../build/menus/', @xml:id, '.html')"/>

  <xsl:apply-templates select="doc($menu)" mode="to-xhtml"/>

  <article class="{local-name(.)}">
    <h1>
      <!-- HACK! -->
      <xsl:choose>
	<xsl:when test="@xml:id = 'home'">
	  <xsl:text>Just another </xsl:text>
          <em id="fine">fine</em>
          <xsl:text> pipeline language</xsl:text>
          <a href="https://github.com/ndw/jafpl" class="github-corner">
<svg xmlns="http://www.w3.org/2000/svg" width="80" height="80"
viewBox="0 0 250 250" style="fill:#151513; color:#fff; position: absolute;
top: 0; border: 0; right: 0;" aria-hidden="true"><path d="M0,0 L115,115 L130,115
L142,142 L250,250 L250,0 Z"></path><path d="M128.3,109.0 C113.8,99.7 119.0,89.6
119.0,89.6 C122.0,82.7 120.5,78.6 120.5,78.6 C119.2,72.0 123.4,76.3 123.4,76.3
C127.3,80.9 125.5,87.3 125.5,87.3 C122.9,97.6 130.6,101.9 134.4,103.2"
fill="currentColor" style="transform-origin: 130px 106px;"
class="octo-arm"></path><path d="M115.0,115.0 C114.9,115.1 118.7,116.5 119.8,115.4
L133.7,101.6 C136.9,99.2 139.9,98.4 142.2,98.6 C133.8,88.0 127.5,74.4 143.8,58.0
C148.5,53.4 154.0,51.2 159.7,51.0 C160.3,49.4 163.2,43.6 171.4,40.1 C171.4,40.1
176.1,42.5 178.8,56.2 C183.1,58.6 187.2,61.8 190.9,65.4 C194.5,69.0 197.7,73.2
200.1,77.6 C213.8,80.2 216.3,84.9 216.3,84.9 C212.7,93.1 206.9,96.0 205.4,96.6
C205.1,102.4 203.0,107.8 198.3,112.5 C181.9,128.9 168.3,122.5 157.7,114.1
C157.9,116.9 156.7,120.9 152.7,124.9 L141.0,136.5 C139.8,137.7 141.6,141.9
141.8,141.8 Z" fill="currentColor" class="octo-body"></path></svg></a>
	</xsl:when>
	<xsl:otherwise>
	  <xsl:apply-templates select="db:info/db:title" mode="titlepage"/>
	</xsl:otherwise>
      </xsl:choose>
    </h1>

    <xsl:apply-templates/>

    <xsl:call-template name="t:process-footnotes"/>

    <footer>
      <xsl:variable name="gitfn" select="substring-after(base-uri(/), $gitlog/@root)"/>
      <xsl:variable name="commit" select="($gitlog/r:commit[r:file = $gitfn])[1]"/>
      <xsl:variable name="cdate" select="$commit/r:date"/>
      <xsl:variable name="committer" select="substring-before($commit/r:committer, ' &lt;')"/>

      <p>Copyright © 2016 Norman Walsh. See
      <a href="https://github.com/ndw/jafpl/blob/master/LICENSE.md">LICENSE</a>.
      <xsl:if test="exists($cdate)">
        <xsl:variable name="date" select="$cdate cast as xs:dateTime"/>
        <xsl:text>Last updated on </xsl:text>
        <xsl:value-of select="format-dateTime($date, '[D01] [MNn,*-3] [Y0001]')"/>
        <xsl:text> at </xsl:text>
        <xsl:value-of select="format-dateTime($date, '[h01]:[m01][P] [z]')"/>
        <xsl:text> by </xsl:text>
        <xsl:value-of select="$committer"/>
      </xsl:if>
      </p>
    </footer>
  </article>
</xsl:template>

<xsl:template match="element()" mode="to-xhtml">
  <xsl:element name="{local-name(.)}" namespace="http://www.w3.org/1999/xhtml">
    <xsl:apply-templates select="@*,node()" mode="to-xhtml"/>
  </xsl:element>
</xsl:template>

<xsl:template match="attribute()|text()|comment()|processing-instruction()" mode="to-xhtml">
  <xsl:copy/>
</xsl:template>

<!-- ============================================================ -->

<!-- complete and total f'ing hack -->
<xsl:function name="f:mediaobject-href" as="xs:string">
  <xsl:param name="filename" as="xs:string"/>
  <xsl:choose>
    <xsl:when test="starts-with($filename, '/')">
      <xsl:value-of select="substring-after($filename, '/pages/')"/>
    </xsl:when>
    <xsl:otherwise>
      <xsl:value-of select="$filename"/>
    </xsl:otherwise>
  </xsl:choose>
</xsl:function>

</xsl:stylesheet>
