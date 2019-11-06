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

package org.opendatakit.aggregate.client.popups;

import static org.opendatakit.aggregate.client.security.SecurityUtils.secureRequest;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.TextBox;
import java.util.function.Consumer;
import org.opendatakit.aggregate.client.AggregateUI;
import org.opendatakit.aggregate.client.SecureGWT;
import org.opendatakit.aggregate.client.widgets.AggregateButton;
import org.opendatakit.aggregate.client.widgets.ClosePopupButton;
import org.opendatakit.aggregate.client.widgets.EnumListBox;
import org.opendatakit.aggregate.constants.common.BinaryOption;
import org.opendatakit.aggregate.constants.common.ExternalServicePublicationOption;
import org.opendatakit.aggregate.constants.common.ExternalServiceType;

public final class PublishPopup extends AbstractPopupBase {

  public static final Consumer<String> NO_OP_CONSUMER = (String __) -> {
  };
  private static final String EMPTY_STRING = "";
  private static final String BUTTON_TXT = "<img src=\"images/green_right_arrow.png\" /> Publish";
  private static final String TOOLTIP_TXT = "Publier les données";
  private static final String HELP_BALLOON_TXT = "Les données seront publiées sur Google Spreadsheets ou sur un serveur acceptant le contenu JSON.";
  private static final String ES_SERVICEOPTIONS_TOOLTIP = "Les données de méthode doivent être publiées";
  private static final String ES_SERVICEOPTIONS_BALLOON = "Choisissez si vous souhaitez que seules les anciennes données, que les nouvelles données ou toutes les données soient publiées.";
  private static final String ES_TYPE_TOOLTIP = "Type de connexion de service externe";
  private static final String ES_TYPE_BALLOON = "Sélectionnez l'application où vous souhaitez que vos données soient publiées.";
  private static final String BO_TYPE_TOOLTIP = "Définit comment les données binaires de Media doivent être publiées";
  private static final String BO_TYPE_BALLOON = "Sélectionne comment la date binaire à partir du média doit être publiée. Peogo Survey fournira des liens dans la publication OU intégrera les données dans la publication";
  // this is the main flex table for the popup
  private final FlexTable layout;
  // this is the header
  private final FlexTable topBar;
  // to hold the options
  private final FlexTable optionsBar;

  private final AggregateButton publishButton;

  // to hold the google spreadsheet only options
  private final FlexTable gsBar;
  private final TextBox gsName;
  private final TextBox gsOwnerEmail;

  // to hold the jsonServer only options
  private final FlexTable jsBar;
  private final TextBox jsAuthKey;
  private final TextBox jsUrl;
  private final EnumListBox<BinaryOption> jsBinaryOptions;

  private final String formId;

  private final EnumListBox<ExternalServiceType> serviceType;
  private final EnumListBox<ExternalServicePublicationOption> esOptions;

