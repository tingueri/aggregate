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

package org.opendatakit.aggregate.client;

import static org.opendatakit.aggregate.client.LayoutUtils.buildVersionNote;

import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import org.opendatakit.aggregate.buildconfig.BuildConfig;
import org.opendatakit.aggregate.client.preferences.Preferences;
import org.opendatakit.aggregate.client.preferences.Preferences.PreferencesCompletionCallback;
import org.opendatakit.aggregate.client.widgets.ServletPopupButton;
import org.opendatakit.aggregate.client.widgets.SkipMalformedSubmissionsCheckbox;
import org.opendatakit.aggregate.constants.common.HelpSliderConsts;
import org.opendatakit.aggregate.constants.common.PreferencesConsts;
import org.opendatakit.aggregate.constants.common.UIConsts;

public class PreferencesSubTab extends AggregateSubTabBase {

  // Preferences tab
  private static final String INDENTED_STYLE = "indentedRegion";
  private static final String INDENTED_ENTRY_STYLE = "indentedEntryRegion";
  private static final String UNDEFINED_STYLE = "undefinedValue";
  private static final String DEFINED_STYLE = "definedValue";

  private static final String VERSION_LABEL = "<h2>Information sur la version</h2>";
  private static final String VERSION_STRING_STYLE = "app_version_string";

  private static final String GOOGLE_API_CREDENTIALS_LABEL = "<h2>Informations d'identification Google API</h2>";
  private static final String GOOGLE_API_CREDENTIALS_INFO = "<p>Voir <a href=\"http://opendatakit.org/use/aggregate/oauth2-service-account/\" target=\"_blank\">http://opendatakit.org/use/aggregate/oauth2-service-account/</a> pour obtenir des instructions sur l'obtention et la fourniture de ces valeurs.</p>";
  private static final String GOOGLE_API_KEY_LABEL = "<h3>Clé d'accès API simple</h3>";
  private static final String GOOGLE_API_KEY_INFO = "<p>Recommandé pour accéder à Google Maps.</p>";
  private static final String GOOGLE_API_CLIENT_ID_LABEL = "<h3>Informations d'identification Google OAuth2</h3>";
  private static final String GOOGLE_API_CLIENT_ID_INFO = "<p>Nécessaire pour la publication sur Google Spreadsheets</p>";

  private static final String NEW_SERVICE_ACCOUNT_TXT = "Modifier les informations d'identification de l'API Google";
  private static final String NEW_SERVICE_ACCOUNT_TOOLTIP_TXT = "Téléchargez les NOUVELLES informations sur la clé Google Simple API et le compte de service Oauth2.";
  private static final String NEW_SERVICE_ACCOUNT_BALLOON_TXT = "Importer une nouvelle clé Google Simple API et des informations sur le compte de service Oauth2 dans Aggregate.";
  private static final String NEW_SERVICE_ACCOUNT_BUTTON_TEXT = "<img src=\"images/yellow_plus.png\" /> "
      + NEW_SERVICE_ACCOUNT_TXT;

  private static final String ENKETO_API_CREDENTIALS_LABEL = "<h2>Intégration Enketo Webform</h2>";
  private static final String ENKETO_API_CREDENTIALS_INFO = "<p>Voir <a href=\"https://accounts.enketo.org/support/aggregate/\" target=\"_blank\">instructions</a> sur comment faire cela.</p>";
  private static final String ENKETO_API_URL_LABEL = "<h3>Enketo API URL</h3>";
  private static final String ENKETO_API_URL_INFO = "<p>L'URL de l'API de service Enketo</p>";
  private static final String ENKETO_API_TOKEN = "<h3>Token d'API Enketo</h3>";
  private static final String ENKETO_API_TOKEN_INFO = "<p>Nécessaire pour l'authentification avec le service Enketo</p>";

  private static final String NEW_ENKETO_SERVICE_ACCOUNT_TXT = "Changer la configuration de l'API Enketo";
  private static final String NEW_ENKETO_SERVICE_ACCOUNT_TOOLTIP_TXT = "Entrer l'URL du service Enketo et les informations du Token de l'API.";
  private static final String NEW_ENKETO_SERVICE_ACCOUNT_BALLOON_TXT = "Entrez une URL d'API de service Enketo et des informations du Token de l''API pour activer Enketo Webforms..";
  private static final String NEW_ENKETO_SERVICE_ACCOUNT_BUTTON_TEXT = "<img src=\"images/yellow_plus.png\" /> "
      + NEW_ENKETO_SERVICE_ACCOUNT_TXT;

