/*
 * Copyright (C) 2009 Google Inc.
 * Copyright (C) 2010 University of Washington.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.opendatakit.aggregate.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.lang3.StringEscapeUtils;
import org.opendatakit.aggregate.ContextFactory;
import org.opendatakit.aggregate.constants.ErrorConsts;
import org.opendatakit.aggregate.constants.HtmlUtil;
import org.opendatakit.aggregate.constants.ServletConsts;
import org.opendatakit.aggregate.constants.common.UIConsts;
import org.opendatakit.aggregate.exception.ODKFormAlreadyExistsException;
import org.opendatakit.aggregate.exception.ODKIncompleteSubmissionData;
import org.opendatakit.aggregate.exception.ODKParseException;
import org.opendatakit.aggregate.parser.FormParserForJavaRosa;
import org.opendatakit.aggregate.parser.MultiPartFormData;
import org.opendatakit.aggregate.parser.MultiPartFormItem;
import org.opendatakit.common.persistence.PersistConsts;
import org.opendatakit.common.persistence.exception.ODKDatastoreException;
import org.opendatakit.common.persistence.exception.ODKEntityPersistException;
import org.opendatakit.common.web.CallingContext;
import org.opendatakit.common.web.constants.BasicConsts;
import org.opendatakit.common.web.constants.HtmlConsts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Servlet to upload, parse, and save an XForm
 *
 * @author wbrunette@gmail.com
 * @author mitchellsundt@gmail.com
 */
public class FormUploadServlet extends ServletUtilBase {

  /**
   * URI from base
   */
  public static final String ADDR = UIConsts.FORM_UPLOAD_SERVLET_ADDR;
  /**
   * Serial number for serialization
   */
  private static final long serialVersionUID = -3784460108221008112L;
  /**
   * Title for generated webpage
   */
  private static final String TITLE_INFO = "Charger Xform";

  private static final String UPLOAD_PAGE_BODY_START =

