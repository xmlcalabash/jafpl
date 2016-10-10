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
  <link rel="icon" href="/graphics/icon.ico" type="image/ico"/>
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
          <em>fine</em>
          <xsl:text> pipeline language</xsl:text>
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
  <xsl:value-of select="substring-after($filename, '/pages/')"/>
</xsl:function>

</xsl:stylesheet>