  private static final String FEATURES_LABEL = "<h2>Caractéristiques de Peogo Survey</h2>";

  // external: slower background publishing checkbox

  private Label simpleApiKey;
  private Label googleApiClientId;

  private Label enketoApiUrl;
  private Label enketoApiToken;
  private SkipMalformedSubmissionsCheckbox skipMalformedSubmissions;

  private PreferencesCompletionCallback settingsChange = new PreferencesCompletionCallback() {
    @Override
    public void refreshFromUpdatedPreferences() {
      setCredentialValues();
      skipMalformedSubmissions.updateValue(Preferences.getSkipMalformedSubmissions());
    }

    @Override
    public void failedRefresh() {
      // Error message is displayed. Leave everything as-is.
    }
  };

  public PreferencesSubTab() {
    // vertical
    setStylePrimaryName(UIConsts.VERTICAL_FLOW_PANEL_STYLENAME);

    HTML labelVersion = new HTML(VERSION_LABEL);
    add(labelVersion);
    Label version = new Label();
    version.setStylePrimaryName(VERSION_STRING_STYLE);
    version.setText(BuildConfig.VERSION);
    add(version);

    HTML labelCredentialsSection = new HTML(GOOGLE_API_CREDENTIALS_LABEL);
    add(labelCredentialsSection);

    HTML labelCredentialsInfo = new HTML(GOOGLE_API_CREDENTIALS_INFO);
    labelCredentialsInfo.setStylePrimaryName(INDENTED_STYLE);
    add(labelCredentialsInfo);

    HTML labelApiKeyHeading = new HTML(GOOGLE_API_KEY_LABEL);
    labelApiKeyHeading.setStylePrimaryName(INDENTED_STYLE);
    add(labelApiKeyHeading);

    simpleApiKey = new Label();
    simpleApiKey.setStylePrimaryName(INDENTED_ENTRY_STYLE);
    add(simpleApiKey);

    HTML labelApiKeyInfo = new HTML(GOOGLE_API_KEY_INFO);
    labelApiKeyInfo.setStylePrimaryName(INDENTED_STYLE);
    add(labelApiKeyInfo);

    HTML labelApiClientIdHeading = new HTML(GOOGLE_API_CLIENT_ID_LABEL);
    labelApiClientIdHeading.setStylePrimaryName(INDENTED_STYLE);
    add(labelApiClientIdHeading);

    googleApiClientId = new Label();
    googleApiClientId.setStylePrimaryName(INDENTED_ENTRY_STYLE);
    add(googleApiClientId);

    HTML labelApiClientIdInfo = new HTML(GOOGLE_API_CLIENT_ID_INFO);
    labelApiClientIdInfo.setStylePrimaryName(INDENTED_STYLE);
    add(labelApiClientIdInfo);

    ServletPopupButton newCredential = new ServletPopupButton(NEW_SERVICE_ACCOUNT_BUTTON_TEXT,
        NEW_SERVICE_ACCOUNT_TXT, UIConsts.SERVICE_ACCOUNT_PRIVATE_KEY_UPLOAD_ADDR, this,
        NEW_SERVICE_ACCOUNT_TOOLTIP_TXT, NEW_SERVICE_ACCOUNT_BALLOON_TXT);
    newCredential.setStylePrimaryName(INDENTED_STYLE);
    add(newCredential);

    HTML labelCredentialsSectionEnketo = new HTML(ENKETO_API_CREDENTIALS_LABEL);
    add(labelCredentialsSectionEnketo);

    HTML labelCredentialsInfoEnketo = new HTML(ENKETO_API_CREDENTIALS_INFO);
    labelCredentialsInfoEnketo.setStylePrimaryName(INDENTED_STYLE);
    add(labelCredentialsInfoEnketo);

    HTML labelApiKeyHeadingEnketo = new HTML(ENKETO_API_URL_LABEL);
    labelApiKeyHeadingEnketo.setStylePrimaryName(INDENTED_STYLE);
    add(labelApiKeyHeadingEnketo);

    enketoApiUrl = new Label();
    enketoApiUrl.setStylePrimaryName(INDENTED_ENTRY_STYLE);
    add(enketoApiUrl);

    HTML labelApiKeyInfoEnketo = new HTML(ENKETO_API_URL_INFO);
    labelApiKeyInfoEnketo.setStylePrimaryName(INDENTED_STYLE);
    add(labelApiKeyInfoEnketo);

    HTML labelApiClientIdHeadingEnketo = new HTML(ENKETO_API_TOKEN);
    labelApiClientIdHeadingEnketo.setStylePrimaryName(INDENTED_STYLE);
    add(labelApiClientIdHeadingEnketo);

    enketoApiToken = new Label();
    enketoApiToken.setStylePrimaryName(INDENTED_ENTRY_STYLE);
    add(enketoApiToken);

    HTML labelApiClientIdInfoEnketo = new HTML(ENKETO_API_TOKEN_INFO);
    labelApiClientIdInfoEnketo.setStylePrimaryName(INDENTED_STYLE);
    add(labelApiClientIdInfoEnketo);

    ServletPopupButton newEnketoCredential = new ServletPopupButton(
        NEW_ENKETO_SERVICE_ACCOUNT_BUTTON_TEXT, NEW_ENKETO_SERVICE_ACCOUNT_TXT,
        UIConsts.ENKETO_SERVICE_ACCOUNT_PRIVATE_KEY_UPLOAD_ADDR, this,
        NEW_ENKETO_SERVICE_ACCOUNT_TOOLTIP_TXT, NEW_ENKETO_SERVICE_ACCOUNT_BALLOON_TXT);
    newEnketoCredential.setStylePrimaryName(INDENTED_STYLE);
    add(newEnketoCredential);

    setCredentialValues();
    // add(new UpdateGMapsKeyButton(mapsApiKey));
    // add(new
    // UpdateGoogleClientCredentialsButton(googleApiClientId.getText()));

    HTML features = new HTML(FEATURES_LABEL);
    add(features);

    skipMalformedSubmissions = new SkipMalformedSubmissionsCheckbox(
        Preferences.getSkipMalformedSubmissions(), settingsChange);
    add(skipMalformedSubmissions);

    add(buildVersionNote(this));
  }

