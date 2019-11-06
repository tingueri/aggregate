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
package org.opendatakit.common.security.common;

import java.io.Serializable;


/**
 * Shared code between GWT Javascript and the server side.  This class
 * defines the system-defined granted authority names.  The convention is that:
 * <ul><li>any name beginning with ROLE_ is a primitive authority.</li>
 * <li>any name beginning with RUN_AS_ is a primitive run-as directive.</li>
 * </ul>
 * Only non-primitive names can be granted primitive authorities.
 *
 * @author mitchellsundt@gmail.com
 */
public enum GrantedAuthorityName implements Serializable {

  AUTH_LOCAL("tout utilisateur authentifié via les informations d'identification détenues localement (<em> mot de passe global </ em>)"),
  AUTH_OUT_OF_BAND("tous les utilisateurs authentifiés via les mécanismes  hors bande"),
  AUTH_OPENID("tous les utilisateurs authentifiés via OpenID"),
  AUTH_GOOGLE_OAUTH2("tout utilisateur authentifié via un proxy Google Oauth2"),

  USER_IS_ANONYMOUS("pour un accès non authentifié"),
  USER_IS_REGISTERED("pour les utilisateurs enregistrés de ce système (un utilisateur identifié " +
      "en tant qu'utilisateur enregistré aura toujours été authentifié)"),
  USER_IS_DAEMON("réservé à l'exécution de tâches en arrière-plan"),

  ROLE_USER("requis pour afficher la page d'accueil (formulaires) et la liste xform xml lisible par l'homme"),
  ROLE_DATA_COLLECTOR("nécessaire pour récupérer les formulaires, les manifestes, les fichiers multimédias et télécharger les soumissions"),
  ROLE_ATTACHMENT_VIEWER("nécessaire pour visualiser des images, de la vidéo, de l'audio et d'autres données complexes dans le formulaire; diviser pour contourner la limitation de Google Earth."),
  ROLE_DATA_VIEWER("nécessaire pour visualiser les soumissions et pour générer des fichiers csv et kml et les télécharger"),
  ROLE_DATA_OWNER("nécessaire pour télécharger de nouveaux xforms, télécharger des modifications à des xforms existants, configurer des services externes et la publication de données, et supprimer des xforms ou leurs données"),
  ROLE_SITE_ACCESS_ADMIN("requis pour les pages de gestion des autorisations, y compris les utilisateurs enregistrés, les droits d'accès aux groupes et l'appartenance d'utilisateurs à des groupes"),

  GROUP_DATA_COLLECTORS("Collecteur de données"),
  GROUP_DATA_VIEWERS("Visualiseur de données"),
  GROUP_FORM_MANAGERS("Administrateur des formulaires"),
  GROUP_SITE_ADMINS("Administrateur Peogo Survey");

  public static final String GROUP_PREFIX = "GROUP_";
  public static final String ROLE_PREFIX = "ROLE_";
  public static final String RUN_AS_PREFIX = "RUN_AS_";
  private String displayText;

  private GrantedAuthorityName() {
    // GWT
  }

  GrantedAuthorityName(String displayText) {
    this.displayText = displayText;
  }

  public static final boolean permissionsCanBeAssigned(String authority) {
    return (authority != null) &&
        !(authority.startsWith(ROLE_PREFIX) || authority.startsWith(RUN_AS_PREFIX));
  }

  public String getDisplayText() {
    return displayText;
  }
}
