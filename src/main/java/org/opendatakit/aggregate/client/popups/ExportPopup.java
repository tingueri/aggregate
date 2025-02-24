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

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTML;
import org.opendatakit.aggregate.client.AggregateUI;
import org.opendatakit.aggregate.client.SecureGWT;
import org.opendatakit.aggregate.client.filter.FilterGroup;
import org.opendatakit.aggregate.client.filter.FilterSet;
import org.opendatakit.aggregate.client.widgets.AggregateButton;
import org.opendatakit.aggregate.client.widgets.ClosePopupButton;
import org.opendatakit.aggregate.client.widgets.EnumListBox;
import org.opendatakit.aggregate.client.widgets.FilterListBox;
import org.opendatakit.aggregate.constants.common.ExportType;
import org.opendatakit.aggregate.constants.common.SubTabs;

public final class ExportPopup extends AbstractPopupBase {

  private static final String EXPORT_ERROR_MSG = "Problème lors de la création de votre fichier d'exportation";

  private static final String FILE_TYPE_TOOLTIP = "Type de fichier à générer";
  private static final String FILE_TYPE_BALLOON = "Sélectionnez le type de fichier que vous souhaitez créer.";

  private static final String CREATE_BUTTON_TXT = "<img src=\"images/green_right_arrow.png\" /> Exportation";
  private static final String CREATE_BUTTON_TOOLTIP = "Créer un fichier d'exportation";
  private static final String CREATE_BUTTON_HELP_BALLOON = "Cela crée un fichier CSV ou KML de vos données.";

  private static final String PROBLEM_NULL_FILTER_GROUP = "Le groupe de filtrage n'est pas valide";

  // this will be the standard header across the top
  private final FlexTable optionsBar;
  private final EnumListBox<ExportType> fileType;

  private final FilterListBox filtersBox;

  private final AggregateButton exportButton;

  private final String formId;

  public ExportPopup(String formid, FilterGroup selectedFilterGroup) {
    super();
    this.formId = formid;

    // ensure the filter group passed in is for the correct form
    if (selectedFilterGroup != null && formid.equals(selectedFilterGroup.getFormId())) {
      filtersBox = new FilterListBox(selectedFilterGroup);
    } else {
      filtersBox = new FilterListBox();
    }

    SecureGWT.getFilterService().getFilterSet(formId, new FiltersCallback());

    exportButton = new AggregateButton(CREATE_BUTTON_TXT, CREATE_BUTTON_TOOLTIP,
        CREATE_BUTTON_HELP_BALLOON);
    exportButton.addClickHandler(new CreateExportHandler());

    fileType = new EnumListBox<ExportType>(ExportType.values(), FILE_TYPE_TOOLTIP,
        FILE_TYPE_BALLOON);

    // set the standard header widgets
    optionsBar = new FlexTable();
    optionsBar.addStyleName("stretch_header");
    optionsBar.setWidget(0, 0, new HTML("<h2> Form:</h2>"));
    optionsBar.setWidget(0, 1, new HTML(new SafeHtmlBuilder().appendEscaped(formId).toSafeHtml()));
    optionsBar.setWidget(0, 2, new HTML("<h2>Type:</h2>"));
    optionsBar.setWidget(0, 3, fileType);
    optionsBar.setWidget(0, 4, new HTML("<h2>Filtre:</h2>"));
    optionsBar.setWidget(0, 5, filtersBox);
    optionsBar.setWidget(0, 6, exportButton);
    optionsBar.setWidget(0, 7, new ClosePopupButton(this));
    setWidget(optionsBar);
  }

  private static class ErrorDialog extends DialogBox {

    public ErrorDialog() {
      setText("Erreur Type d'exportation inconnu !! S'il vous plaît déclarer le problème sur Peogo Survey!");
      Button ok = new Button("OK");
      ok.addClickHandler(new ClickHandler() {
        public void onClick(ClickEvent event) {
          ErrorDialog.this.hide();
        }
      });
      setWidget(ok);
    }
  }

  ;

  private class FiltersCallback implements AsyncCallback<FilterSet> {

    private static final String PROBLEM_NULL_FILTER_SET = "PROBLÈME: obtenu un NULL pour un ensemble de filtres du serveur";

    @Override
    public void onFailure(Throwable caught) {
      filtersBox.updateFilterDropDown(null);
      AggregateUI.getUI().reportError(caught);
    }

    @Override
    public void onSuccess(FilterSet filterSet) {
      if (filterSet == null) {
        AggregateUI.getUI().reportError(new Throwable(PROBLEM_NULL_FILTER_SET));
      }

      // updates the filter dropdown and sets the class state to the newly
      // created filter list
      filtersBox.updateFilterDropDown(filterSet);
    }
  }

  private class CreateExportHandler implements ClickHandler {
    @Override
    public void onClick(ClickEvent event) {
      ExportType type = ExportType.valueOf(fileType.getValue(fileType.getSelectedIndex()));

      final FilterGroup filterGroup = filtersBox.getSelectedFilter();

      if (filterGroup == null) {
        onFailure(new Throwable(PROBLEM_NULL_FILTER_GROUP));
        return;
      }

      if (type == ExportType.CSV) {
        secureRequest(
            SecureGWT.getFormService(),
            (rpc, sc, cb) -> rpc.createCsvFromFilter(filterGroup, cb),
            this::onSuccess,
            this::onFailure
        );
      } else if (type == ExportType.JSONFILE) {
        secureRequest(
            SecureGWT.getFormService(),
            (rpc, sc, cb) -> rpc.createJsonFileFromFilter(filterGroup, cb),
            this::onSuccess,
            this::onFailure
        );
      } else if (type == ExportType.KML) {
        KmlOptionsPopup popup = new KmlOptionsPopup(formId, filterGroup);
        popup.setPopupPositionAndShow(popup.getPositionCallBack());
        hide();
      } else {
        new ErrorDialog().show();
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
