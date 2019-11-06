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
 * Constant values used in ODK aggregate to aid with servlet management
 *
 * @author wbrunette@gmail.com
 * @author mitchellsundt@gmail.com
 */
public final class ServletConsts {

  // system constants
  public static final String APPLICATION_NAME = "Peogo Survey";

  public static final String OPEN_ROSA_VERSION_HEADER = "X-OpenRosa-Version";
  public static final String OPEN_ROSA_VERSION = "1.0";

  public static final String OPEN_ROSA_ACCEPT_CONTENT_LENGTH_HEADER = "X-OpenRosa-Accept-Content-Length";

  /**
   * Flag on submissions and form uploads indicating that this is
   * a partial submission or form upload.
   */
  public static final String TRANSFER_IS_INCOMPLETE = "*isIncomplete*";
  /**
   * Name of form field that contains XML submission
   */
  public static final String XML_SUBMISSION_FILE = "xml_submission_file";
  /**
   * Name of form field that contains the form name value (form upload)
   */
  public final static String FORM_NAME_PRAM = "form_name";
  /**
   * Name of form field that contains the xform xml definittion (form upload)
   */
  public final static String FORM_DEF_PRAM = "form_def_file";

  /**
   * The name of the property that includes the form id
   */
  public static final String FORM_ID = "formId";

  /**
   * The name of the property that determines how to format webpage
   */
  public static final String HUMAN_READABLE = "readable";

  /**
   * The name of the property that specifies the type of interaction with an
   * external service
   */
  public static final String EXTERNAL_SERVICE_TYPE = "externalServiceType";

  public static final String BLOB_KEY = "blobKey";

  public static final String AS_ATTACHMENT = "as_attachment";

  public static final String DOWNLOAD_XML_BUTTON_TXT = "Download XML";

  public static final String CSV_FILENAME_APPEND = "_results.csv";

  public static final String KML_FILENAME_APPEND = "_results.kml";

  public static final String JSON_FILENAME_APPEND = "_results.json";

  public static final String RECORD_KEY = "record";

  public static final int EXPORT_CURSOR_CHUNK_SIZE = 100;
  /**
   * The name of the parameter that specifies the cursor location for retrieving
   * data from the data table (fragmented Csv servlet)
   */
  public static final String CURSOR = "cursor";
  /**
   * The name of the parameter that specifies how many rows to return from the
   * cursor (fragmented Csv servlet).
   */
  public static final String NUM_ENTRIES = "numEntries";

  /**
   * Script path to include...
   */
  public static final String UPLOAD_SCRIPT_RESOURCE = "javascript/upload_control.js";

  public static final String UPLOAD_STYLE_RESOURCE = "stylesheets/upload.css";

  public static final String UPLOAD_BUTTON_STYLE_RESOURCE = "stylesheets/button.css";

  public static final String UPLOAD_TABLE_STYLE_RESOURCE = "stylesheets/table.css";

  public static final String UPLOAD_NAVIGATION_STYLE_RESOURCE = "stylesheets/navigation.css";

  public static final String AGGREGATE_STYLE = "AggregateUI.css";

}
