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


public enum ExportConsts implements HelpSliderConsts {
  EXPORT("Après exécution \"Export\" sur les soumissions -> Filtrer les soumissions, vous devriez avoir une liste de fichiers que vous avez exportés.",
      "Comprendre la table:<br>" +
          "1. Type de fichier - fichier CSV ou KML.<br>" +
          "2.  Status - Indique si le fichier est en cours de création ou s'il est maintenant disponible.<br>" +
          "3.  Heure demandée - indique l'heure à laquelle vous avez fini de remplir le formulaire \"Exporter\".<br>" +
          "4.  Time Completed - indique l'heure à laquelle la tâche \"Exporter\" est terminée et le fichier prêt..<br>" +
          "5.  Dernière tentative - indique l'heure à laquelle le fichier a été créé pour la dernière fois..<br>" +
          "6.  Télécharger le fichier - cliquez sur le lien pour voir votre fichier exporté.");

  private String title;
  private String content;

  private ExportConsts(String titleString, String contentString) {
    title = titleString;
    content = contentString;
  }

  public String getTitle() {
    return title;
  }

  public String getContent() {
    return content;
  }
}
