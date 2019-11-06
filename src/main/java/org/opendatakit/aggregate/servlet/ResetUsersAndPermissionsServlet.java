/*
 * Copyright (C) 2010 University of Washington
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
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.TreeSet;
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
import org.opendatakit.aggregate.rest.RFC4180CsvReader;
import org.opendatakit.aggregate.parser.MultiPartFormData;
import org.opendatakit.aggregate.parser.MultiPartFormItem;
import org.opendatakit.common.persistence.client.exception.DatastoreFailureException;
import org.opendatakit.common.security.User;
import org.opendatakit.common.security.client.UserSecurityInfo;
import org.opendatakit.common.security.client.UserSecurityInfo.UserType;
import org.opendatakit.common.security.common.EmailParser;
import org.opendatakit.common.security.common.EmailParser.Email;
import org.opendatakit.common.security.common.EmailParser.Email.Form;
import org.opendatakit.common.security.common.GrantedAuthorityName;
import org.opendatakit.common.security.server.SecurityServiceUtil;
import org.opendatakit.common.web.CallingContext;
import org.opendatakit.common.web.constants.HtmlConsts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Servlet for uploading a .csv file containing a list of users and their privileges.
 * This must contain all the users and their privileges on the system.  Passwords
 * can be managed separately using the UserManagePasswordsServlet or the UI.
 *
 * @author mitchellsundt@gmail.com
 */
public class ResetUsersAndPermissionsServlet extends ServletUtilBase {

  /**
   * URI from base
   */
  public static final String ADDR = UIConsts.USERS_AND_PERMS_UPLOAD_SERVLET_ADDR;
  /**
   * Name of form field that contains the users and capabilities csv file.
   */
  public final static String ACCESS_DEF_PRAM = "access_def_file";
  /**
   * Serial number for serialization
   */
  private static final long serialVersionUID = 3078038743780061673L;
  /**
   * Title for generated webpage
   */
  private static final String TITLE_INFO = "Définir les utilisateurs et les autorisations via le téléchargement d'un fichier .csv";

  private static final String UPLOAD_PAGE_BODY_START =

      "<div class=\"gwt-HTML\"><table class=\"gwt-TabPanel\"><tbody>"
          + "<tr><td><form id=\"ie_backward_compatible_form\""
          + " accept-charset=\"UTF-8\" method=\"POST\" encoding=\"multipart/form-data\" enctype=\"multipart/form-data\""
          + " action=\"";// emit the ADDR
  private static final String UPLOAD_PAGE_BODY_MIDDLE = "\">"
      + "     <table id=\"uploadTable\">"
      + "      <tr>"
      + "         <td><label for=\"access_def_file\">utilisateurs et fonctionnalités fichier csv:</label></td>"
      + "      </tr><tr>"
      + "         <td><input id=\"access_def_file\" type=\"file\" size=\"80\" class=\"gwt-Button\""
      + "            name=\"access_def_file\" /></td>"
      + "      </tr><tr>"
      + "         <td><input id=\"reset_permissions\" type=\"submit\" name=\"button\" class=\"gwt-Button\" value=\"Mettre à jour les autorisations\" /></td>"
      + "         <td />"
      + "      </tr>"
      + "     </table>\n"
      + "     </form>"
      + "<br><br></td></tr>"
      + "<tr><td><p id=\"subHeading\"><h2>Usage</h2></p>"
      + "<p>Utilisez Excel ou OpenOffice pour créer une feuille de calcul avec tous les utilisateurs du serveur et leurs fonctionnalités..</p>"
      + "<p>Enregistrez cette feuille de calcul sous forme de fichier .csv et chargez-la dans PeogoSurvey. Le serveur va:</p>"
      + "<ol><li>supprimer tous les utilisateurs non définis dans ce fichier,</li>"
      + "<li>créer des utilisateurs s'ils n'existent pas encore sur le serveur, et</li>"
      + "<li>modifier les fonctionnalités de tous les utilisateurs afin qu'ils correspondent à ceux définis dans le fichier .csv.</li>"
      + "</ol>"
      + "<p>Le fichier .csv peut commencer par un nombre quelconque de lignes contenant des informations spécifiques au site <em> fournies </ em> ci-dessous."
      + " les lignes contiennent moins de 4 colonnes.</p>"
      + "<p>La première ligne de 4 cellules ou plus large devrait contenir les en-têtes de colonne"
      + " pour la table des utilisateurs et des capacités; chaque ligne suivante définit un utilisateur sur le système."
      + " Les lignes vierges sont autorisées et sont ignorées. Les en-têtes de colonne non reconnus sont ignorés"
      + " (Celles-ci peuvent être utilisées pour des commentaires ou à d'autres fins spécifiques au site.).</p>"
      + "<p>Les en-têtes de colonne du tableau utilisateurs-capacités interprétés par PeogoSurvey sont::</p>"
      + "<ul>"
      + "<li><strong>Username</strong> - 'anonymousUser', ou un nom d'utilisateur PeogoSurvey ou une adresse électronique.</li>"
      + "<li><strong>Full Name</strong> - le nom convivial affiché en se référant à ce nom d'utilisateur.</li>"
      + "<li><strong>Account Type</strong> - 'ODK', ou 'Google' ou vide (pour anonymousUser)</li>"
      + "<li><strong>Data Collector</strong> - toute marque dans cette colonne accorde cette possibilité à cet utilisateur.</li>"
      + "<li><strong>Data Viewer</strong> - toute marque dans cette colonne accorde cette possibilité à cet utilisateur.</li>"
      + "<li><strong>Form Manager</strong> - toute marque dans cette colonne accorde cette possibilité à cet utilisateur.</li>"
      + "<li><strong>Site Administrator</strong> - toute marque dans cette colonne accorde cette possibilité à cet utilisateur.</li>"
      + "</ul>"
      + "<p>Parmi ceux-ci, seul 'Username' and 'Account Type' sont obligatoire.</p><p>Le serveur interdira certaines"
      + " actions telles que la suppression du super-utilisateur ou l'octroi de privilèges d'administrateur de site à anonymousUser</p>"
      + "</td></tr></tbody></table></div>\n";

