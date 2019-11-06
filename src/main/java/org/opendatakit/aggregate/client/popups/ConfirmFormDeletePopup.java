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
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTML;
import org.opendatakit.aggregate.client.AggregateUI;
import org.opendatakit.aggregate.client.SecureGWT;
import org.opendatakit.aggregate.client.widgets.AggregateButton;
import org.opendatakit.aggregate.client.widgets.ClosePopupButton;

public final class ConfirmFormDeletePopup extends AbstractPopupBase {

  private static final String BUTTON_TXT = "<img src=\"images/green_right_arrow.png\" /> Supprimer les données et le formulaire";
  private static final String TOOLTIP_TXT = "Supprimer les données et le formulaire";
  private static final String HELP_BALLOON_TXT = "Ceci supprimera le formulaire et toutes les données contenues. ";

  private final String formId;

  public ConfirmFormDeletePopup(String formId) {
    super();

    this.formId = formId;

    AggregateButton deleteButton = new AggregateButton(BUTTON_TXT, TOOLTIP_TXT, HELP_BALLOON_TXT);
    deleteButton.addClickHandler(new DeleteHandler());

    FlexTable layout = new FlexTable();

    HTML message = new HTML(new SafeHtmlBuilder()
        .appendEscaped("Supprimer toutes les données et la définition de formulaire pour ")
        .appendHtmlConstant("<b>" + formId + "</b><br/>")
        .appendEscaped("Souhaitez-vous supprimer toutes les données téléchargées et la définition de formulaire pour ce formulaire?")
        .toSafeHtml());
    layout.setWidget(0, 0, message);
    layout.setWidget(0, 1, deleteButton);
    layout.setWidget(0, 2, new ClosePopupButton(this));

    setWidget(layout);
  }

  private class DeleteHandler implements ClickHandler {

    @Override
    public void onClick(ClickEvent event) {
      secureRequest(
          SecureGWT.getFormAdminService(),
          (rpc, sessionCookie, cb) -> rpc.deleteForm(formId, cb),
          () -> {
            AggregateUI.getUI().clearError();
            Window.alert("Planifié avec succès la suppression de ce formulaire.\n"
                + "Plusieurs minutes peuvent être nécessaires pour supprimer tous les "
                + "soumissions de données pour ce formulaire - après quoi "
                + "la définition du formulaire elle-même sera supprimée.");
            AggregateUI.getUI().getTimer().refreshNow();
          },
          AggregateUI.getUI()::reportError
      );
      hide();
    }
  }
}