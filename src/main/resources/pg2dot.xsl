<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:g="http://jafpl.com/ns/graph"
                xmlns:dot="http://jafpl.com/ns/dot"
                xmlns="http://jafpl.com/ns/dot"
                version="2.0">

<!-- Yes, there's some irony in constructing a multi-stage pipeline directly
     in XSLT to support generation of diagrams for a pipeline language. -->

<xsl:output method="text" encoding="utf-8" indent="yes"/>
<xsl:strip-space elements="*"/>

<xsl:param name="digraph" select="()"/>

<!-- ============================================================ -->

<xsl:template match="/">
  <xsl:variable name="gv" as="element(dot:digraph)">
    <xsl:apply-templates/>
  </xsl:variable>

  <xsl:if test="exists($digraph)">
    <xsl:result-document method="xml" href="{$digraph}">
      <xsl:sequence select="$gv"/>
    </xsl:result-document>
  </xsl:if>

  <xsl:apply-templates select="$gv" mode="gv2dot"/>
</xsl:template>

<!-- ============================================================ -->

<xsl:key name="uid" match="g:node|g:container-end|g:container" use="@id"/>

<xsl:template match="g:graph">
  <digraph name="pg_graph">
    <xsl:apply-templates/>

    <!-- stick the edges in a temporary tree and remove duplicates -->

    <xsl:variable name="edges">
      <xsl:apply-templates select="//g:out-edge" mode="edges"/>
    </xsl:variable>
    <xsl:for-each select="$edges/dot:edge">
      <xsl:variable name="edge" select="."/>
      <xsl:if test="empty($edge/preceding-sibling::dot:edge
                             [@from=$edge/@from and @to=$edge/@to])">
        <xsl:sequence select="."/>
      </xsl:if>
    </xsl:for-each>
  </digraph>
</xsl:template>

<xsl:template match="g:container">
  <subgraph name="cluster-{g:id(.)}" label="{@name}\n{@label}"
            color="gray">
    <xsl:apply-templates/>
  </subgraph>
</xsl:template>

<xsl:template match="g:node">
  <subgraph name="cluster-{g:id(.)}" label="{@name}\n{@label}"
            color="black">
    <xsl:apply-templates/>
  </subgraph>
</xsl:template>

<xsl:template match="g:node[parent::g:graph]" priority="100">
  <xsl:apply-templates select="g:inputs/*|g:outputs/*" mode="boundary"/>
</xsl:template>

<xsl:template match="g:container-end">
  <subgraph name="cluster-{g:id(.)}" label="{@name}\n{@label}"
            color="black">
    <xsl:apply-templates/>
  </subgraph>
</xsl:template>

<xsl:template match="g:inputs">
  <subgraph name="cluster-{g:id(.)}" label="inputs"
            fontcolor="gray" style="rounded" color="gray">
    <xsl:apply-templates/>
  </subgraph>
</xsl:template>

<xsl:template match="g:in-edge">
  <port id="{g:id(.)}" label="{@input-port}"/>
</xsl:template>

<xsl:template match="g:outputs">
  <subgraph name="cluster-{g:id(.)}" label="outputs"
            fontcolor="gray" style="rounded" color="gray">
    <xsl:apply-templates/>
  </subgraph>
</xsl:template>

<xsl:template match="g:container/g:outputs" priority="100"/>

<xsl:template match="g:out-edge">
  <port id="{g:id(.)}" label="{@output-port}"/>
</xsl:template>

<xsl:template match="element()">
  <xsl:apply-templates/>
</xsl:template>

<xsl:template match="attribute()|text()|comment()|processing-instruction()">
  <!-- nop -->
</xsl:template>

<!-- ============================================================ -->

<xsl:template match="g:in-edge" mode="boundary">
  <port id="{g:id(.)}" label="{../../@name}\n{../../@label}\n{@input-port}"
        shape="house"/>
</xsl:template>

<xsl:template match="g:out-edge" mode="boundary">
  <port id="{g:id(.)}" label="{../../@name}\n{../../@label}\n{@input-port}"
        shape="invhouse"/>
</xsl:template>

<!-- ============================================================ -->

<xsl:template match="g:container/g:outputs" mode="container-outputs">
  <xsl:variable name="edges" as="element(dot:port)*">
    <xsl:for-each select="g:out-edge">
      <xsl:variable name="iport" select="@input-port"/>
      <xsl:variable name="oport" select="@output-port"/>
      <xsl:if test="empty(../../g:inputs/g:in-edge[@input-port = $iport])">
        <xsl:apply-templates select="."/>
      </xsl:if>
    </xsl:for-each>
  </xsl:variable>

  <xsl:if test="$edges">
    <subgraph name="cluster-{g:id(.)}" label="outputs"
              fontcolor="gray" style="rounded" color="gray">
      <xsl:sequence select="$edges"/>
    </subgraph>
  </xsl:if>
</xsl:template>

<!-- ============================================================ -->

