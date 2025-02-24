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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.lang3.StringEscapeUtils;
import org.opendatakit.aggregate.ContextFactory;
import org.opendatakit.aggregate.constants.BeanDefs;
import org.opendatakit.aggregate.constants.ErrorConsts;
import org.opendatakit.aggregate.constants.HtmlUtil;
import org.opendatakit.aggregate.constants.ParserConsts;
import org.opendatakit.aggregate.constants.ServletConsts;
import org.opendatakit.aggregate.constants.common.FormElementNamespace;
import org.opendatakit.aggregate.constants.common.OperationalStatus;
import org.opendatakit.aggregate.constants.common.UIConsts;
import org.opendatakit.aggregate.exception.ODKConversionException;
import org.opendatakit.aggregate.exception.ODKFormNotFoundException;
import org.opendatakit.aggregate.exception.ODKFormSubmissionsDisabledException;
import org.opendatakit.aggregate.exception.ODKIncompleteSubmissionData;
import org.opendatakit.aggregate.exception.ODKParseException;
import org.opendatakit.aggregate.externalservice.ExternalService;
import org.opendatakit.aggregate.externalservice.FormServiceCursor;
import org.opendatakit.aggregate.form.IForm;
import org.opendatakit.aggregate.format.Row;
import org.opendatakit.aggregate.format.element.XmlAttributeFormatter;
import org.opendatakit.aggregate.parser.MultiPartFormData;
import org.opendatakit.aggregate.parser.SubmissionParser;
import org.opendatakit.aggregate.submission.Submission;
import org.opendatakit.aggregate.task.UploadSubmissions;
import org.opendatakit.aggregate.util.BackendActionsTable;
import org.opendatakit.common.persistence.exception.ODKDatastoreException;
import org.opendatakit.common.persistence.exception.ODKEntityPersistException;
import org.opendatakit.common.persistence.exception.ODKTaskLockException;
import org.opendatakit.common.web.CallingContext;
import org.opendatakit.common.web.constants.BasicConsts;
import org.opendatakit.common.web.constants.HtmlConsts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Servlet to process a submission from a form
 *
 * @author wbrunette@gmail.com
 * @author mitchellsundt@gmail.com
 */
public class SubmissionServlet extends ServletUtilBase {

  /**
   * URI from base
   */
  public static final String ADDR = UIConsts.SUBMISSION_SERVLET_ADDR;
  private static final Logger logger = LoggerFactory.getLogger(SubmissionServlet.class);
  /**
   * Serial number for serialization
   */
  private static final long serialVersionUID = -9115712148453543651L;
  /**
   * Title for generated webpage
   */
  private static final String TITLE = "Telecharger les soumissions";

  private static final String UPLOAD_PAGE_BODY_START =

