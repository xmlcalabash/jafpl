<p:pipeline xmlns:p="http://www.w3.org/ns/xproc"
            xmlns:c="http://www.w3.org/ns/xproc-step"
            version="1.0">
  <p:serialization port="result" method="xhtml"/>
  <p:xinclude/>
  <p:xslt>
    <p:input port="stylesheet">
      <p:document href="webpage.xsl"/>
    </p:input>
  </p:xslt>
</p:pipeline>