      "<div style=\"overflow: auto;\"><p id=\"subHeading\"><h2>Telecharger un formulaire dans Peogo Survey</h2></p>"
          + "<!--[if true]><p style=\"color: red;\">Pour une meilleure experience utilisateur, utilisez Chrome, Firefox ou Safari</p>"
          + "<![endif] -->"
          + "<form id=\"ie_backward_compatible_form\""
          + " accept-charset=\"UTF-8\" method=\"POST\" encoding=\"multipart/form-data\" enctype=\"multipart/form-data\""
          + " action=\"";// emit the ADDR
  private static final String UPLOAD_PAGE_BODY_MIDDLE = "\">"
      + "     <table id=\"uploadTable\">"
      + "       <tr>"
      + "           <td><label for=\"form_def_file\">Definition de formulaire (fichier XML):</label></td>"
      + "           <td><input id=\"form_def_file\" type=\"file\" size=\"80\" class=\"gwt-Button\""
      + "               name=\"form_def_file\" /></td>"
      + "       </tr>\n"
      + "       <tr>"
      + "           <td><label for=\"mediaFiles\">Fichier (s) media optionnel:</label></td>"
      + "           <td><input id=\"mediaFiles\" class=\"gwt-Button\" type=\"file\" size=\"80,20\" name=\"datafile\" multiple /><input id=\"clear_media_files\" type=\"button\" class=\"gwt-Button\" value=\"Effacer\" onClick=\"clearMediaInputField('mediaFiles')\" /></td>"
      + "       </tr>"
      + "       <!--[if true]>"
      + "         <tr>"
      + "             <td><label for=\"mediaFiles2\">Fichier multimedia #2:</label></td>"
      + "             <td><input id=\"mediaFiles2\" class=\"gwt-Button\" type=\"file\" size=\"80\" name=\"datafile\" /><input id=\"clear_media_files2\" type=\"button\" class=\"gwt-Button\" value=\"Effacer\" onClick=\"clearMediaInputField('mediaFiles2')\" /></td>"
      + "         </tr>"
      + "         <tr>"
      + "             <td><label for=\"mediaFiles3\">Fichier multimedia #3:</label></td>"
      + "             <td><input id=\"mediaFiles3\" class=\"gwt-Button\" type=\"file\" size=\"80\" name=\"datafile\" /><input id=\"clear_media_files3\" type=\"button\" class=\"gwt-Button\" value=\"Effacer\" onClick=\"clearMediaInputField('mediaFiles3')\" /></td>"
      + "         </tr>"
      + "         <tr>"
      + "             <td><label for=\"mediaFiles4\">Fichier multimedia #4:</label></td>"
      + "             <td><input id=\"mediaFiles4\" class=\"gwt-Button\" type=\"file\" size=\"80\" name=\"datafile\" /><input id=\"clear_media_files4\" type=\"button\" class=\"gwt-Button\" value=\"Effacer\" onClick=\"clearMediaInputField('mediaFiles4')\" /></td>"
      + "         </tr>"
      + "         <tr>"
      + "             <td><label for=\"mediaFiles5\">Fichier multimedia #5:</label></td>"
      + "             <td><input id=\"mediaFiles5\" class=\"gwt-Button\" type=\"file\" size=\"80\" name=\"datafile\" /><input id=\"clear_media_files5\" type=\"button\" class=\"gwt-Button\" value=\"Effacer\" onClick=\"clearMediaInputField('mediaFiles5')\" /></td>"
      + "         </tr>"
      + "         <tr>"
      + "             <td><label for=\"mediaFiles6\">Fichier multimedia #6:</label></td>"
      + "             <td><input id=\"mediaFiles6\" class=\"gwt-Button\" type=\"file\" size=\"80\" name=\"datafile\" /><input id=\"clear_media_files6\" type=\"button\" class=\"gwt-Button\" value=\"Effacer\" onClick=\"clearMediaInputField('mediaFiles6')\" /></td>"
      + "         </tr>"
      + "         <![endif]-->\n"
      + "       <tr>"
      + "           <td><input id=\"upload_form\" type=\"submit\" name=\"button\" class=\"gwt-Button\" value=\"Telecharger le formulaire\" /></td>"
      + "           <td />"
      + "       </tr>"
      + "     </table>\n"
      + "     </form>"
      + "<p>Fichiers multimedias pour le logo du formulaire, les images, les clips audio et les clips video "
      + "(le cas echeant) doit figurer dans un seul repertoire sans sous-repertoires.</p>"
      + "<br><br>"
      + "<p id=\"note\"><b><font color=\"red\">NOTE:</font> Si la definition de formulaire contient des reponses à une chaine, les donnees de la chaine seront tronquees "
      + Long.toString(PersistConsts.DEFAULT_MAX_STRING_LENGTH)
      + " caracteres.</b>  Consultez la documentation Peogo Survey pour savoir comment augmenter (ou diminuer) cette taille..</p>"
      + "<br>"
      + "<p>Sur Peogo Survey, le fichier nomme \"<code>form_logo.png</code>\","
      + "contenu dans le dossier de support, sera affiche comme logo du formulaire. </p>" + "</div>\n";

  /**
   * Title for generated webpage to obtain title
   */
  private static final String OBTAIN_TITLE_INFO = "Entree de titre Xform";

  /**
   * Text to display to user to obtain title
   */
  private static final String TITLE_OF_THE_XFORM = "Titre du Xform:";

  private static final Logger logger = LoggerFactory.getLogger(FormUploadServlet.class);

  /**
   * Handler for HTTP Get request to create xform upload page
   *
   * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest,
   *     javax.servlet.http.HttpServletResponse)
   */
  @Override
  public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    CallingContext cc = ContextFactory.getCallingContext(this, req);

    Double openRosaVersion = getOpenRosaVersion(req);
    if (openRosaVersion != null) {
      /*
       * If we have an OpenRosa version header, assume that this is due to a
       * channel redirect (http: => https:) and that the request was originally
       * a HEAD request. Reply with a response appropriate for a HEAD request.
       *
       * It is unclear whether this is a GAE issue or a Spring Frameworks issue.
       */
      logger.warn("Inside doGet -- replying as doHead");
      doHead(req, resp);
      return;
    }