<xsl:template match="g:container/g:outputs/g:out-edge" priority="100" mode="edges">
  <xsl:variable name="out-edge" select="."/>
  <xsl:variable name="end" select="key('uid', ../../@end)"/>
  <xsl:variable name="end-input"
                select="$end/g:inputs/g:in-edge[@input-port = $out-edge/@output-port]"/>
  <xsl:choose>
    <xsl:when test="$end-input">
      <xsl:variable name="to" select="key('uid', @destination)"/>
      <xsl:variable name="to-edge"
                    select="$to/g:inputs/g:in-edge[@input-port=$out-edge/@input-port]"/>
      <xsl:for-each select="$end-input">
        <xsl:variable name="from" select="."/>
        <xsl:for-each select="$to-edge">
          <xsl:variable name="to" select="."/>
          <edge from="{g:id($from)}" to="{g:id($to)}"/>
        </xsl:for-each>
      </xsl:for-each>
    </xsl:when>
    <xsl:otherwise>
      <xsl:next-match/>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template match="g:out-edge" mode="edges">
  <xsl:variable name="iport" select="@input-port"/>
  <xsl:variable name="oport" select="@output-port"/>
  <xsl:variable name="to" select="key('uid', @destination)"/>
  <xsl:variable name="to-edge"
                select="$to/g:inputs/g:in-edge[@input-port=$iport]"/>

  <!-- It's only a link through an input edge if the input
       edge is in a container -->
  <xsl:variable name="in-edge"
                select="../../g:inputs[parent::g:container]
                        /g:in-edge[@input-port=$iport]"/>

  <xsl:choose>
    <xsl:when test="$in-edge">
      <edge from="{g:id($in-edge)}" to="{g:id($to-edge)}"/>
    </xsl:when>
    <xsl:otherwise>
      <xsl:variable name="here" select="."/>
      <xsl:for-each select="$to-edge">
        <edge from="{g:id($here)}" to="{g:id(.)}"/>
      </xsl:for-each>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<!-- ============================================================ -->

<xsl:function name="g:id" as="xs:string">
  <xsl:param name="node" as="element()"/>
  <xsl:choose>
    <xsl:when test="$node/self::g:container and $node/@id">
      <xsl:value-of select="concat('C', $node/@id)"/>
    </xsl:when>
    <xsl:when test="$node/self::g:node and $node/@id">
      <xsl:value-of select="concat('N', $node/@id)"/>
    </xsl:when>
    <xsl:when test="$node/self::g:container-end and $node/@id">
      <xsl:value-of select="concat('E', $node/@id)"/>
    </xsl:when>
    <xsl:when test="$node/self::g:inputs">
      <xsl:value-of select="concat(g:id($node/..),'-inputs')"/>
    </xsl:when>
    <xsl:when test="$node/self::g:outputs">
      <xsl:value-of select="concat(g:id($node/..),'-outputs')"/>
    </xsl:when>
    <xsl:when test="$node/self::g:in-edge">
      <xsl:value-of select="concat(g:id($node/../..),'-I-',$node/@input-port)"/>
    </xsl:when>
    <xsl:when test="$node/self::g:out-edge">
      <xsl:variable name="oport" select="$node/@output-port"/>
      <xsl:choose>
        <xsl:when test="$node/../../self::g:container
                        and $node/../../g:inputs/g:in-edge[@input-port = $oport]">
          <xsl:value-of select="g:id($node/../../g:inputs/g:in-edge[@input-port = $oport])"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="concat(g:id($node/../..),'-O-',$node/@output-port)"/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:when>
    <xsl:otherwise>
      <xsl:message terminate="yes">ID of <xsl:sequence select="$node"/></xsl:message>
    </xsl:otherwise>
  </xsl:choose>
</xsl:function>

<!-- ============================================================ -->

<xsl:template match="dot:digraph" mode="gv2dot">
  <xsl:text>digraph </xsl:text>
  <xsl:value-of select="@name"/>
  <xsl:text> {&#10;</xsl:text>
  <xsl:apply-templates mode="gv2dot"/>
  <xsl:text>}&#10;</xsl:text>
</xsl:template>

<xsl:template match="dot:subgraph" mode="gv2dot">
  <xsl:text>subgraph "</xsl:text>
  <xsl:value-of select="@name"/>
  <xsl:text>" {&#10;</xsl:text>

  <xsl:for-each select="@*">
    <xsl:value-of select="local-name(.)"/>
    <xsl:text> = "</xsl:text>
    <xsl:value-of select="."/>
    <xsl:text>";&#10;</xsl:text>
  </xsl:for-each>

  <xsl:apply-templates mode="gv2dot"/>
  <xsl:text>}&#10;</xsl:text>
</xsl:template>

<xsl:template match="dot:port" mode="gv2dot">
  <xsl:text>"</xsl:text>
  <xsl:value-of select="@id"/>
  <xsl:text>" [&#10;</xsl:text>

  <xsl:for-each select="@* except @id">
    <xsl:value-of select="local-name(.)"/>
    <xsl:text> = "</xsl:text>
    <xsl:value-of select="."/>
    <xsl:text>";&#10;</xsl:text>
  </xsl:for-each>

  <xsl:text>];&#10;</xsl:text>
</xsl:template>

<xsl:template match="dot:edge" mode="gv2dot">
  <xsl:text>"</xsl:text>
  <xsl:value-of select="@from"/>
  <xsl:text>" -> "</xsl:text>
  <xsl:value-of select="@to"/>
  <xsl:text>";&#10;</xsl:text>
</xsl:template>

</xsl:stylesheet>
