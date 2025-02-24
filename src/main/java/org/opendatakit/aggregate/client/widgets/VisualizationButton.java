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
import org.opendatakit.aggregate.client.FilterSubTab;
import org.opendatakit.aggregate.client.popups.VisualizationPopup;

public final class VisualizationButton extends AggregateButton implements ClickHandler {

  private static final String BUTTON_TXT = "<img src=\"images/bar_chart.png\" /> Visualiser";
  private static final String TOOLTIP_TXT = "Visualiser les données";
  private static final String HELP_BALLOON_TXT = "Cela ouvrira une nouvelle fenêtre contextuelle qui vous permettra de  "
      + "visualiser les données à l'aide d'un graphique à barres, d'un graphique à secteurs ou de Google Maps.";

  private final FilterSubTab filterSubTab;

  public VisualizationButton(FilterSubTab filterSubTab) {
    super(BUTTON_TXT, TOOLTIP_TXT, HELP_BALLOON_TXT);
    this.filterSubTab = filterSubTab;
  }

  public void onClick(ClickEvent event) {
    super.onClick(event);

    VisualizationPopup vizPopup = new VisualizationPopup(filterSubTab);
    vizPopup.setPopupPositionAndShow(vizPopup.getPositionCallBack());
  }
}
