/*
 * Copyright (C) 2016 University of Washington
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

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTML;
import java.util.ArrayList;
import org.opendatakit.aggregate.client.AggregateUI;
import org.opendatakit.aggregate.client.SecureGWT;
import org.opendatakit.aggregate.client.filter.FilterGroup;
import org.opendatakit.aggregate.client.form.KmlGeoTraceNShapeOption;
import org.opendatakit.aggregate.client.form.KmlGeopointOption;
import org.opendatakit.aggregate.client.form.KmlOptionsSummary;
import org.opendatakit.aggregate.client.form.KmlSelection;
import org.opendatakit.aggregate.client.widgets.AggregateButton;
import org.opendatakit.aggregate.client.widgets.ClosePopupButton;
import org.opendatakit.aggregate.constants.common.SubTabs;

public final class KmlOptionsPopup extends AbstractPopupBase {

  private static final String EXPORT_ERROR_MSG = "Une des options KML n'était pas valide. Par exemple, le champ Geopoint ou votre champ Titre n'étaient pas valides";
  private static final String KML_ELEMENTS_ZERO_ERROR_MSG = "Pour exporter les données au format KML, sélectionnez au moins un élément KML.";

  private static final String CREATE_BUTTON_TXT = "<img src=\"images/green_right_arrow.png\" /> Export";
  private static final String CREATE_BUTTON_TOOLTIP = "Créer un fichier KML";
  private static final String CREATE_BUTTON_HELP_BALLOON = "Cela exporte vos données dans un fichier KML avec les options suivantes.";

  // this will be the standard header across the top
  private final FlexTable topBar;

  private final AggregateButton exportButton;

  private final String formId;
  private final FilterGroup selectedfilterGroup;

  private ArrayList<KmlSelectionGeneration> rows;

  public KmlOptionsPopup(String formid, FilterGroup selectedFilterGroup) {
    super();
    this.formId = formid;
    this.selectedfilterGroup = selectedFilterGroup;
    this.rows = new ArrayList<>();

    SecureGWT.getFormService().getPossibleKmlSettings(formId, new KmlSettingsCallback());

    exportButton = new AggregateButton(CREATE_BUTTON_TXT, CREATE_BUTTON_TOOLTIP,
        CREATE_BUTTON_HELP_BALLOON);
    exportButton.addClickHandler(new CreateExportHandler());

    // disable export button until data received from server
    exportButton.setEnabled(false);

    // set the standard header widgets
    topBar = new FlexTable();
    topBar.addStyleName("stretch_header");
    topBar.setWidget(0, 0, new HTML("<h2> Form:</h2>"));
    topBar.setWidget(0, 1, new HTML(new SafeHtmlBuilder().appendEscaped(formId).toSafeHtml()));
    topBar.setWidget(0, 2, new HTML("<h2>Filtre:</h2>"));
    topBar.setWidget(0, 3, new HTML(new SafeHtmlBuilder().appendEscaped(selectedFilterGroup.getName()).toSafeHtml()));
    topBar.setWidget(0, 6, exportButton);
    topBar.setWidget(0, 7, new ClosePopupButton(this));


    FlexTable initlayout = new FlexTable();
    initlayout.setWidget(0, 0, topBar);
    setWidget(initlayout);
  }

  private class KmlSettingsCallback implements AsyncCallback<KmlOptionsSummary> {
    @Override
    public void onFailure(Throwable caught) {
      AggregateUI.getUI().reportError(caught);
    }

    @Override
    public void onSuccess(KmlOptionsSummary result) {
      // eliminate kml options and refresh table
      rows = new ArrayList<>();

      FlexTable layout = new FlexTable();
      int tableRow = 0;
      layout.setWidget(tableRow++, 0, topBar);
      layout.setWidget(tableRow++, 0, new HTML("<h2>Options Export KML:</h2>"));

      for (KmlGeopointOption gpNode : result.getGeopointOptions()) {
        KmlGeoPointSettingsSelectionRow row = new KmlGeoPointSettingsSelectionRow(formId, gpNode);
        rows.add(row);
        layout.setWidget(tableRow++, 0, row);
      }

      for (KmlGeoTraceNShapeOption gtsNode : result.getGeoTraceNShapeOptions()) {
        KmlGeoTraceNShapeSelectionRow row = new KmlGeoTraceNShapeSelectionRow(formId, gtsNode);
        rows.add(row);
        layout.setWidget(tableRow++, 0, row);
      }

      setWidget(layout);

      // enable export button now that data has been received from server
      exportButton.setEnabled(true);
      center();
    }
  }

  private class CreateExportHandler implements ClickHandler {
    @Override
    public void onClick(ClickEvent event) {

      boolean atLeastOneKmlElementCreated = false;
      ArrayList<KmlSelection> kmlElementsToInclude = new ArrayList<>();
      for (KmlSelectionGeneration row : rows) {
        KmlSelection kmlElement = row.generateKmlSelection();
        if (kmlElement != null) {
          atLeastOneKmlElementCreated = true;
          kmlElementsToInclude.add(kmlElement);
        }
      }

      if (atLeastOneKmlElementCreated) {
        secureRequest(
            SecureGWT.getFormService(),
            (rpc, sc, cb) -> rpc.createKmlFromFilter(selectedfilterGroup, kmlElementsToInclude, cb),
            this::onSuccess,
            this::onFailure
        );
      } else {
        Window.alert(KML_ELEMENTS_ZERO_ERROR_MSG);
      }
    }

    private void onFailure(Throwable cause) {
      AggregateUI.getUI().reportError(cause);
    }

    private void onSuccess(Boolean result) {
      if (result) {
        AggregateUI.getUI().redirectToSubTab(SubTabs.EXPORT);
      } else {
        Window.alert(EXPORT_ERROR_MSG);
      }

      hide();
    }
  }
}