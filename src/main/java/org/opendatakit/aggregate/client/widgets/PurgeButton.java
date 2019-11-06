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

package org.opendatakit.aggregate.client.widgets;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.Window;
import java.util.Date;
import org.opendatakit.aggregate.client.externalserv.ExternServSummary;
import org.opendatakit.aggregate.client.popups.ConfirmPurgePopup;
import org.opendatakit.aggregate.constants.common.ExternalServicePublicationOption;
import org.opendatakit.common.utils.GwtShims;

public final class PurgeButton extends AggregateButton implements ClickHandler {

  private static final String BUTTON_TXT = "Purger les données publiées";
  private static final String TOOLTIP_TXT = "Effacer les données publiées";
  private static final String HELP_BALLOON_TXT = "Cela supprimera les données publiées.";

  private final String formId;
  private final ExternServSummary externServ;

  public PurgeButton(String formId, ExternServSummary externalService) {
    super(BUTTON_TXT, TOOLTIP_TXT, HELP_BALLOON_TXT);
    this.formId = formId;
    this.externServ = externalService;
  }

  @Override
  public void onClick(ClickEvent event) {
    super.onClick(event);

    Date earliest = null;
    switch (externServ.getPublicationOption()) {
      case UPLOAD_ONLY:
        if (externServ.getUploadCompleted()) {
          earliest = externServ.getTimeEstablished();
        } else {
          earliest = externServ.getTimeLastUploadCursor();
        }
        break;
      case UPLOAD_N_STREAM:
        if (externServ.getUploadCompleted()) {
          earliest = externServ.getTimeLastStreamingCursor();
          if (earliest == null) {
            earliest = externServ.getTimeEstablished();
          }
        } else {
          earliest = externServ.getTimeLastUploadCursor();
        }
        break;
      case STREAM_ONLY:
        earliest = externServ.getTimeLastStreamingCursor();
        if (earliest == null) {
          earliest = externServ.getTimeEstablished();
        }
        break;
    }

    SafeHtmlBuilder b = new SafeHtmlBuilder();
    if (earliest == null) {
      Window.alert("Les données n'ont pas encore été publiées - aucune donnée ne sera purgée");
    } else {
      if (externServ.getPublicationOption() != ExternalServicePublicationOption.UPLOAD_ONLY) {
        b.appendHtmlConstant("<p>")
            .appendHtmlConstant("<b>Note:</b>")
            .appendEscaped("Même si l’action de publication choisie implique un flux continu ")
            .appendEscaped("des données au service externe, cette action de purge est un événement unique et ")
            .appendEscaped("n'est pas automatiquement en cours. Vous devrez répéter périodiquement ce processus.")
            .appendHtmlConstant("</p>");
      }
      b.appendEscaped("Cliquez pour confirmer la purge de ")
          .appendHtmlConstant("<b>").appendEscaped(formId).appendHtmlConstant("</b>")
          .appendEscaped(" soumissions plus anciennes que " + GwtShims.gwtFormatDateTimeHuman(earliest));

      // TODO: display pop-up with text from b...
      final ConfirmPurgePopup popup = new ConfirmPurgePopup(externServ, earliest, b.toSafeHtml());
      popup.setPopupPositionAndShow(popup.getPositionCallBack());
    }
  }

}