      "<div style=\"overflow: auto;\">"
          + "<p id=\"subHeading\"><b>Teleccharger une soumission dans Peogo Survey</b></p>"
          + "<!--[if true]><p style=\"color: red;\">Pour une meilleure experience utilisateur, utilisez Chrome, Firefox ou Safari</p>"
          + "<![endif] -->"
          + "<form id=\"ie_backward_compatible_form\""
          + "                        accept-charset=\"UTF-8\" method=\"POST\" encoding=\"multipart/form-data\" enctype=\"multipart/form-data\""
          + "                        action=\"";// emit the ADDR
  private static final String UPLOAD_PAGE_BODY_MIDDLE = "\">"
      + "    <table id=\"uploadTable\">"
      + "     <tr>"
      + "        <td><label for=\"xml_submission_file\">Fichier de données de soumission:</label></td>"
      + "        <td><input id=\"xml_submission_file\" type=\"file\" size=\"80\" class=\"gwt-Button\""
      + "           name=\"xml_submission_file\" /></td>"
      + "     </tr>"
      + "     <tr>"
      + "        <td><label for=\"mediaFiles\">Fichier (s) de donnees associe (s):</label></td>"
      + "        <td><input id=\"mediaFiles\" type=\"file\" class=\"gwt-Button\" size=\"80,20\" name=\"datafile\" multiple /><input id=\"clear_media_files\" type=\"button\" class=\"gwt-Button\" value=\"Effacer\" onClick=\"clearMediaInputField('mediaFiles')\" /></td>"
      + "     </tr>"
      + "     <!--[if true]>"
      + "        <tr>"
      + "            <td><label for=\"mediaFiles2\">Fichier de donnees associe #2:</label></td>"
      + "            <td><input id=\"mediaFiles2\" class=\"gwt-Button\" type=\"file\" size=\"80\" name=\"datafile\" /><input id=\"clear_media_files2\" type=\"button\" class=\"gwt-Button\" value=\"Effacer\" onClick=\"clearMediaInputField('mediaFiles2')\" /></td>"
      + "        </tr>"
      + "        <tr>"
      + "            <td><label for=\"mediaFiles3\">Fichier de données associé #3:</label></td>"
      + "            <td><input id=\"mediaFiles3\" class=\"gwt-Button\" type=\"file\" size=\"80\" name=\"datafile\" /><input id=\"clear_media_files3\" type=\"button\" class=\"gwt-Button\" value=\"Effacer\" onClick=\"clearMediaInputField('mediaFiles3')\" /></td>"
      + "        </tr>"
      + "        <tr>"
      + "            <td><label for=\"mediaFiles4\">Fichier de données associé #4:</label></td>"
      + "            <td><input id=\"mediaFiles4\" class=\"gwt-Button\" type=\"file\" size=\"80\" name=\"datafile\" /><input id=\"clear_media_files4\" type=\"button\" class=\"gwt-Button\" value=\"Effacer\" onClick=\"clearMediaInputField('mediaFiles4')\" /></td>"
      + "        </tr>"
      + "        <tr>"
      + "            <td><label for=\"mediaFiles5\"Fichier de données associé #5:</label></td>"
      + "            <td><input id=\"mediaFiles5\" class=\"gwt-Button\" type=\"file\" size=\"80\" name=\"datafile\" /><input id=\"clear_media_files5\" type=\"button\" class=\"gwt-Button\" value=\"Effacer\" onClick=\"clearMediaInputField('mediaFiles5')\" /></td>"
      + "        </tr>"
      + "        <tr>"
      + "            <td><label for=\"mediaFiles6\">Fichier de données associé #6:</label></td>"
      + "            <td><input id=\"mediaFiles6\" class=\"gwt-Button\" type=\"file\" size=\"80\" name=\"datafile\" /><input id=\"clear_media_files6\" type=\"button\" class=\"gwt-Button\" value=\"Effacer\" onClick=\"clearMediaInputField('mediaFiles6')\" /></td>"
      + "        </tr>"
      + "        <![endif]-->"
      + "     <tr>"
      + "        <td><input id=\"upload_submission\" type=\"submit\" name=\"button\" class=\"gwt-Button\" value=\"Envoyer la soumission\" /></td>"
      + "        <td />"
      + "     </tr>"
      + "    </table>"
      + "    </form>"
      + "<p id=\"note\">Les envois se trouvent dans le repertoire <code> / odk / instances </ code> de la carte SD du telephone."
      + " Ce repertoire contiendra des sous-repertoires avec des noms de la forme: <code> formID_yyyy-mm-jj_hh-MM-ss</code></p>"
      + "<p>Dans chacun de ces sous-repertoires se trouve le fichier de donnees de soumission (nomme: <code> formID_yyyy-mm-jj_hh-MM-ss.xml</code>),"
      + "et zero ou plusieurs fichiers de donnees associes pour les images, les clips audio, les clips vidéo, "
      + "etc. lie à cette soumission.</p>" + "</div>";

  /**
   * Handler for HTTP Get request that processes a form submission
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

    beginBasicHtmlResponse(TITLE, headerString.toString(), resp, cc);// header
    // info
    PrintWriter out = resp.getWriter();
    out.write(UPLOAD_PAGE_BODY_START);
    out.write(cc.getWebApplicationURL(ADDR));
    out.write(UPLOAD_PAGE_BODY_MIDDLE);
    finishBasicHtmlResponse(resp);
  }

  /**
   * Handler for HTTP head request. This is used to verify that channel security
   * and authentication have been properly established.
   */
  @Override
  protected void doHead(HttpServletRequest req, HttpServletResponse resp) {
    addOpenRosaHeaders(resp);
    // TODO Remove this header when no client relies on it to identify legacy Aggregate servers (v0.9 or older)
    resp.setHeader("Location", String.format("%s/%s", ContextFactory.getCallingContext(this, req).getServerURL(), ADDR));
    resp.setStatus(204);
  }

