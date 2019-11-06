/*
 * Copyright (C) 2011 University of Washington
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

package org.opendatakit.aggregate.constants.common;

public class UIConsts {
  public static final String KML_NONE_OPTION = "Aucun";
  public static final String KML_NONE_ENCODE_KEY = "*NONE*";

  public static final String URI_DEFAULT = "no uuid";
  public static final String PREVIEW_PARAM = "previewImage";
  public static final String PREVIEW_SET = "&" + PREVIEW_PARAM + "=true";
  public static final String PREVIEW_IMAGE_STYLENAME = "thumbnail";
  public static final String HOST_PAGE_BASE_ADDR = "Aggregate.html";
  public static final String VERTICAL_FLOW_PANEL_STYLENAME = "verticalFlowPanel";

  public static final String FILTER_NONE = "Aucun";

  public static final String FORM_UPLOAD_SERVLET_ADDR = "formUpload";
  public static final String USERS_AND_PERMS_UPLOAD_SERVLET_ADDR = "ssl/reset-users-and-permissions";
  public static final String GET_USERS_AND_PERMS_CSV_SERVLET_ADDR = "ssl/get-users-and-permissions";

  public static final String SERVICE_ACCOUNT_PRIVATE_KEY_UPLOAD_ADDR = "ssl/oauth2-service-account";

  public static final String ENKETO_SERVICE_ACCOUNT_PRIVATE_KEY_UPLOAD_ADDR = "ssl/enketo-service-account";
  public static final String ENKETO_API_HANDLER_ADDR = "enk/enketoApiHandler";

  public static final String SUBMISSION_SERVLET_ADDR = "submission";
  public static final String ERROR_NO_FILTERS = "Vous avez besoin d'au moins un filtre pour enregistrer un groupe.";
  public static final String ERROR_NO_NAME = "Vous devez donner un nom à ce groupe de filtres pour continuer.";
  public static final String PROMPT_FOR_NAME_TXT = "S'il vous plaît entrer un nom pour ce groupe";
  public static final String REPROMPT_FOR_NAME_TXT = "Ce groupe existe déjà. S'il vous plaît entrer un nouveau nom";
  public static final String PROMPT_FOR_EMAIL_TXT = "Veuillez saisir l'adresse e-mail avec laquelle l'accès à ces documents sera autorisé.";
  public static final String REPROMPT_FOR_EMAIL_TXT = "Ce n'est pas une adresse email valide. Veuillez saisir l'adresse e-mail avec laquelle l'accès à ces documents sera autorisé.";

}