  public PublishPopup(String formId) {
    super();

    this.formId = formId;
    this.publishButton = new AggregateButton(BUTTON_TXT, TOOLTIP_TXT, HELP_BALLOON_TXT);
    publishButton.addClickHandler(new CreateExernalServiceHandler());

    ExternalServiceType[] valuesToShow = {ExternalServiceType.GOOGLE_SPREADSHEET, ExternalServiceType.JSON_SERVER};
    serviceType = new EnumListBox<>(valuesToShow, ES_TYPE_TOOLTIP,
        ES_TYPE_BALLOON);
    serviceType.addChangeHandler(new ExternalServiceTypeChangeHandler());

    esOptions = new EnumListBox<>(
        ExternalServicePublicationOption.values(), ES_SERVICEOPTIONS_TOOLTIP,
        ES_SERVICEOPTIONS_BALLOON);

    // Set up the tables in the popup
    layout = new FlexTable();

    topBar = new FlexTable();
    topBar.addStyleName("stretch_header");
    topBar.setWidget(0, 0, new HTML("<h2>Form: </h2>"));
    topBar.setWidget(0, 1, new HTML(new SafeHtmlBuilder().appendEscaped(formId).toSafeHtml()));
    topBar.setWidget(0, 2, new HTML("<h2>Publier dans: </h2>"));
    topBar.setWidget(0, 3, serviceType);
    topBar.setWidget(0, 4, publishButton);
    topBar.setWidget(0, 5, new ClosePopupButton(this));

    optionsBar = new FlexTable();
    optionsBar.addStyleName("flexTableBorderTopStretchWidth");
    optionsBar.setWidget(1, 0, new HTML("<h3>Données à publier:</h3>"));
    optionsBar.setWidget(1, 1, esOptions);

    // this is only for google spreadsheets
    gsBar = new FlexTable();
    gsBar.addStyleName("stretch_header");
    gsBar.setWidget(1, 0, new HTML("<h3>Nom du classeur:</h3>"));
    // make the name textbox an appropriate size
    gsName = new TextBox();
    gsName.setText(EMPTY_STRING);
    gsName.setVisibleLength(35);
    gsBar.setWidget(1, 1, gsName);
    gsBar.setWidget(2, 0, new HTML("<h3>Email du propriétaire:</h3>"));
    gsOwnerEmail = new TextBox();
    gsOwnerEmail.setText(EMPTY_STRING);
    gsOwnerEmail.setVisibleLength(35);
    gsOwnerEmail.getElement().setAttribute("type", "email");
    gsBar.setWidget(2, 1, gsOwnerEmail);


    // this is only for simple json server
    jsBar = new FlexTable();
    jsBar.addStyleName("stretch_header");
    // get the URL
    jsBar.setWidget(1, 0, new HTML("<h3>URL à publier sur:</h3>"));
    jsUrl = new TextBox();
    jsUrl.setVisibleLength(60);
    jsBar.setWidget(1, 1, jsUrl);
    // get token
    jsBar.setWidget(2, 0, new HTML("<h3>Jeton d'autorisation:</h3>"));
    jsAuthKey = new TextBox();
    jsAuthKey.setText(EMPTY_STRING);
    jsAuthKey.setVisibleLength(45);
    jsBar.setWidget(2, 1, jsAuthKey);
    // make the options for how to handle the binary
    jsBar.setWidget(3, 0, new HTML("<h3>Inclure les médias en tant que:</h3>"));
    jsBinaryOptions = new EnumListBox<>(
        BinaryOption.values(), BO_TYPE_TOOLTIP,
        BO_TYPE_BALLOON);
    jsBar.setWidget(3, 1, jsBinaryOptions);

    FlowPanel grouping = new FlowPanel();
    grouping.add(gsBar);
    grouping.add(jsBar);
    gsBar.setVisible(false);
    jsBar.setVisible(false);
    optionsBar.setWidget(2, 0, grouping);
    optionsBar.getFlexCellFormatter().setColSpan(2, 0, 2);

    layout.setWidget(0, 0, topBar);
    layout.setWidget(1, 0, optionsBar);
    // set the options to fill the table as well
    layout.getFlexCellFormatter().setColSpan(1, 0, 6);
    setWidget(layout);

    updateUIOptions();

  }

  public void updateUIOptions() {
    System.out.println("MISE À JOUR DES OPTIONS D'UI APPELÉES");
    System.out.println("Type:" + serviceType.getSelectedValue());

    String externalServiceTypeString = serviceType.getSelectedValue();
    ExternalServiceType type = (externalServiceTypeString == null) ? null :
        ExternalServiceType.valueOf(externalServiceTypeString);

    if (type == null) {
      gsBar.setVisible(false);
      jsBar.setVisible(false);
      publishButton.setEnabled(false);
      return;
    }

    publishButton.setEnabled(true);

    switch (type) {
      case GOOGLE_SPREADSHEET:
        gsBar.setVisible(true);
        jsBar.setVisible(false);
        optionsBar.getRowFormatter().setStyleName(2, "enabledTableRow");
        break;
      case JSON_SERVER:
        gsBar.setVisible(false);
        jsBar.setVisible(true);
        optionsBar.getRowFormatter().setStyleName(2, "enabledTableRow");
        break;
      default: // unknown type
        gsBar.setVisible(false);
        jsBar.setVisible(false);
        optionsBar.getRowFormatter().setStyleName(2, "disabledTableRow");
        publishButton.setEnabled(false);
        break;
    }
  }

