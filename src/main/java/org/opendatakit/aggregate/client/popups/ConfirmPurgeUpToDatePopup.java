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
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTML;
import java.util.Date;
import org.opendatakit.aggregate.client.AggregateUI;
import org.opendatakit.aggregate.client.SecureGWT;
import org.opendatakit.aggregate.client.form.FormSummary;
import org.opendatakit.aggregate.client.widgets.AggregateButton;
import org.opendatakit.aggregate.client.widgets.ClosePopupButton;
import org.opendatakit.common.utils.GwtShims;

public class ConfirmPurgeUpToDatePopup extends AbstractPopupBase {

  private static final String BUTTON_TXT = "<img src=\"images/green_right_arrow.png\" /> Purge Data";
  private static final String TOOLTIP_TXT = "Supprimer les données des soumissions jusqu'à la date donnée";
  private static final String HELP_BALLOON_TXT = "Cela confirme que vous souhaitez supprimer les données de soumission.";

  private FormSummary summary;
  private Date earliest;

  @SuppressWarnings("deprecation")
  public ConfirmPurgeUpToDatePopup(FormSummary summary, Date earliest) {
    super();

    this.summary = summary;
    this.earliest = earliest;

    AggregateButton confirm = new AggregateButton(BUTTON_TXT, TOOLTIP_TXT, HELP_BALLOON_TXT);
    confirm.addClickHandler(new PurgeHandler());

    FlexTable layout = new FlexTable();
    SafeHtml content = new SafeHtmlBuilder()
        .appendEscaped("Supprimer les données de soumission de ")
        .appendHtmlConstant("<b>" + summary.getTitle() + " [" + summary.getId() + "]</b>")
        .appendEscaped(" à travers ")
        .appendEscaped(GwtShims.gwtFormatDateHuman(earliest))
        .appendEscaped(". Les soumissions incomplètes ne seront pas supprimées.")
        .toSafeHtml();
    layout.setWidget(0, 0, new HTML(content));
    layout.setWidget(0, 1, confirm);
    layout.setWidget(0, 2, new ClosePopupButton(this));
    setWidget(layout);
  }

  private class PurgeHandler implements ClickHandler {
    @Override
    public void onClick(ClickEvent event) {
      secureRequest(
          SecureGWT.getFormAdminService(),
          (rpc, sessionCookie, cb) -> rpc.purgeSubmissionsData(summary.getId(), earliest, cb),
          (Date result) -> {
            Window.alert("" +
                "Début réussi de la purge de:\n" +
                summary.getTitle() + " [" + summary.getId() + "].\n" +
                "Supprimer toutes les données de soumission via\n  " +
                GwtShims.gwtFormatDateHuman(result) + "\n" +
                "Les soumissions incomplètes ne seront pas supprimées.");
          },
          cause -> AggregateUI.getUI().reportError("Échec de la purge des données de soumission: ", cause)
      );
      hide();
    }
  }
}
