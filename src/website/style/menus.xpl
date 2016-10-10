<p:declare-step xmlns:p="http://www.w3.org/ns/xproc" version="1.0"
                xmlns:c="http://www.w3.org/ns/xproc-step"
                xmlns:cx="http://xmlcalabash.com/ns/extensions"
                xmlns:exf="http://exproc.org/standard/functions"
                exclude-inline-prefixes="cx exf"
                name="main">
<p:input port="source"/>
<p:input port="parameters" kind="parameter"/>
<p:output port="result">
  <p:pipe step="menus" port="result"/>
</p:output>
<p:serialization port="result" indent="true"/>

<p:declare-step type="cx:message">
  <p:input port="source" sequence="true"/>
  <p:output port="result" sequence="true"/>
  <p:option name="message" required="true"/>
</p:declare-step>

<p:xslt name="menus">
  <p:input port="stylesheet">
    <p:document href="menus.xsl"/>
  </p:input>
</p:xslt>

<p:for-each>
  <p:iteration-source>
    <p:pipe step="menus" port="secondary"/>
  </p:iteration-source>

  <p:store name="store-chunk" method="xhtml" encoding="utf-8" indent="false">
    <p:with-option name="href" select="base-uri(/)"/>
    <p:log href="/tmp/log.xml" port="result"/>
  </p:store>
</p:for-each>

</p:declare-step>
