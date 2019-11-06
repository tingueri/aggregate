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

package org.opendatakit.aggregate.constants.common;

import java.io.Serializable;

/**
 * Enum of upload-only, streaming-only and upload-then-streaming
 * data propagation actions.  Used when configuring aggregate's
 * external services connections.
 *
 * @author wbrunette@gmail.com
 * @author mitchellsundt@gmail.com
 */
public enum ExternalServicePublicationOption implements Serializable {
  UPLOAD_ONLY("Télécharger les données de soumission existantes UNIQUEMENT"),
  STREAM_ONLY("Diffuser les nouvelles données de soumission UNIQUEMENT"),
  UPLOAD_N_STREAM("Télécharger les données existantes et les nouvelles données de soumission");

  private String descriptionOfOption;

  private ExternalServicePublicationOption() {
    // GWT
  }

  private ExternalServicePublicationOption(String desc) {
    descriptionOfOption = desc;
  }

  public String getDescriptionOfOption() {
    return descriptionOfOption;
  }

  public String toString() {
    return descriptionOfOption;
  }
}
