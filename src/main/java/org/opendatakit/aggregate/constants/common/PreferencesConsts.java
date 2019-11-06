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


public enum PreferencesConsts implements HelpSliderConsts {
  GOOGLE("Google API Credentials", "Ces informations d'identification sont utilisées lors de la publication dans les services Google."),
  ENKETO("Enketo Credentials", "Ces informations d'identification sont utilisées pour l'intégration des formulaires Web Enketo.");
  private String title;
  private String content;

  private PreferencesConsts(String titleString, String contentString) {
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
