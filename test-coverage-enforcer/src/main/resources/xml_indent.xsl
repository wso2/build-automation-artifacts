<?xml version="1.0" encoding="utf-8" ?>
<!-- say indenter.xsl -->
<xsl:stylesheet version="1.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xalan="http://xml.apache.org/xslt"
>
<xsl:output method="xml" encoding="utf-8" indent="yes" xalan:indent-amount="4" />
<xsl:strip-space elements="*" />
<xsl:template match="@*|node()|comment()|processing-instruction()|text()">
    <xsl:copy>
        <xsl:apply-templates select="@*|node()|comment()|processing-instruction()|text()" />
    </xsl:copy>
</xsl:template>
</xsl:stylesheet>