  /**
   * Handler for HTTP post request that processes a form submission Currently
   * supports plain/xml and multipart
   *
   * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest,
   *     javax.servlet.http.HttpServletResponse)
   */
  @Override
  public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    CallingContext cc = ContextFactory.getCallingContext(this, req);

    Double openRosaVersion = getOpenRosaVersion(req);
    boolean isIncomplete = false;
    try {
      SubmissionParser submissionParser = null;
      if (ServletFileUpload.isMultipartContent(req)) {
        MultiPartFormData uploadedSubmissionItems = new MultiPartFormData(req);
        String isIncompleteFlag = uploadedSubmissionItems
            .getSimpleFormField(ServletConsts.TRANSFER_IS_INCOMPLETE);
        isIncomplete = (isIncompleteFlag != null && isIncompleteFlag.compareToIgnoreCase("YES") == 0);
        submissionParser = new SubmissionParser(uploadedSubmissionItems, isIncomplete, cc);
      } else {
        // TODO: check that it is the proper types we can deal with
        // XML received, we hope...
        submissionParser = new SubmissionParser(req.getInputStream(), cc);
      }

      IForm form = submissionParser.getForm();

      // Only trigger uploads if this submission was not already
      // marked as complete before this interaction and if it is
      // now complete. AND...
      // Issue a publish request only if we haven't issued one recently.
      // use BackendActionsTable to mediate that decision.
      // This test ONLY OCCURS during submissions, not during Watchdog
      // firings, so we don't have to worry about bugs here affecting Watchdog.
      if (!submissionParser.wasPreexistingComplete() &&
          submissionParser.getSubmission().isComplete() &&
          BackendActionsTable.triggerPublisher(form.getUri(), cc)) {
        // send information to remote servers that need to be notified
        List<ExternalService> tmp = FormServiceCursor.getExternalServicesForForm(form, cc);
        UploadSubmissions uploadTask = (UploadSubmissions) cc.getBean(BeanDefs.UPLOAD_TASK_BEAN);

        // publication failures should not fail the submission...
        CallingContext ccDaemon = ContextFactory.getCallingContext(this, req);
        ccDaemon.setAsDaemon(true);
        for (ExternalService rs : tmp) {
          // only create upload tasks for active publishers
          if (rs.getFormServiceCursor().getOperationalStatus() == OperationalStatus.ACTIVE) {
            uploadTask.createFormUploadTask(rs.getFormServiceCursor(), ccDaemon);
          }
        }
      }

      // form full url including scheme...
      String serverUrl = cc.getServerURL();
      String url = serverUrl + BasicConsts.FORWARDSLASH + ADDR;
      resp.setHeader("Location", url);

      resp.setStatus(HttpServletResponse.SC_CREATED);
      if (openRosaVersion == null) {
        logger.info("Soumission réussie non-OpenRosa");

        resp.setContentType(HtmlConsts.RESP_TYPE_HTML);
        resp.setCharacterEncoding(HtmlConsts.UTF8_ENCODE);
        PrintWriter out = resp.getWriter();
        out.write(HtmlConsts.HTML_OPEN);
        out.write(HtmlConsts.BODY_OPEN);
        out.write("<p>Téléchargement de soumission réussi.</p><p>Cliquez ");
        out.write(HtmlUtil.createHref(cc.getWebApplicationURL(ADDR), "ici", false));
        out.write(" retourner à télécharger la page de soumission.</p>");
        out.write(HtmlConsts.BODY_CLOSE);
        out.write(HtmlConsts.HTML_CLOSE);
      } else {
        logger.info("Soumission réussie d'OpenRosa");

        addOpenRosaHeaders(resp);
        resp.setContentType(HtmlConsts.RESP_TYPE_XML);
        resp.setCharacterEncoding(HtmlConsts.UTF8_ENCODE);
        PrintWriter out = resp.getWriter();
        out.write("<OpenRosaResponse xmlns=\"http://openrosa.org/http/response\">");
        if (isIncomplete) {
          out.write("<message>l'envoi partiel de la soumission a réussi!</message>");
        } else {
          out.write("<message>l'envoi complet de la soumission a réussi!</message>");
        }

        // for Briefcase2, use the attributes on a <submissionMetadata> tag to
        // update the local copy of the data (if these attributes are
        // available).
        {
          XmlAttributeFormatter attributeFormatter = new XmlAttributeFormatter();
          Submission sub = submissionParser.getSubmission();
          Row attributeRow = new Row(sub.constructSubmissionKey(null));
          //
          // add what could be considered the form's metadata...
          //
          attributeRow.addFormattedValue("id=\"" + StringEscapeUtils.escapeXml10(form.getFormId().replace(ParserConsts.FORWARD_SLASH_SUBSTITUTION, ParserConsts.FORWARD_SLASH))
              + "\"");
          if (form.isEncryptedForm()) {
            attributeRow.addFormattedValue("encrypted=\"yes\"");
          }
          sub.getFormattedNamespaceValuesForRow(attributeRow,
              Collections.singletonList(FormElementNamespace.METADATA), attributeFormatter, false,
              cc);

          out.write("<submissionMetadata xmlns=\""
              + StringEscapeUtils.escapeXml10(ParserConsts.NAMESPACE_ODK) + "\"");
          Iterator<String> itrAttributes = attributeRow.getFormattedValues().iterator();
          while (itrAttributes.hasNext()) {
            out.write(" ");
            out.write(itrAttributes.next());
          }
          out.write("/>");
        }
        out.write("</OpenRosaResponse>");
      }
    } catch (ODKFormNotFoundException e) {
      e.printStackTrace();
      logger.warn("Form not found - " + e.getMessage());
      odkIdNotFoundError(resp);
    } catch (ODKParseException e) {
      logger.warn("Parsing failure - " + e.getMessage());
      e.printStackTrace();
      resp.sendError(HttpServletResponse.SC_BAD_REQUEST, ErrorConsts.PARSING_PROBLEM);
    } catch (ODKEntityPersistException e) {
      logger.error("Persist failure - " + e.getMessage());
      e.printStackTrace();
      resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ErrorConsts.PARSING_PROBLEM);
    } catch (ODKIncompleteSubmissionData e) {
      logger.warn("Incomplete submission failure - " + e.getMessage());
      e.printStackTrace();
      resp.sendError(HttpServletResponse.SC_BAD_REQUEST, ErrorConsts.PARSING_PROBLEM);
    } catch (ODKConversionException e) {
      logger.warn("Datatype casting failure - " + e.getMessage());
      e.printStackTrace();
      resp.sendError(HttpServletResponse.SC_BAD_REQUEST, ErrorConsts.PARSING_PROBLEM);
    } catch (ODKDatastoreException e) {
      logger.error("Datastore failure - " + e.getMessage());
      e.printStackTrace();
      resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ErrorConsts.PARSING_PROBLEM);
    } catch (ODKTaskLockException e) {
      logger.error("Task lock failure - " + e.getMessage());
      e.printStackTrace();
      resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ErrorConsts.TASK_LOCK_PROBLEM);
    } catch (FileUploadException e) {
      logger.warn("Attachments parsing failure - " + e.getMessage());
      e.printStackTrace();
      resp.sendError(HttpServletResponse.SC_BAD_REQUEST, ErrorConsts.PARSING_PROBLEM);
    } catch (ODKFormSubmissionsDisabledException e) {
      logger.warn("Form submission disabled - " + e.getMessage());
      e.printStackTrace();
      resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
          ErrorConsts.FORM_DOES_NOT_ALLOW_SUBMISSIONS);
    } catch (Exception e) {
      logger.error("Unexpected exception: " + e.getMessage());
      e.printStackTrace();
      resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unexpected exception");
    }
  }
}
