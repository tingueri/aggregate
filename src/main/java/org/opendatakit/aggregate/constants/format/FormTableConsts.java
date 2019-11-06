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
package org.opendatakit.aggregate.constants.format;

import org.opendatakit.aggregate.constants.HtmlUtil;

/**
 * @author wbrunette@gmail.com
 * @author mitchellsundt@gmail.com
 */
public final class FormTableConsts {

  // form table headers
  public static final String FT_HEADER_XFORM = "Définition Xform";
  public static final String FT_HEADER_CSV = "Soumissions en CSV";
  public static final String FT_HEADER_RESULTS = "Résultats de la soumission";
  public static final String FT_HEADER_USER = "Utilisateur";
  public static final String FT_HEADER_FORM_ID = "Identifiant";
  public static final String FT_HEADER_NAME = "Nom";
  public static final String FT_HEADER_KML = "Fichier KML";
  public static final String FT_HEADER_EXTERNAL_SERVICE = "Envoyer des soumissions à un service externe";
  public static final String FT_HEADER_QUERY = "Résultats de la requête";

  // button text
  public static final String XML_BUTTON_TXT = "Visualiser XML";
  public static final String CSV_BUTTON_TXT = "Créer un fichier CSV";
  public static final String RESULTS_BUTTON_TXT = "Visualiser les soumissions";
  public static final String EXTERNAL_SERVICE_BUTTON_TXT = "Ajouter une connexion de service externe";
  public static final String KML_BUTTON_TXT = "Créer un fichier KML";
  public static final String QUERY_BUTTON_TXT = "Requête";
  // xml form list tags
  public static final String URL_ATTR = "url";
  // link text
  public static final String VIEW_LINK_TEXT = "Voir";
  public static final String DOWNLOAD_LINK_TEXT = "Télécharger";
  public static final String FORMS_TAG = "forms";
  public static final String FORM_TAG = "form";
  public static final String BEGIN_FORMS_TAG = HtmlUtil.createBeginTag(FORMS_TAG);
  public static final String END_FORMS_TAG = HtmlUtil.createEndTag(FORMS_TAG);
  public static final String END_FORM_TAG = HtmlUtil.createEndTag(FORM_TAG);


}
