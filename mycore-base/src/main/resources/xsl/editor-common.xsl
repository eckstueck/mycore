<?xml version="1.0" encoding="UTF-8"?>

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xalan="http://xml.apache.org/xalan"
  xmlns:editor="http://www.mycore.org/editor" xmlns:i18n="xalan://org.mycore.services.i18n.MCRTranslation">

  <!-- ========================================================================= -->

  <!-- ============ Parameter aus MyCoRe LayoutServlet ============ -->
  <xsl:param name="WebApplicationBaseURL" />
  <xsl:param name="ServletsBaseURL" />
  <xsl:param name="DefaultLang" />
  <xsl:param name="CurrentLang" />
  <xsl:param name="MCRSessionID" />
  <xsl:param name="HttpSession" />
  <xsl:param name="JSessionID" />

  <!-- ========= multi-language label ======== -->
  <xsl:template name="output.label">
    <xsl:choose>

      <!-- If there is a i18n key, output the translation from messages file -->
      <xsl:when test="@i18n">
        <xsl:value-of select="i18n:translate(@i18n)" disable-output-escaping="yes" />
      </xsl:when>

      <!-- If there is a label with i18n key, output the translation from messages file -->
      <xsl:when test="label[@i18n]">
        <xsl:value-of select="i18n:translate(label/@i18n)" disable-output-escaping="yes" />
      </xsl:when>

      <!-- If there is a label with xml:lang = selected lang, output it -->
      <xsl:when test="label[lang($CurrentLang) or lang('all')]">
        <xsl:for-each select="label[lang($CurrentLang) or lang('all')]">
          <xsl:copy-of select="*|text()" />
        </xsl:for-each>
      </xsl:when>

      <!-- Otherwise, if there is a label in the default language, output it -->
      <xsl:when test="label[lang($DefaultLang)]">
        <xsl:for-each select="label[lang($DefaultLang)]">
          <xsl:copy-of select="*|text()" />
        </xsl:for-each>
      </xsl:when>

      <!-- Otherwise, use the language-independent @label attribute, if it exists -->
      <xsl:when test="@label">
        <xsl:value-of select="@label" />
      </xsl:when>

      <!-- Otherwise, use the language-independent nested label elements, if existing -->
      <xsl:when test="label[string-length(@xml:lang)=0]">
        <xsl:for-each select="label[string-length(@xml:lang)=0]">
          <xsl:copy-of select="*|text()" />
        </xsl:for-each>
      </xsl:when>

      <xsl:when test="@title">
        <xsl:copy-of select="*|text()" />
      </xsl:when>

      <!-- Otherwise, use the first label of any language that exists -->
      <xsl:otherwise>
        <xsl:for-each select="label[1]">
          <xsl:copy-of select="*|text()" />
        </xsl:for-each>
      </xsl:otherwise>

    </xsl:choose>
  </xsl:template>

  <!-- ========================================================================= -->

  <!-- ========= multi-language title ======== -->
  <xsl:template name="output.title">
    <xsl:param name="usefont" select="'no'" />

    <xsl:if test="$usefont = 'yes'">
      <xsl:text disable-output-escaping="yes">&lt;span class="editorText" &gt;</xsl:text>
    </xsl:if>

    <xsl:choose>

      <!-- If there is a title with xml:lang = selected lang, output it -->
      <xsl:when test="title[lang($CurrentLang) or lang('all')]">
        <xsl:for-each select="title[lang($CurrentLang) or lang('all')]">
          <xsl:copy-of select="*|text()" />
        </xsl:for-each>
      </xsl:when>

      <!-- Otherwise, if there is a title in the default language, output it -->
      <xsl:when test="title[lang($DefaultLang)  or lang('all')]">
        <xsl:for-each select="title[lang($DefaultLang) or lang('all')]">
          <xsl:copy-of select="*|text()" />
        </xsl:for-each>
      </xsl:when>

      <!-- Otherwise, use the language-independent @title attribute, if it exists -->
      <xsl:when test="@title">
        <xsl:value-of select="@title" />
      </xsl:when>

      <!-- Otherwise, use the language-independent nested title elements, if existing -->
      <xsl:when test="title[string-length(@xml:lang)=0]">
        <xsl:for-each select="title[string-length(@xml:lang)=0]">
          <xsl:copy-of select="*|text()" />
        </xsl:for-each>
      </xsl:when>

      <!-- Otherwise, use the first title of any language that exists -->
      <xsl:otherwise>
        <xsl:for-each select="title[1]">
          <xsl:copy-of select="*|text()" />
        </xsl:for-each>
      </xsl:otherwise>

      <!-- Otherwise give up, user is too stupid to configure the editor -->

    </xsl:choose>

    <xsl:if test="$usefont = 'yes'">
      <xsl:text disable-output-escaping="yes">&lt;/span&gt;</xsl:text>
    </xsl:if>

  </xsl:template>

  <!-- ========================================================================= -->

</xsl:stylesheet>