  private class CreateExernalServiceHandler implements ClickHandler {

    @Override
    public void onClick(ClickEvent event) {
      // Validate common required fields
      if (serviceType.getSelectedValue() == null || serviceType.getSelectedValue().isEmpty()) {
        Window.alert("Vous devez sélectionner un type d'éditeur dans la liste déroulante.");
        return;
      }

      if (esOptions.getSelectedValue() == null || esOptions.getSelectedValue().isEmpty()) {
        Window.alert("Vous devez sélectionner les soumissions à publier.");
        return;
      }

      ExternalServiceType type = ExternalServiceType.valueOf(serviceType.getSelectedValue());
      ExternalServicePublicationOption serviceOp = ExternalServicePublicationOption.valueOf(esOptions.getSelectedValue());

      switch (type) {
        case GOOGLE_SPREADSHEET:
          // Validate the workbook name
          String workbookName = gsName.getText();
          if (workbookName == null || workbookName.isEmpty()) {
            Window.alert("Vous devez fournir un nom de classeur");
            return;
          }

          // Validate the owner's email
          String ownerEmail = gsOwnerEmail.getText();
          if (ownerEmail == null || ownerEmail.isEmpty()) {
            Window.alert("Vous devez fournir l'email du propriétaire");
            return;
          } else if (!validateEmail(ownerEmail)) {
            Window.alert("Email du propriétaire invalide");
            return;
          } else if (!Window.confirm("Veuillez confirmer que l'adresse e-mail du propriétaire que vous avez introduite est correcte: " + ownerEmail)) {
            gsOwnerEmail.setTitle("");
            return;
          }

          secureRequest(
              SecureGWT.getServicesAdminService(),
              (rpc, sc, cb) -> rpc.createGoogleSpreadsheet(formId, workbookName, serviceOp, "mailto:" + ownerEmail, cb),
              NO_OP_CONSUMER,
              this::onFailure
          );
          break;
        case JSON_SERVER: {
          // Validate the URL to publish to
          String url = jsUrl.getText();
          if (url == null || url.isEmpty()) {
            Window.alert("Vous devez fournir une URL pour publier");
            return;
          } else if(!validateURL(url)){
            Window.alert("URL invalide: il faut commencer par http:// or https://");
            return;
          }

          final String jsBinaryOpString = jsBinaryOptions.getSelectedValue();
          final BinaryOption jsBinaryOp = (jsBinaryOpString == null) ? null : BinaryOption.valueOf(jsBinaryOpString);
          secureRequest(
              SecureGWT.getServicesAdminService(),
              (rpc, sc, cb) -> rpc.createSimpleJsonServer(formId, jsAuthKey.getText(), url, serviceOp, "mailto:N/A", jsBinaryOp, cb),
              NO_OP_CONSUMER,
              this::onFailure
          );
        }
        break;
        default: // unknown type
          break;
      }

      hide();
    }

    private void onFailure(Throwable cause) {
      AggregateUI.getUI().reportError(cause);
    }
  }

  private class ExternalServiceTypeChangeHandler implements ChangeHandler {
    @Override
    public void onChange(ChangeEvent event) {
      updateUIOptions();
    }
  }

  public native static boolean validateEmail(String email) /*-{
    var re = /^(([^<>()\[\]\\.,;:\s@"]+(\.[^<>()\[\]\\.,;:\s@"]+)*)|(".+"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/;
    return re.test(String(email).toLowerCase());
  }-*/;

  public boolean validateURL(String url){
    return url.startsWith("http://") || url.startsWith("https://");
  }

}
