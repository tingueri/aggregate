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
import org.opendatakit.aggregate.client.form.FormSummary;
import org.opendatakit.aggregate.client.popups.PurgeUpToDatePopup;

public final class PurgeUpToDateButton extends AggregateButton implements ClickHandler {

  private static final String BUTTON_TXT = "Données de soumission de purge";
  private static final String TOOLTIP_TXT = "Supprimer les données jusqu'à une date donnée";
  private static final String HELP_BALLOON_TXT = "Supprimer les données d'un formulaire sélectionné jusqu'à une date donnée.";

  private FormSummary formSummary;

  public PurgeUpToDateButton() {
    super(BUTTON_TXT, TOOLTIP_TXT, HELP_BALLOON_TXT);
  }

  public void setSelectedForm(FormSummary formSummary) {
    if (formSummary == null || formSummary.getId().equals("")) {
      this.formSummary = null;
      setEnabled(false);
    } else {
      this.formSummary = formSummary;
      setEnabled(true);
    }
  }

  @Override
  public void onClick(ClickEvent event) {
    super.onClick(event);

    PurgeUpToDatePopup popup = new PurgeUpToDatePopup(formSummary);
    popup.setPopupPositionAndShow(popup.getPositionCallBack());
  }
}