  private static final Logger logger = LoggerFactory.getLogger(ResetUsersAndPermissionsServlet.class);

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws
      IOException {
    if (req.getScheme().equals("http")) {
      logger.warn("Réinitialisation des utilisateurs et des fonctionnalités via http");
    }
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
    headerString.append("<link rel=\"stylesheet\" type=\"text/css\" href=\"");
    headerString.append(cc.getWebApplicationURL(ServletConsts.AGGREGATE_STYLE));
    headerString.append("\" />");
    headerString.append("<link rel=\"stylesheet\" type=\"text/css\" href=\"");
    headerString.append(cc.getWebApplicationURL(ServletConsts.UPLOAD_BUTTON_STYLE_RESOURCE));
    headerString.append("\" />");
    headerString.append("<link rel=\"stylesheet\" type=\"text/css\" href=\"");
    headerString.append(cc.getWebApplicationURL(ServletConsts.UPLOAD_TABLE_STYLE_RESOURCE));
    headerString.append("\" />");
    headerString.append("<link rel=\"stylesheet\" type=\"text/css\" href=\"");
    headerString.append(cc.getWebApplicationURL(ServletConsts.UPLOAD_NAVIGATION_STYLE_RESOURCE));
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
   * and authentication have been properly established when uploading user
   * permission definitions via a program (e.g., Briefcase).
   */
  @Override
  protected void doHead(HttpServletRequest req, HttpServletResponse resp) {
    addOpenRosaHeaders(resp);
    // TODO Remove this header when no client relies on it to identify legacy Aggregate servers (v0.9 or older)
    resp.setHeader("Location", String.format("%s/%s", ContextFactory.getCallingContext(this, req).getServerURL(), ADDR));
    resp.setStatus(204);
  }

  /**
   * Processes the multipart form that contains the csv file which holds the
   * list of users and thier permissions. Returns success if the changes have
   * been applied; false otherwise.
   */
  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws
      IOException {
    if (req.getScheme().equals("http")) {
      logger.warn("Réinitialisation des utilisateurs et des fonctionnalités via http");
    }
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
      MultiPartFormData resetUsersAndPermissions = new MultiPartFormData(req);

      MultiPartFormItem usersAndPermissionsCsv = resetUsersAndPermissions
          .getFormDataByFieldName(ACCESS_DEF_PRAM);

      String inputCsv = null;

      if (usersAndPermissionsCsv != null) {
        // TODO: changed added output stream writer. probably something better
        // exists
        inputCsv = usersAndPermissionsCsv.getStream().toString(HtmlConsts.UTF8_ENCODE);
      }

      StringReader csvContentReader = null;
      RFC4180CsvReader csvReader = null;
      try {
        // we need to build up the UserSecurityInfo records for all the users
        ArrayList<UserSecurityInfo> users = new ArrayList<UserSecurityInfo>();

        // build reader for the csv content
        csvContentReader = new StringReader(inputCsv);
        csvReader = new RFC4180CsvReader(csvContentReader);

        // get the column headings -- these mimic those in Site Admin / Permissions table.
        // Order is irrelevant; no change-password column.
        //
        String[] columns;
        int row = 0;

        for (; ; ) {
          ++row;
          columns = csvReader.readNext();

          if (columns == null) {
            logger.error("Utilisateurs et fonctionnalités .csv chargé - fichier csv vide");
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                ErrorConsts.MISSING_PARAMS + "\nutilisateurs et fonctionnalités .csv est vide");
            return;
          }

          // count non-blank columns
          int nonBlankColCount = 0;
          for (String col : columns) {
            if (col != null && col.trim().length() != 0) {
              ++nonBlankColCount;
            }
          }

          // if there are fewer than 4 columns, it must be a comment field.
          // if there are 4 or more columns, then we expect it to be the column headers
          // for the users and capabilities table. We could require just 3, but that
          // would not be very useful or realistic.
          if (nonBlankColCount < 4) continue;

          break;
        }
        if (row != 1) {
          logger.warn("utilisateurs et fonctionnalités .csv upload - ligne d'interprétation " + row + " comme ligne d'en-tête de colonne");
          warnings.append("<tr><td>"+ row+" lignes interprétée(s) " + " comme ligne d'en-tête de colonne.</td></tr>");
        }

        // TODO: validate column headings....
        int idxUsername = -1;
        int idxFullName = -1;
        int idxUserType = -1;
        int idxDataCollector = -1;
        int idxDataViewer = -1;
        int idxFormManager = -1;
        int idxSiteAdmin = -1;

        for (int i = 0; i < columns.length; ++i) {
          String heading = columns[i];
          if (heading == null || heading.trim().length() == 0) {
            continue;
          }
          heading = heading.trim();
          // 'Username' is required
          if ("Username".compareToIgnoreCase(heading) == 0) {
            if (idxUsername != -1) {
              logger.error("utilisateurs et fonctionnalités .csv upload - fichier csv non valide - l'en-tête de colonne 'Nom d'utilisateur' est répété");
              resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                  ErrorConsts.MISSING_PARAMS + "\nUtilisateurs et fonctionnalités non valides .csv - l'en-tête de colonne 'Nom d'utilisateur' est répété");
              return;
            }
            idxUsername = i;
          }
          // 'Full Name' is optional. The value in 'Username' will be used to construct this if unspecified.
          else if ("Full Name".compareToIgnoreCase(heading) == 0) {
            if (idxFullName != -1) {
              logger.error("utilisateurs et fonctionnalités .csv chargé - fichier csv non valide - l'en-tête de colonne 'Full Name' est répété");
              resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                  ErrorConsts.MISSING_PARAMS + "\nUtilisateurs et fonctionnalités non valides .csv - l'en-tête de colonne 'Full Name' est répété");
              return;
            }
            idxFullName = i;
          }
          // 'Account Type' is required
          else if ("Account Type".compareToIgnoreCase(heading) == 0) {
            if (idxUserType != -1) {
              logger.error("Utilisateurs et fonctionnalités .csv chargé - Fichier csv non valide - L'en-tête de colonne 'Account Type' est répété");
              resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                  ErrorConsts.MISSING_PARAMS + "\nUtilisateurs et fonctionnalités non valides .csv - l'en-tête de colonne 'Account Type' est répété");
              return;
            }
            idxUserType = i;
          }
          // Permissions columns begin here. All are optional
          else if ("Data Collector".compareToIgnoreCase(heading) == 0) {
            if (idxDataCollector != -1) {
              logger.error("Utilisateurs et fonctionnalités .csv chargé - Fichier csv non valide - L'en-tête de colonne 'Data Collector' est répété");
              resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                  ErrorConsts.MISSING_PARAMS + "\nutilisateurs et fonctionnalités non valides .csv - l'en-tête de colonne 'Data Collector' est répété");
              return;
            }
            idxDataCollector = i;
          } else if ("Data Viewer".compareToIgnoreCase(heading) == 0) {
            if (idxDataViewer != -1) {
              logger.error("Utilisateurs et fonctionnalités .csv chargé - Fichier csv non valide - L'en-tête de colonne 'Data Viewer' est répété");
              resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                  ErrorConsts.MISSING_PARAMS + "\nutilisateurs et fonctionnalités non valides .csv - l'en-tête de colonne 'Data Viewer' est répété");
              return;
            }
            idxDataViewer = i;
          } else if ("Form Manager".compareToIgnoreCase(heading) == 0) {
            if (idxFormManager != -1) {
              logger.error("utilisateurs et fonctionnalités .csv chargé - fichier csv non valide - l'en-tête de colonne 'Form Manager' est répété");
              resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                  ErrorConsts.MISSING_PARAMS + "\nUtilisateurs et fonctionnalités non valides .csv - l'en-tête de colonne 'Form Manager' est répété");
              return;
            }
            idxFormManager = i;
          } else if ("Site Administrator".compareToIgnoreCase(heading) == 0) {
            if (idxSiteAdmin != -1) {
              logger.error("Utilisateurs et fonctionnalités .csv chargé - Fichier csv non valide - L'en-tête de colonne 'Site Administrator' est répété");
              resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                  ErrorConsts.MISSING_PARAMS + "\nusers and capabilities invalid .csv -- column header 'Site Administrator' is repeated");
              return;
            }
            idxSiteAdmin = i;
          } else {
            logger.warn("utilisateurs et fonctionnalités .csv chargé - fichier csv non valide - en-tête de colonne '" + heading + "' n'est pas reconnu");
            warnings.append("<tr><td>En-tête de colonne '" + heading + "' n'est pas reconnu et sera ignoré.</tr></td>");
          }
        }

        if (idxUsername == -1) {
          logger.error("Utilisateurs et fonctionnalités .csv chargé - fichier csv invalide");
          resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
              ErrorConsts.MISSING_PARAMS + "\nUtilisateurs et fonctionnalités non valides .csv - l'en-tête de colonne 'Username' est manquant");
          return;
        }
        if (idxUserType == -1) {
          logger.error("Utilisateurs et fonctionnalités .csv chargé - fichier csv invalide");
          resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
              ErrorConsts.MISSING_PARAMS + "\nutilisateurs et fonctionnalités non valides .csv - l'en-tête de colonne 'Account Type' est manquant");
          return;
        }

        while ((columns = csvReader.readNext()) != null) {
          ++row;

          // empty -- silently skip
          if (columns.length == 0) continue;

          // count non-blank columns
          int nonBlankColCount = 0;
          for (String col : columns) {
            if (col != null && col.trim().length() != 0) {
              ++nonBlankColCount;
            }

          }

          // all blank-- silently skip
          if (nonBlankColCount == 0) continue;

          // ignore rows where...
          // the row is not long enough to include the Username and Account Type columns
          if (columns.length <= idxUsername || columns.length <= idxUserType) {
            warnings.append("<tr><td>Ignorer la ligne " + row + " -- ne spécifie pas de Username et/ou Account Type.</tr></td>");
            continue;
          }

          // ignore rows where...
          // Username is not specified or it is not the anonymousUser and Account Type is blank
          if ((columns[idxUsername] == null || columns[idxUsername].trim().length() == 0) ||
              (!columns[idxUsername].equals(User.ANONYMOUS_USER) && (columns[idxUserType] == null || columns[idxUserType].trim().length() == 0))) {
            warnings.append("<tr><td>Ignorer la ligne " + row + " -- Username n'est pas le " + User.ANONYMOUS_USER + " et aucun Account Type specifié.</tr></td>");
            continue;
          }


          String accType = (idxUserType == -1) ? "ODK" : columns[idxUserType];
          UserType type = (accType == null) ? UserType.ANONYMOUS : UserType.REGISTERED;

          if ((type != UserType.ANONYMOUS) && (columns[idxUsername] == null)) {
            logger.error("Utilisateurs et fonctionnalités .csv chargé - fichier csv invalide");
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                ErrorConsts.MISSING_PARAMS + "\nutilisateurs et fonctionnalités non valides .csv - username non spécifié");
            return;
          }

          String username;
          String email;
          String fullname = (idxFullName == -1 || columns.length < idxFullName) ? null : columns[idxFullName];

          if (accType == null) {
            username = User.ANONYMOUS_USER;
            email = null;
            fullname = User.ANONYMOUS_USER_NICKNAME;
          } else if ("ODK".equals(accType)) {

            Collection<Email> emails = EmailParser.parseEmails(columns[idxUsername]);
            if (emails.size() != 1) {
              logger.error("Utilisateurs et fonctionnalités .csv chargé - fichier csv invalide");
              resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                  ErrorConsts.MISSING_PARAMS + "\nUtilisateurs et fonctionnalités non valides .csv - username \'" +
                      columns[idxUsername] + "\' contient des caractères illégaux (espaces, par exemple)");
              return;
            }
            email = null;
            Email parsedValue = emails.iterator().next();
            if (parsedValue.getType() == Form.EMAIL) {
              logger.error("Utilisateurs et fonctionnalités .csv chargé - fichier csv invalide");
              resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                  ErrorConsts.MISSING_PARAMS + "\nUtilisateurs et fonctionnalités non valides .csv -- username \'" +
                      columns[idxUsername] + "\' Vous ne pouvez pas utiliser un courrier électronique comme nom d'utilisateur du compte.");
              return;
            } else {
              username = parsedValue.getUsername();
            }
            if (fullname == null) {
              fullname = parsedValue.getFullName();
            }
          } else if ("Google".equals(accType)) {
            logger.error("Utilisateurs et fonctionnalités .csv chargé - fichier csv invalide");
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                ErrorConsts.MISSING_PARAMS + "\nUtilisateurs et fonctionnalités non valides .csv  -- Account Type \'" +
                    accType + "\' n'est pas supporté");
            return;
          } else {
            logger.error("Utilisateurs et fonctionnalités .csv chargé - fichier csv invalide");
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                ErrorConsts.MISSING_PARAMS + "\nUtilisateurs et fonctionnalités non valides .csv -- Account Type \'" +
                    accType + "\' n'est ni 'ODK' ni 'Google' ni vierge (anonymous)");
            return;
          }
          UserSecurityInfo info = new UserSecurityInfo(username, fullname, email, type);
          // now add permissions
          TreeSet<GrantedAuthorityName> authorities = new TreeSet<GrantedAuthorityName>();

          if (idxDataCollector != -1 && columns.length > idxDataCollector && columns[idxDataCollector] != null && columns[idxDataCollector].trim().length() != 0) {
            authorities.add(GrantedAuthorityName.GROUP_DATA_COLLECTORS);
          }
          if (idxDataViewer != -1 && columns.length > idxDataViewer && columns[idxDataViewer] != null && columns[idxDataViewer].trim().length() != 0) {
            authorities.add(GrantedAuthorityName.GROUP_DATA_VIEWERS);
          }
          if (idxFormManager != -1 && columns.length > idxFormManager && columns[idxFormManager] != null && columns[idxFormManager].trim().length() != 0) {
            authorities.add(GrantedAuthorityName.GROUP_FORM_MANAGERS);
          }
          if (idxSiteAdmin != -1 && columns.length > idxSiteAdmin && columns[idxSiteAdmin] != null && columns[idxSiteAdmin].trim().length() != 0) {
            authorities.add(GrantedAuthorityName.GROUP_SITE_ADMINS);
          }

          info.setAssignedUserGroups(authorities);
          users.add(info);
        }

        // allGroups is empty. This is currently not used.
        ArrayList<GrantedAuthorityName> allGroups = new ArrayList<GrantedAuthorityName>();

        // now scan for duplicate entries for the same username
        {
          HashMap<String, HashSet<UserSecurityInfo>> multipleRows = new HashMap<String, HashSet<UserSecurityInfo>>();
          for (UserSecurityInfo i : users) {
            if (i.getType() != UserType.REGISTERED) {
              continue;
            }
            if (i.getUsername() != null) {
              HashSet<UserSecurityInfo> existing;
              existing = multipleRows.get(i.getUsername());
              if (existing == null) {
                existing = new HashSet<UserSecurityInfo>();
                multipleRows.put(i.getUsername(), existing);
              }
              existing.add(i);
            }
          }
          for (Entry<String, HashSet<UserSecurityInfo>> entry : multipleRows.entrySet()) {
            if (entry.getValue().size() != 1) {
              logger.error("Utilisateurs et fonctionnalités .csv chargé - fichier csv invalide");
              resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                  ErrorConsts.MISSING_PARAMS + "\nutilisateurs et fonctionnalités non valides .csv -- " +
                      "plusieurs lignes définissent les capacités pour le même nom d'utilisateur: " + entry.getKey());
              return;
            }
          }
        }

        // and scan for duplicate entries for the same e-mail address
        {
          HashMap<String, HashSet<UserSecurityInfo>> multipleRows = new HashMap<String, HashSet<UserSecurityInfo>>();
          for (UserSecurityInfo i : users) {
            if (i.getType() != UserType.REGISTERED) {
              continue;
            }
            if (i.getEmail() != null) {
              HashSet<UserSecurityInfo> existing;
              existing = multipleRows.get(i.getEmail());
              if (existing == null) {
                existing = new HashSet<UserSecurityInfo>();
                multipleRows.put(i.getEmail(), existing);
              }
              existing.add(i);
            }
          }
          for (Entry<String, HashSet<UserSecurityInfo>> entry : multipleRows.entrySet()) {
            if (entry.getValue().size() != 1) {
              logger.error("Utilisateurs et fonctionnalités .csv chargé - fichier csv invalide");
              resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                  ErrorConsts.MISSING_PARAMS + "\nutilisateurs et fonctionnalités non valides .csv -- " +
                      "plusieurs lignes définissent les fonctionnalités pour le même courrier électronique: " +
                      entry.getKey().substring(EmailParser.K_MAILTO.length()));
              return;
            }
          }
        }

        // now scan for the anonymousUser
        UserSecurityInfo anonUser = null;
        for (UserSecurityInfo i : users) {
          if (i.getType() == UserType.ANONYMOUS) {
            if (anonUser != null) {
              logger.error("Utilisateurs et fonctionnalités .csv chargé - fichier csv invalide");
              resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                  ErrorConsts.MISSING_PARAMS + "\nutilisateurs et fonctionnalités non valides .csv -- " +
                      "Plusieurs lignes définissent les fonctionnalités de anonymousUser. Avez-vous oublié de spécifier le type de compte?");
              return;
            }
            anonUser = i;
          }
        }

        // and figure out whether the anonymousUser currently has ROLE_ATTACHMENT_VIEWER capabilities
        // (these allow Google Earth to access the server).
        //
        // If it does, preserve that capability.
        // To do this, fetch the existing info for anonymous...
        UserSecurityInfo anonExisting = new UserSecurityInfo(User.ANONYMOUS_USER,
            User.ANONYMOUS_USER_NICKNAME, null, UserSecurityInfo.UserType.ANONYMOUS);
        SecurityServiceUtil.setAuthenticationListsForSpecialUser(anonExisting,
            GrantedAuthorityName.USER_IS_ANONYMOUS, cc);
        // test if the existing anonymous had the capability
        if (anonExisting.getAssignedUserGroups().contains(GrantedAuthorityName.ROLE_ATTACHMENT_VIEWER)) {
          if (anonUser == null) {
            // no anonUser specified in the incoming .csv -- add it with just that capability.
            TreeSet<GrantedAuthorityName> auths = new TreeSet<GrantedAuthorityName>();
            auths.add(GrantedAuthorityName.ROLE_ATTACHMENT_VIEWER);
            anonExisting.setAssignedUserGroups(auths);
            users.add(anonExisting);
          } else {
            // add this capability to the existing set of capabilities
            anonUser.getAssignedUserGroups().add(GrantedAuthorityName.ROLE_ATTACHMENT_VIEWER);
          }
        }

        SecurityServiceUtil.setStandardSiteAccessConfiguration(users, allGroups, cc);

        // GAE requires some settle time before these entries will be
        // accurately retrieved. Do not re-fetch the form after it has been
        // uploaded.
        resp.setStatus(HttpServletResponse.SC_OK);
        if (openRosaVersion == null) {
          // web page -- show HTML response
          resp.setContentType(HtmlConsts.RESP_TYPE_HTML);
          resp.setCharacterEncoding(HtmlConsts.UTF8_ENCODE);
          PrintWriter out = resp.getWriter();

          StringBuilder headerString = new StringBuilder();
          headerString.append("<link rel=\"stylesheet\" type=\"text/css\" href=\"");
          headerString.append(cc.getWebApplicationURL(ServletConsts.AGGREGATE_STYLE));
          headerString.append("\" />");
          headerString.append("<link rel=\"stylesheet\" type=\"text/css\" href=\"");
          headerString.append(cc.getWebApplicationURL(ServletConsts.UPLOAD_BUTTON_STYLE_RESOURCE));
          headerString.append("\" />");
          headerString.append("<link rel=\"stylesheet\" type=\"text/css\" href=\"");
          headerString.append(cc.getWebApplicationURL(ServletConsts.UPLOAD_TABLE_STYLE_RESOURCE));
          headerString.append("\" />");
          headerString.append("<link rel=\"stylesheet\" type=\"text/css\" href=\"");
          headerString.append(cc.getWebApplicationURL(ServletConsts.UPLOAD_NAVIGATION_STYLE_RESOURCE));
          headerString.append("\" />");

          // header info
          beginBasicHtmlResponse(TITLE_INFO, headerString.toString(), resp, cc);
          if (warnings.length() != 0) {
            out.write("<p>Utilisateurs et fonctionnalités .csv téléchargés avec avertissements.</p>"
                + "<table>");
            out.write(warnings.toString());
            out.write("</table>");
          } else {
            out.write("<p>Utilisateurs et fonctionnalités .csv téléchargés avec succès.</p>");
          }
          out.write("<p>Click ");

          out.write(HtmlUtil.createHref(cc.getWebApplicationURL(ADDR), "ici", false));
          out.write(" pour revenir à la page de téléchargement des utilisateurs et des fonctionnalités .csv.</p>");
          finishBasicHtmlResponse(resp);
        } else {
          addOpenRosaHeaders(resp);
          resp.setContentType(HtmlConsts.RESP_TYPE_XML);
          resp.setCharacterEncoding(HtmlConsts.UTF8_ENCODE);
          PrintWriter out = resp.getWriter();
          out.write("<OpenRosaResponse xmlns=\"http://openrosa.org/http/response\">");
          if (warnings.length() != 0) {
            StringBuilder b = new StringBuilder();
            b.append("<p>Utilisateurs et fonctionnalités .csv téléchargés avec avertissements.</p>"
                + "<table>");
            b.append(warnings.toString());
            b.append("</table>");
            out.write("<message>");
            out.write(StringEscapeUtils.escapeXml10(b.toString()));
            out.write("</message>");
          } else {
            out.write("<message>Utilisateurs et fonctionnalités .csv téléchargés avec succès.</message>");
          }
          out.write("</OpenRosaResponse>");
        }

      } catch (DatastoreFailureException e) {
        logger.error("Utilisateurs et fonctionnalités .csv téléchargé. -- Erreur de persistance : " + e.toString());
        e.printStackTrace();
        resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
            ErrorConsts.PERSISTENCE_LAYER_PROBLEM + "\n" + e.toString());
      } finally {
        if (csvReader != null) {
          csvReader.close();
        }
        if (csvContentReader != null) {
          csvContentReader.close();
        }
      }

    } catch (FileUploadException e) {
      logger.error("Utilisateurs et fonctionnalités .csv téléchargé. -- Erreur de persistance: " + e.toString());
      e.printStackTrace(resp.getWriter());
      resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ErrorConsts.UPLOAD_PROBLEM);
    }
  }
}