  @Override
  public boolean canLeave() {
    return true;
  }

  private void setCredentialValues() {
    String value;
    String enketoValue;

    value = SafeHtmlUtils.fromString(Preferences.getGoogleSimpleApiKey()).asString();
    if (value.length() == 0) {
      value = "undefined";
      simpleApiKey.setStyleName(UNDEFINED_STYLE, true);
      simpleApiKey.setStyleName(DEFINED_STYLE, false);
    } else {
      simpleApiKey.setStyleName(UNDEFINED_STYLE, false);
      simpleApiKey.setStyleName(DEFINED_STYLE, true);
    }
    simpleApiKey.setText(value);

    value = SafeHtmlUtils.fromString(Preferences.getGoogleApiClientId()).asString();
    if (value.length() == 0) {
      value = "undefined";
      googleApiClientId.setStyleName(UNDEFINED_STYLE, true);
      googleApiClientId.setStyleName(DEFINED_STYLE, false);
    } else {
      googleApiClientId.setStyleName(UNDEFINED_STYLE, false);
      googleApiClientId.setStyleName(DEFINED_STYLE, true);
    }
    googleApiClientId.setText(value);

    enketoValue = SafeHtmlUtils.fromString(Preferences.getEnketoApiUrl()).asString();
    if (enketoValue.length() == 0) {
      enketoValue = "undefined";

      enketoApiUrl.setStyleName(UNDEFINED_STYLE, true);
      enketoApiUrl.setStyleName(DEFINED_STYLE, false);
    } else {

      enketoApiUrl.setStyleName(UNDEFINED_STYLE, false);
      enketoApiUrl.setStyleName(DEFINED_STYLE, true);
    }
    enketoApiUrl.setText(enketoValue);

    enketoValue = SafeHtmlUtils.fromString(Preferences.getEnketoApiToken()).asString();
    if (enketoValue.length() == 0) {
      enketoValue = "undefined";

      enketoApiToken.setStyleName(UNDEFINED_STYLE, true);
      enketoApiToken.setStyleName(DEFINED_STYLE, false);
    } else {

      enketoApiToken.setStyleName(UNDEFINED_STYLE, false);
      enketoApiToken.setStyleName(DEFINED_STYLE, true);
    }
    enketoApiToken.setText(enketoValue);
  }

  @Override
  public void update() {
    Preferences.updatePreferences(settingsChange);
  }

  @Override
  public HelpSliderConsts[] getHelpSliderContent() {
    return PreferencesConsts.values();
  }

}