    StringBuilder headerString = new StringBuilder();
    headerString.append("<script type=\"application/javascript\" src=\"");
    headerString.append(cc.getWebApplicationURL(ServletConsts.UPLOAD_SCRIPT_RESOURCE));
    headerString.append("\"></script>");
    headerString.append("<link rel=\"stylesheet\" type=\"text/css\" href=\"");
    headerString.append(cc.getWebApplicationURL(ServletConsts.UPLOAD_STYLE_RESOURCE));
    headerString.append("\" />");
    headerString.append("<link rel=\"stylesheet\" type=\"text/css\" href=\"");
    headerString.append(cc.getWebApplicationURL(ServletConsts.UPLOAD_BUTTON_STYLE_RESOURCE));
    headerString.append("\" />");
    headerString.append("<link rel=\"stylesheet\" type=\"text/css\" href=\"");
    headerString.append(cc.getWebApplicationURL(ServletConsts.AGGREGATE_STYLE));
    headerString.append("\" />");

    // header info
    beginBasicHtmlResponse(TITLE_INFO, headerString.toString(), resp, cc);
    PrintWriter out = resp.getWriter();
    out.write(UPLOAD_PAGE_BODY_START);
    out.write(cc.getWebApplicationURL(ADDR));
    out.write(UPLOAD_PAGE_BODY_MIDDLE);
    finishBasicHtmlResponse(resp);
  }

  /**
   * Handler for HTTP head request. This is used to verify that channel security
   * and authentication have been properly established when uploading form
   * definitions via a program (e.g., Briefcase).
   */
  @Override
  protected void doHead(HttpServletRequest req, HttpServletResponse resp) {
    addOpenRosaHeaders(resp);
    // TODO Remove this header when no client relies on it to identify legacy Aggregate servers (v0.9 or older)
    resp.setHeader("Location", String.format("%s/%s", ContextFactory.getCallingContext(this, req).getServerURL(), ADDR));
    resp.setStatus(204);
  }

  /**
   * Handler for HTTP Post request that takes an xform, parses, and saves a
   * parsed version in the datastore
   *
   * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest,
   *     javax.servlet.http.HttpServletResponse)
   */
  @Override
  public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    CallingContext cc = ContextFactory.getCallingContext(this, req);

    Double openRosaVersion = getOpenRosaVersion(req);

    // verify request is multipart
    if (!ServletFileUpload.isMultipartContent(req)) {
      resp.sendError(HttpServletResponse.SC_BAD_REQUEST, ErrorConsts.NO_MULTI_PART_CONTENT);
      return;
    }

    StringBuilder warnings = new StringBuilder();
    // TODO Add in form title process so it will update the changes in the XML
    // of form

    try {
      // process form
      MultiPartFormData uploadedFormItems = new MultiPartFormData(req);

      FormParserForJavaRosa parser = null;

      MultiPartFormItem formNameData = uploadedFormItems
          .getFormDataByFieldName(ServletConsts.FORM_NAME_PRAM);
      MultiPartFormItem formXmlData = uploadedFormItems
          .getFormDataByFieldName(ServletConsts.FORM_DEF_PRAM);

      String formName = null;
      String inputXml = null;
      String xmlFileName = "default.xml";

      if (formNameData != null) {
        formName = formNameData.getStream().toString(HtmlConsts.UTF8_ENCODE);
      }
      if (formXmlData != null) {
        // TODO: changed added output stream writer. probably something better
        // exists
        inputXml = formXmlData.getStream().toString(HtmlConsts.UTF8_ENCODE);
        xmlFileName = formXmlData.getFilename();
      }

      try {
        parser = new FormParserForJavaRosa(formName, formXmlData, inputXml, xmlFileName,
            uploadedFormItems, warnings, cc);
        logger.info("Telecharger le formulaire avec succes: " + parser.getFormId());
        // GAE requires some settle time before these entries will be
        // accurately retrieved. Do not re-fetch the form after it has been
        // uploaded.
        resp.setStatus(HttpServletResponse.SC_CREATED);
        resp.setHeader("Location", cc.getServerURL() + BasicConsts.FORWARDSLASH + ADDR);
        if (openRosaVersion == null) {
          // web page -- show HTML response
          resp.setContentType(HtmlConsts.RESP_TYPE_HTML);
          resp.setCharacterEncoding(HtmlConsts.UTF8_ENCODE);

          StringBuilder headerString = new StringBuilder();
          headerString.append("<script type=\"application/javascript\" src=\"");
          headerString.append(cc.getWebApplicationURL(ServletConsts.UPLOAD_SCRIPT_RESOURCE));
          headerString.append("\"></script>");
          headerString.append("<link rel=\"stylesheet\" type=\"text/css\" href=\"");
          headerString.append(cc.getWebApplicationURL(ServletConsts.UPLOAD_STYLE_RESOURCE));
          headerString.append("\" />");
          headerString.append("<link rel=\"stylesheet\" type=\"text/css\" href=\"");
          headerString.append(cc.getWebApplicationURL(ServletConsts.UPLOAD_BUTTON_STYLE_RESOURCE));
          headerString.append("\" />");
          headerString.append("<link rel=\"stylesheet\" type=\"text/css\" href=\"");
          headerString.append(cc.getWebApplicationURL(ServletConsts.AGGREGATE_STYLE));
          headerString.append("\" />");

          // header info
          beginBasicHtmlResponse(TITLE_INFO, headerString.toString(), resp, cc);
          PrintWriter out = resp.getWriter();
          if (warnings.length() != 0) {
            out.write("<p>Formulaire charge avec des avertissements. Il existe des champs de valeur dans le formulaire qui "
                + "n'ont pas de <code>&lt;bind/&gt;</code> declaration  ou <code>&lt;bind/&gt;</code> "
                + "ces declarations n'ont pas d'attribut <code>type</code> "
                + "identifiant le type de donnees de ce champ (par exemple, boolean, int, decimal, date, dateTime, time, string, "
                + "select1, select, barcode, geopoint or binary).</p>"
                + "<p><b>Tous ces champs de valeur ont ete declares en tant que valeurs de chaine.</b> Il utilisera "
                + "l'ordre lexical sur ces champs. Par exemple, la valeur 100 sera consideree comme inferieure à 11.</p>"
                + "<p><font color=\"red\">Si ces champs de valeur contiennent des donnees de date, de date et d’heure, de temps ou numeriques (par exemple, decimal ou int), "
                + "Peogo Survey produira des tris errones et des resultats de filtrage errones par rapport à ces champs de valeur.</font></p>"
                + "<table><th><td>Nom du champs</td></th>");
            out.write(warnings.toString());
            out.write("</table>");
          } else {
            out.write("<p>Telechargement de formulaire reussi.</p>");
          }
          out.write("<p>Cliquez sur ");

          out.write(HtmlUtil.createHref(cc.getWebApplicationURL(ADDR), "here", false));
          out.write(" pour revenir ajouter une nouvelle page de formulaire.</p>");
          finishBasicHtmlResponse(resp);
        } else {
          addOpenRosaHeaders(resp);
          resp.setContentType(HtmlConsts.RESP_TYPE_XML);
          resp.setCharacterEncoding(HtmlConsts.UTF8_ENCODE);
          PrintWriter out = resp.getWriter();
          out.write("<OpenRosaResponse xmlns=\"http://openrosa.org/http/response\">");
          if (warnings.length() != 0) {
            StringBuilder b = new StringBuilder();
            b.append("<p>Formulaire charge avec des avertissements. Il existe des champs de valeur dans le formulaire qui "
                    + "n'ont pas de <code>&lt;bind/&gt;</code> declaration  ou <code>&lt;bind/&gt;</code> "
                    + "ces declarations n'ont pas d'attribut <code>type</code> "
                    + "identifiant le type de donnees de ce champ (par exemple, boolean, int, decimal, date, dateTime, time, string, "
                    + "select1, select, barcode, geopoint or binary).</p>"
                    + "<p><b>Tous ces champs de valeur ont ete declares en tant que valeurs de chine.</b> Il utilisera "
                    + "l'ordre lexical sur ces champs. Par exemple, la valeur 100 sera consideree comme inferieure a 11.</p>"
                    + "<p><font color=\"red\">Si ces champs de valeur contiennent des donnees de date, de date et d’heure, de temps ou numeriques (par exemple, decimal ou int), "
                    + "Peogo Survey produira des tris errones et des resultats de filtrage errones par rapport à ces champs de valeur.</font></p>"
                    + "<table><th><td>Nom du champs</td></th>");
            b.append(warnings.toString());
            b.append("</table>");
            out.write("<message>");
            out.write(StringEscapeUtils.escapeXml10(b.toString()));
            out.write("</message>");
          } else {
            out.write("<message>Envoi reussi.</message>");
          }
          out.write("</OpenRosaResponse>");
        }

      } catch (ODKFormAlreadyExistsException e) {
        logger.info("Le formulaire existe deja: " + e.toString());
        resp.sendError(HttpServletResponse.SC_CONFLICT, ErrorConsts.FORM_WITH_ODKID_EXISTS + "\n"
            + e.toString());
      } catch (ODKIncompleteSubmissionData e) {
        logger.warn("Erreur d'analyse de telechargement de formulaire: " + e.toString());
        switch (e.getReason()) {
          case TITLE_MISSING:
            createTitleQuestionWebpage(resp, inputXml, xmlFileName, cc);
            return;
          case ID_MALFORMED:
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, ErrorConsts.JAVA_ROSA_PARSING_PROBLEM
                + "\n" + e.toString());
            return;
          case ID_MISSING:
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, ErrorConsts.MISSING_FORM_ID);
            return;
          case MISSING_XML:
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, ErrorConsts.MISSING_FORM_INFO);
            return;
          case BAD_JR_PARSE:
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, ErrorConsts.JAVA_ROSA_PARSING_PROBLEM
                + "\n" + e.toString());
            return;
          case MISMATCHED_SUBMISSION_ELEMENT:
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                ErrorConsts.FORM_INVALID_SUBMISSION_ELEMENT);
            return;
          default:
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, ErrorConsts.INVALID_PARAMS);
            return;
        }
      } catch (ODKEntityPersistException e) {
        // TODO NEED TO FIGURE OUT PROPER ACTION FOR ERROR
        logger.error("Erreur de persistance d'envoi de formulaire: " + e.toString());
        e.printStackTrace();
        resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
            ErrorConsts.PERSISTENCE_LAYER_PROBLEM + "\n" + e.toString());
      } catch (ODKDatastoreException e) {
        logger.error("Erreur de persistance d'envoi de formulaire: " + e.toString());
        e.printStackTrace();
        resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
            ErrorConsts.PERSISTENCE_LAYER_PROBLEM + "\n" + e.toString());
      } catch (ODKParseException e) {
        // unfortunately, the underlying javarosa utility swallows the parsing
        // error.
        logger.error("Erreur de persistance d'envoi de formulaire: " + e.toString());
        e.printStackTrace();
        resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
            ErrorConsts.PARSING_PROBLEM + "\n" + e.toString());
      }
    } catch (FileUploadException e) {
      logger.error("Erreur de persistance d'envoi de formulaire: " + e.toString());
      e.printStackTrace(resp.getWriter());
      resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ErrorConsts.UPLOAD_PROBLEM);
    }
  }

  private void createTitleQuestionWebpage(HttpServletResponse resp, String formXml,
                                          String xmlFileName, CallingContext cc) throws IOException {
    beginBasicHtmlResponse(OBTAIN_TITLE_INFO, resp, cc); // header info

    PrintWriter out = resp.getWriter();
    out.write(HtmlUtil.createFormBeginTag(cc.getWebApplicationURL(FormUploadServlet.ADDR),
        HtmlConsts.MULTIPART_FORM_DATA, HtmlConsts.POST));
    out.write(TITLE_OF_THE_XFORM + HtmlConsts.LINE_BREAK);
    out.write(HtmlUtil.createInput(HtmlConsts.INPUT_TYPE_TEXT, ServletConsts.FORM_NAME_PRAM, null));
    out.write(HtmlConsts.LINE_BREAK + HtmlConsts.LINE_BREAK);
    out.write(HtmlUtil.encodeFormInHiddenInput(formXml, xmlFileName));
    out.write(HtmlConsts.LINE_BREAK + HtmlConsts.LINE_BREAK);
    out.write(HtmlUtil.createInput(HtmlConsts.INPUT_TYPE_SUBMIT, null, "Submit"));
    out.write(HtmlConsts.FORM_CLOSE);
    finishBasicHtmlResponse(resp);
    System.out.println("\n\n\nservlet de telechargement de formulaire\n\n\n");
  }

}
