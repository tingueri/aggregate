/*
 * Copyright (C) 2013 University of Washington
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

package org.opendatakit.aggregate.client.exception;

import java.io.Serializable;

public class FormNotAvailableException extends RequestFailureException implements Serializable {
  private static final String DEFAULT_MSG = "FormNoLongerAvailableException: le formulaire n'est plus disponible à partir de Peogo Survey";

  private String message;

  public FormNotAvailableException(Throwable arg0) {
    super(arg0);
    message = DEFAULT_MSG + "(" + arg0.getMessage() + ")";
  }

  public FormNotAvailableException() {
  }

  @Override
  public String getLocalizedMessage() {
    return message;
  }

  @Override
  public String getMessage() {
    return message;
  }

}
