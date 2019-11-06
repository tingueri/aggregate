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

package org.opendatakit.aggregate.constants;

/**
 * Constants used in ODK aggregate to report errors
 *
 * @author wbrunette@gmail.com
 * @author mitchellsundt@gmail.com
 */
public final class ErrorConsts {

  public static final String POSSIBLE_CORRUPTION = "A rencontré un problème inconnu lors de la lecture de la base de données. C'est souvent un signe de corruption de la base de données. S'il vous plaît consulter les journaux du serveur et déclarer un problème à https://github.com/opendatakit/aggregate/issues";
  /**
   * Error message if the form with FORM ID is not found
   */
  public static final String ODKID_NOT_FOUND =
      "Impossible de trouver le formulaire avec l'identifiant de formulaire correspondant en tant que soumission";
  public static final String FORM_NOT_FOUND =
      "Formulaire non trouvé";
  public static final String FORM_DEFINITION_INVALID =
      "Définition de formulaire incomplète ou manquante";
  /**
   * Error message for if key was not successfully part of the request
   */
  public static final String ODK_KEY_PROBLEM = "Rencontré un problème de réception de clé";
  /**
   * Error message if the FORM ID in the form already exists
   */
  public static final String FORM_WITH_ODKID_EXISTS = "Le formulaire existe déjà pour cet attribut Namespace / Id";

  /**
   * Error message if responseURL returns null
   */
  public static final String ENKETOAPI_RETURN_NULL_RESPONSE = "Problème d'accès à Enketo. Vérifiez les paramètres d’Enketo Webform Integration dans l’onglet Préférences et réessayez.";

  public static final String FORM_INVALID_SUBMISSION_ELEMENT = "Les attributs de l'élément de soumission ne correspondent pas aux attributs de formulaire";
  /**
   * Error message if not all information was received
   */
  public static final String MISSING_FORM_INFO = "N'a pas reçu le nom du formulaire et la description XML du formulaire";

  /**
   * Error message if form ID was not specified
   */
  public static final String MISSING_FORM_ID = "Le formulaire n'a pas spécifié d'identifiant de formulaire. Pour plus d'informations sur l'identifiant de formulaire, veuillez consulter la FAQ d'Open Data Kit.";

  /**
   * Error message if request is not multi-part
   */
  public static final String NO_MULTI_PART_CONTENT = "La demande ne contient pas de contenu en plusieurs parties";
  public static final String INCOMPLETE_DATA = "Problème lors de la localisation d'une partie des données de soumission nécessaires pour compléter la demande";
  /**
   * Constant error string if child does not implement a setValueFromByteArray override
   */
  public static final String BINARY_ERROR = "Le système devrait avoir envoyé une méthode de conversion binaire appropriée";
  public static final String PARSING_PROBLEM = "Problème d'analyse de soumission XML";
  public static final String FORM_DOES_NOT_ALLOW_SUBMISSIONS = "Les soumissions ont été refusées sur ce formulaire";
  public static final String TASK_LOCK_PROBLEM = "Impossible de verrouiller pour la modification ou la création de cette soumission. Réessayer.";
  /**
   * Constant used to log error if string array does not match column size
   */
  public static final String ROW_SIZE_ERROR = "Vous aviez essayé d'ajouter une ligne à la table de résultats qui ne correspondait pas à la taille de l'en-tête! REJET!";
  /**
   * Error message if not all information was received
   */
  public static final String INSUFFIECENT_PARAMS = "Paramètres insuffisants reçus";
  public static final String SUBMISSION_NOT_FOUND =
      "N'a pas trouvé de soumission correspondant aux paramètres fournis";
  public static final String NO_STRING_TO_BLOB_CONVERT = "Le blob ne peut pas être créé à partir d'une chaîne";
  public static final String UNKNOWN_INTERFACE = "Certains champs de la soumission n'ont pas été renseigné ou soumission repetée ";
  public static final String INVALID_PARAMS = "Paramètre (s) non valide";
  public static final String MISSING_PARAMS = "Un ou plusieurs paramètres requis sont manquants";

  /**
   * Constant string identifying XML stream
   */
  public static final String INPUTSTREAM_ERROR = "Problème lors de l'obtention du flux d'entrée submissionXML!";
  public static final String NO_IMAGE_EXISTS = "Aucune image n'existe pour cette entrée!";
  public static final String NOT_A_KEY = "Un type incorrect a été stocké, dans l'attente d'une clé pour le lien de vue";
  public static final String TASK_PROBLEM = "Problème avec la tâche: ";

  public static final String QUOTA_EXCEEDED = "Quota dépassé";
  public static final String PERSISTENCE_LAYER_PROBLEM = "Problème persistant de données ou d'accès aux données";
  public static final String UPLOAD_PROBLEM = "La transmission du téléchargement a échoué";
  public static final String EXPORTED_FILE_PROBLEM = "Problème d'accès au fichier de données exporté";

  public static final String JAVA_ROSA_PARSING_PROBLEM = "Problème avec le formulaire d'analyse JavaRosa:";
}
