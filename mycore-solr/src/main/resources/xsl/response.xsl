<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE xsl:stylesheet [
  <!ENTITY html-output SYSTEM "xsl/xsl-output-html.fragment">
]>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0" xmlns:encoder="xalan://java.net.URLEncoder"
  xmlns:i18n="xalan://org.mycore.services.i18n.MCRTranslation" xmlns:xalan="http://xml.apache.org/xalan" exclude-result-prefixes="xalan i18n encoder">
  &html-output;
  <xsl:include href="MyCoReLayout.xsl" />
  <xsl:include href="xslInclude:solrResponse"/>

  <xsl:param name="WebApplicationBaseURL" />
  <xsl:variable name="PageTitle" select="'Suchergebnisse'" />
  
  <xsl:template match="doc">
    <xsl:variable name="identifier" select="str[@name='id']" />

    <xsl:variable name="linkTo">
      <xsl:choose>
        <xsl:when test="str[@name='object_type'] = 'data_file'">
          <xsl:value-of select="concat($WebApplicationBaseURL, 'receive/', str[@name='derivate_owner'])" />
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="concat($WebApplicationBaseURL, 'receive/',$identifier)" />
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>

    <tr>
      <td class="resultTitle" colspan="2">
        <a href="{$linkTo}" target="_self">
          <xsl:choose>
            <xsl:when test="./arr[@name='search_result_link_text']">
              <xsl:value-of select="./arr[@name='search_result_link_text']/str[position() = 1]" />
            </xsl:when>
            <xsl:otherwise>
              <xsl:value-of select="$identifier" />
            </xsl:otherwise>
          </xsl:choose>
        </a>
      </td>
      <td rowspan="2" class="preview">
        <xsl:choose>
          <xsl:when test="str[@name='object_type'] = 'data_file'">
            <xsl:call-template name="iViewLinkPrev">
              <xsl:with-param name="derivates" select="./str[@name='file_owner']" />
              <xsl:with-param name="mcrid" select="./str[@name='derivate_owner']" />
              <xsl:with-param name="fileName" select="./str[@name='file_path']" />
            </xsl:call-template>
          </xsl:when>
          <xsl:otherwise>
            <xsl:call-template name="iViewLinkPrev">
              <xsl:with-param name="derivates" select="./arr[@name='derivates']/str" />
              <xsl:with-param name="mcrid" select="$identifier" />
              <xsl:with-param name="derivateLinks" select="./arr[@name='derivateLink']/str" />
            </xsl:call-template>
          </xsl:otherwise>
        </xsl:choose>
      </td>
    </tr>
    <tr>
      <td class="description" colspan="2">
        <xsl:if test="./arr[@name='placeOfActivity']">
          <xsl:value-of select="./arr[@name='placeOfActivity']/str[position() = 1]" />
          <br />
        </xsl:if>
        <xsl:if test="./str[@name='pnd']">
          <xsl:value-of select="concat('PND:', ./str[@name='pnd'])" />
          <br />
        </xsl:if>
        <xsl:variable name="date">
          <xsl:call-template name="formatISODate">
            <xsl:with-param select="./date[@name='modifydate']" name="date" />
            <xsl:with-param select="i18n:translate('metaData.date')" name="format" />
          </xsl:call-template>
        </xsl:variable>
        <xsl:value-of select="i18n:translate('results.lastChanged',$date)" />
        <br />
      </td>
    </tr>
  </xsl:template>

  <xsl:template name="iViewLinkPrev">
    <xsl:param name="derivates" />
    <xsl:param name="mcrid" />
    <xsl:param name="fileName" />
    <xsl:param name="derivateLinks" />

    <xsl:for-each select="$derivates">
      <xsl:variable name="firstSupportedFile">
        <xsl:choose>
          <xsl:when test="$fileName">
            <xsl:value-of select="$fileName" />
          </xsl:when>
          <xsl:otherwise>
            <xsl:call-template name="iview2.getSupport">
              <xsl:with-param select="." name="derivID" />
            </xsl:call-template>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:variable>

      <!-- MCR-IView ..start -->
      <xsl:if test="$firstSupportedFile != ''">
        <a>
          <xsl:attribute name="href">
          <xsl:value-of
            select="concat($WebApplicationBaseURL, 'receive/', $mcrid, '?jumpback=true&amp;maximized=true&amp;page=',$firstSupportedFile,'&amp;derivate=', .)" />
        </xsl:attribute>
          <xsl:attribute name="title">
          <xsl:value-of select="i18n:translate('metaData.iView')" />
        </xsl:attribute>
          <xsl:call-template name="iview2.getImageElement">
            <xsl:with-param select="." name="derivate" />
            <xsl:with-param select="$firstSupportedFile" name="imagePath" />
          </xsl:call-template>
        </a>
      </xsl:if>
    </xsl:for-each>

    <!-- display linked images -->
    <xsl:if test="$derivateLinks">
      <xsl:for-each select="$derivateLinks[string-length(.) &gt; 0]">
        <xsl:variable name="derivate" select="substring-before(. , '/')" />
        <xsl:variable name="pageToDisplay" select="concat('/', substring-after(., '/'))" />
        <a>
          <xsl:attribute name="href">
            <xsl:value-of
            select="concat($WebApplicationBaseURL,'receive/',$mcrid,'?jumpback=true&amp;maximized=true&amp;page=',$pageToDisplay,'&amp;derivate=', $derivate)" />
          </xsl:attribute>
          <xsl:attribute name="title">
            <xsl:value-of select="i18n:translate('metaData.iView')" />
          </xsl:attribute>
          <xsl:call-template name="iview2.getImageElement">
            <xsl:with-param select="$derivate" name="derivate" />
            <xsl:with-param select="$pageToDisplay" name="imagePath" />
          </xsl:call-template>
        </a>
      </xsl:for-each>
    </xsl:if>

  </xsl:template>


  <xsl:template match="/response">
    <xsl:variable name="hits" select="result/@numFound" />
    <xsl:variable name="start" select="lst[@name='responseHeader']/lst[@name='params']/str[@name='start']" />
    <xsl:variable name="rows" select="lst[@name='responseHeader']/lst[@name='params']/str[@name='rows']" />
    <xsl:variable name="query" select="encoder:encode(lst[@name='responseHeader']/lst[@name='params']/str[@name='q'])" />

    <xsl:variable name="pageTotal">
      <xsl:choose>
        <xsl:when test="ceiling($hits div $rows) = 0">
          <xsl:value-of select="1" />
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="ceiling($hits div $rows )" />
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>

    <!-- table header -->
    <table class="resultHeader" cellspacing="0" cellpadding="0">
      <tr>
        <td class="resultPages">
          <xsl:value-of
            select="concat(i18n:translate('searchResults.resultPage'), ': ', ceiling((($start + 1) - $rows) div $rows)+1 ,'/', $pageTotal )" />
        </td>
        <td class="resultCount">
          <strong>
            <xsl:value-of select="concat('Es wurden ', $hits,' Objekte gefunden')" />
          </strong>
        </td>
      </tr>
    </table>

    <!-- results -->
    <table id="resultList" cellspacing="0" cellpadding="0">
      <xsl:apply-templates select="result/doc" />
    </table>

    <!-- table footer -->
    <div id="pageSelection">
      <tr>
        <xsl:if test="($start - $rows) &gt;= 0">
          <xsl:variable name="startRecordPrevPage">
            <xsl:value-of select="$start - $rows" />
          </xsl:variable>
          <td>
            <a title="{i18n:translate('searchResults.prevPage')}"
              href="{concat($WebApplicationBaseURL,'servlets/SolrSelectProxy?q=', $query, '&amp;start=', $startRecordPrevPage, '&amp;rows=', $rows)}">&lt;</a>
          </td>
        </xsl:if>

        <xsl:variable name="startRecordNextPage">
          <xsl:value-of select="$start + $rows" />
        </xsl:variable>
        <xsl:if test="$startRecordNextPage &lt; $hits">
          <td>
            <a title="{i18n:translate('searchResults.nextPage')}" href="{concat($WebApplicationBaseURL,'servlets/SolrSelectProxy?q=', $query, '&amp;start=', $start + $rows, '&amp;rows=', $rows)}">&gt;</a>
          </td>
        </xsl:if>
      </tr>
    </div>
  </xsl:template>
</xsl:stylesheet>